## ---------------------------------------------------
## FINAL and COMPLETE main.py FOR PATIENT BACKEND
## ---------------------------------------------------

from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorGridFSBucket
from bson import ObjectId
from passlib.context import CryptContext
from copy import deepcopy
from datetime import datetime, timedelta, time
from typing import Optional, Any, Dict, List
import os
import asyncio
import firebase_admin
from firebase_admin import credentials, messaging
import httpx
import re
from zoneinfo import ZoneInfo
from dotenv import load_dotenv
# Add these to your existing imports
from agora_token_builder import RtcTokenBuilder
import time as time_module
import random
# Agora credentials (prefer env vars, fallback to placeholders)
AGORA_APP_ID = os.environ.get("AGORA_APP_ID") or "18201a6aca7044c6982cdd8fa6e38993"  # <--- PASTE YOUR ID HERE
AGORA_APP_CERTIFICATE = os.environ.get("AGORA_APP_CERTIFICATE") or "589ed7705d32450baff17ed100208833" # <--- PASTE CERT HERE

# Load environment variables from .env file18201a6aca7044c6982cdd8fa6e38993
load_dotenv()

# IST timezone
IST = ZoneInfo("Asia/Kolkata")

app = FastAPI(title="Patient App API")

# Allow frontend to connect
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Add exception handler for all requests
@app.middleware("http")
async def log_all_requests(request, call_next):
    """Log all incoming requests for debugging"""
    print(f"[REQUEST] {request.method} {request.url.path}")
    try:
        response = await call_next(request)
        print(f"[RESPONSE] {request.method} {request.url.path} -> {response.status_code}")
        return response
    except Exception as e:
        print(f"[REQUEST_ERROR] {request.method} {request.url.path} -> {e}")
        raise

# Using async Motor client
mongo_url = os.environ.get("MONGO_URL") or os.getenv("MONGO_URL") or "mongodb://localhost:27017/"
client = AsyncIOMotorClient(mongo_url)
db = client["chakra_hospital"]
patients_collection = db["patients"]
submissions_collection = db["patient_submissions"]
messages_collection = db["messages"]
gridfs_bucket = AsyncIOMotorGridFSBucket(db)

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt", "pbkdf2_sha256"], deprecated="auto")

print("[STARTUP] FastAPI app initialized - main.py loaded")
print("[STARTUP] App will start listening on startup...")

# initialize firebase admin if credential path provided in env
FIREBASE_CRED_PATH = r"firebase_admin.json"
try:
    if FIREBASE_CRED_PATH and not firebase_admin._apps:
        cred = credentials.Certificate(FIREBASE_CRED_PATH.strip('"'))
        firebase_admin.initialize_app(cred)
        print("Firebase admin initialized using:", FIREBASE_CRED_PATH)
        FIREBASE_INITIALIZED = True
        LAST_FIREBASE_ERROR = None
except Exception as e:
    # initialization failing should not crash the whole app; log
    print("Firebase initialization failed:", e)
    FIREBASE_INITIALIZED = False
    LAST_FIREBASE_ERROR = str(e)

# ensure flags exist even if path missing
try:
    FIREBASE_INITIALIZED
except NameError:
    FIREBASE_INITIALIZED = False
    LAST_FIREBASE_ERROR = None

# Optional: doctor backend URL to which notification delivery should be delegated.
# If set, the patient backend will POST notification docs to the doctor backend
# instead of invoking firebase_admin directly. Example: "https://doctor.example.com"
DOCTOR_BACKEND_URL = os.environ.get("DOCTOR_BACKEND_URL") or os.getenv("DOCTOR_BACKEND_URL")

# new collections
fcm_tokens_collection = db["fcm_tokens"]
notifications_collection = db["notifications"]
adherence_collection = db["adherence"]
visiontests_collection = db["visiontests"]
scheduled_meds_collection = db["scheduled_medicines"]
medicine_doses_collection = db["medicine_doses"]
WEEKDAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

# ---------------- Models ----------------
class LoginRequest(BaseModel):
    email: str
    password: str


class PatientSubmission(BaseModel):
    id: Optional[str] = Field(default=None, alias="_id")
    patient_id: str
    patient_name: str
    doctor_id: str
    image_file_id: str
    pain_scale: int
    vision_blur: Optional[int] = 0  # NEW
    swelling: Optional[int] = 0
    redness: Optional[int] = 0
    watering: Optional[int] = 0
    itching: Optional[int] = 0      # NEW
    discharge: Optional[int] = 0    # NEW
    comments: Optional[str] = None
    is_viewed: bool = False
    timestamp: datetime = Field(default_factory=datetime.utcnow)

# --------------- Agora Call Models ---------------
class CallInitiateRequest(BaseModel):
    doctor_id: str
    patient_id: str
    channel_name: str


# ---------------- Utils ----------------
def serialize_doc(obj: Any) -> Any:
    """Recursively convert MongoDB types to JSON-serializable types."""
    if isinstance(obj, ObjectId):
        return str(obj)
    if isinstance(obj, datetime):
        return obj.isoformat()
    if isinstance(obj, dict):
        return {k: serialize_doc(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [serialize_doc(v) for v in obj]
    return obj


def _extract_email_contact(patient: Dict[str, Any]) -> Optional[str]:
    ci = patient.get("contactInfo")
    if isinstance(ci, dict):
        return ci.get("email") or ci.get("phone")
    return patient.get("email")


def _expose_compat_fields(patient: Dict[str, Any]) -> Dict[str, Any]:
    """
    Add convenience / legacy-compatible keys so frontend can read consistent fields:
     - email, phone (top-level)
     - prescription (from doctor.prescription or drugHistory.currentMeds)
     - medicines (alias to prescription for older UI)
     - history_summary (small summary)
     - recent_encounter (computed)
     - normalize _id and created_at
    Supports BOTH old schema (top-level fields) and NEW schema (patientDetails, medicalHistory at root).
    """
    p = deepcopy(patient)

    # Normalize _id which may come as {'$oid': '...'} or ObjectId or plain str
    try:
        raw_id = p.get("_id")
        if isinstance(raw_id, dict) and raw_id.get("$oid"):
            p["_id"] = raw_id.get("$oid")
        else:
            p["_id"] = str(raw_id) if raw_id is not None else None
    except Exception:
        p["_id"] = p.get("_id")

    # NEW SCHEMA: Extract from patientDetails if present (new schema structure)
    pat_details = p.get("patientDetails") or {}
    if isinstance(pat_details, dict):
        # Map patientDetails top-level for backward compatibility
        p.setdefault("name", pat_details.get("name", p.get("name")))
        p.setdefault("email", pat_details.get("email", p.get("email")))
        p.setdefault("phone", pat_details.get("phone", p.get("phone")))
        p.setdefault("age", pat_details.get("age", p.get("age")))
        p.setdefault("sex", pat_details.get("sex", p.get("sex")))
        p.setdefault("bloodType", pat_details.get("bloodType", p.get("bloodType")))
        p.setdefault("address", pat_details.get("address", p.get("address")))
        p.setdefault("registrationId", pat_details.get("registrationId", p.get("registrationId")))

    # Normalize created_at (ensure ISO string)
    if p.get("created_at") is not None:
        try:
            # if datetime object, convert to iso
            if isinstance(p["created_at"], datetime):
                p["created_at"] = p["created_at"].isoformat()
            # if dict with $date
            elif isinstance(p["created_at"], dict) and ("$date" in p["created_at"]):
                p["created_at"] = p["created_at"]["$date"]
        except Exception:
            pass

    # top-level email/phone/address from contactInfo
    ci = p.get("contactInfo") or {}
    if isinstance(ci, dict):
        p["email"] = ci.get("email", p.get("email"))
        p["phone"] = ci.get("phone", p.get("phone"))
        p["address"] = ci.get("address", p.get("address"))

    # demographics exposures (age, sex, bloodType)
    demo = p.get("demographics") or {}
    if isinstance(demo, dict):
        p["age"] = demo.get("age", p.get("age"))
        p["sex"] = demo.get("sex", p.get("sex"))
        p["bloodType"] = demo.get("bloodType", p.get("bloodType"))

    # emergency contact exposure
    ec = p.get("emergencyContact") or {}
    if isinstance(ec, dict):
        p["emergencyContactName"] = ec.get("name")
        p["emergencyContactPhone"] = ec.get("phone")

    # prescription alias: prefer doctor.prescription, fallback to drugHistory.currentMeds, then top-level
    try:
        doc_section = p.get("doctor") or {}
        presc_candidate = None
        if isinstance(doc_section, dict):
            presc_candidate = doc_section.get("prescription")
        if not presc_candidate:
            # fallback to drugHistory.currentMeds
            dh = p.get("drugHistory") or {}
            if isinstance(dh, dict):
                presc_candidate = dh.get("currentMeds") or dh.get("currentMeds") or dh.get("previousMeds")
        if not presc_candidate:
            presc_candidate = p.get("prescription")
        # set both keys so older clients keep working
        p["prescription"] = presc_candidate or {}
        p["medicines"] = p["prescription"]
    except Exception:
        p["prescription"] = p.get("prescription", {})
        p["medicines"] = p["prescription"]

    # history summary: small convenient object
    try:
        hist = p.get("history") or {}
        if isinstance(hist, dict):
            p["history_summary"] = {
                "severity": hist.get("severity"),
                "onset": hist.get("onset"),
                "familyHistory": hist.get("family"),
                "medical_count": len(hist.get("medical") or []),
                "surgical_count": len(hist.get("surgical") or []),
            }
    except Exception:
        p["history_summary"] = {}

    # NEW SCHEMA: medicalHistory and drugHistory at root level
    if not p.get("history_summary") or not p["history_summary"].get("familyHistory"):
        try:
            med_hist = p.get("medicalHistory") or {}
            if isinstance(med_hist, dict):
                p["history_summary"] = {
                    "severity": "",
                    "onset": "",
                    "familyHistory": med_hist.get("familyHistory", ""),
                    "medical_count": len(med_hist.get("medical") or []),
                    "surgical_count": len(med_hist.get("surgical") or []),
                }
        except Exception:
            pass

    # compute a small recent_encounter summary (if encounters present)
    try:
        encs = p.get("encounters")
        if isinstance(encs, list) and encs:
            best = None
            best_dt = None
            for e in encs:
                if not isinstance(e, dict):
                    continue
                dt = e.get("date")
                if isinstance(dt, dict):
                    dt = dt.get("$date") or dt.get("date")
                parsed = None
                if isinstance(dt, str):
                    try:
                        parsed = datetime.fromisoformat(dt.replace("Z", "+00:00"))
                    except Exception:
                        parsed = None
                if best_dt is None or (parsed and parsed > best_dt):
                    best_dt = parsed
                    best = e
            p["recent_encounter"] = best or encs[-1]
        else:
            p["recent_encounter"] = None
    except Exception:
        p["recent_encounter"] = None

    # NEW SCHEMA: extract recent_encounter from visits if present (and encounters not found)
    try:
        if not p.get("recent_encounter"):
            visits = p.get("visits")
            if isinstance(visits, list) and visits:
                # Use the most recent visit
                p["recent_encounter"] = visits[-1] if visits else None
    except Exception:
        pass

    return p


def _email_exists_in_document(doc: Dict[str, Any], email: str) -> bool:
    """
    Recursively search entire document for email (case-insensitive).
    This is a fallback for when the email is not found in known locations.
    """
    email_lower = email.lower()
    
    def search_recursive(obj):
        """Recursively search through nested structures"""
        if isinstance(obj, dict):
            for key, value in obj.items():
                # Check if this field's value matches the email
                if isinstance(value, str) and value.lower() == email_lower:
                    return True
                # Recurse into nested structures
                if search_recursive(value):
                    return True
        elif isinstance(obj, list):
            for item in obj:
                if search_recursive(item):
                    return True
        return False
    
    return search_recursive(doc)


def _extract_password_from_user(user: Dict[str, Any]) -> Optional[str]:
    """
    Two-tier password extraction strategy:
    
    TIER 1: Fast - Search known/common password field locations
    TIER 2: Fallback - Search nested arrays (encounters, visits) for password
    
    Key: Skip EMPTY STRINGS explicitly (they are falsy but block OR chain logic)
    This ensures we find passwords in nested locations even if root level has empty strings.
    
    Returns password string if found, None if not found anywhere.
    Robust to future schema changes - will find password in any location.
    """
    if not isinstance(user, dict):
        return None
    
    # Helper: Check if value is a non-empty password
    def is_valid_password(val):
        return isinstance(val, str) and len(val.strip()) > 0
    
    # ========== TIER 1: Fast Path (Known Field Locations) ==========
    print(f"[PASSWORD_EXTRACT] TIER 1: Searching known password field locations")
    
    # Root-level fields
    candidates_tier1 = [
        ("password", user.get("password")),
        ("hashed_password", user.get("hashed_password")),
    ]
    
    # PatientDetails fields (new schema)
    pat_details = user.get("patientDetails") or {}
    if isinstance(pat_details, dict):
        candidates_tier1.append(("patientDetails.password", pat_details.get("password")))
    
    # LastReception fields (nested)
    last_reception = user.get("lastReception") or {}
    if isinstance(last_reception, dict):
        last_rec_pat = last_reception.get("patientDetails") or {}
        if isinstance(last_rec_pat, dict):
            candidates_tier1.append(("lastReception.patientDetails.password", last_rec_pat.get("password")))
    
    # Check TIER 1 candidates (skip empty strings)
    for field_name, pwd_value in candidates_tier1:
        if is_valid_password(pwd_value):
            print(f"[PASSWORD_EXTRACT] ✓ TIER 1 SUCCESS - Found password in: {field_name}")
            return pwd_value
    
    print(f"[PASSWORD_EXTRACT] ✗ TIER 1 FAILED - No valid password in known locations, trying TIER 2...")
    
    # ========== TIER 2: Fallback Path (Nested Arrays) ==========
    print(f"[PASSWORD_EXTRACT] TIER 2: Scanning nested encounters/visits arrays")
    
    # Try encounters array (most recent first)
    encounters = user.get("encounters") or []
    if isinstance(encounters, list) and len(encounters) > 0:
        for idx, encounter in enumerate(encounters):
            if not isinstance(encounter, dict):
                continue
            
            # Check: encounters[idx].password
            if is_valid_password(encounter.get("password")):
                print(f"[PASSWORD_EXTRACT] ✓ TIER 2 SUCCESS - Found password in encounters[{idx}].password")
                return encounter.get("password")
            
            # Check: encounters[idx].details.patientDetails.password
            details = encounter.get("details") or {}
            if isinstance(details, dict):
                pat_det = details.get("patientDetails") or {}
                if isinstance(pat_det, dict) and is_valid_password(pat_det.get("password")):
                    print(f"[PASSWORD_EXTRACT] ✓ TIER 2 SUCCESS - Found password in encounters[{idx}].details.patientDetails.password")
                    return pat_det.get("password")
    
    # Try visits array (most recent first)
    visits = user.get("visits") or []
    if isinstance(visits, list) and len(visits) > 0:
        for idx, visit in enumerate(visits):
            if not isinstance(visit, dict):
                continue
            
            # Check: visits[idx].password
            if is_valid_password(visit.get("password")):
                print(f"[PASSWORD_EXTRACT] ✓ TIER 2 SUCCESS - Found password in visits[{idx}].password")
                return visit.get("password")
            
            # Check: visits[idx].stages.reception.data.patientDetails.password
            stages = visit.get("stages") or {}
            if isinstance(stages, dict):
                reception = stages.get("reception") or {}
                if isinstance(reception, dict):
                    data = reception.get("data") or {}
                    if isinstance(data, dict):
                        pat_det = data.get("patientDetails") or {}
                        if isinstance(pat_det, dict) and is_valid_password(pat_det.get("password")):
                            print(f"[PASSWORD_EXTRACT] ✓ TIER 2 SUCCESS - Found password in visits[{idx}].stages.reception.data.patientDetails.password")
                            return pat_det.get("password")
    
    # Ultra-robust recursive search for any password field (handles unknown future formats)
    print(f"[PASSWORD_EXTRACT] TIER 2B: Recursive search for password in any nested location...")
    def find_password_recursive(obj, depth=0, path=""):
        """Recursively search for any non-empty 'password' field"""
        if depth > 15:  # Prevent infinite recursion
            return None
        
        if isinstance(obj, dict):
            # Found a password field?
            if "password" in obj and is_valid_password(obj["password"]):
                print(f"[PASSWORD_EXTRACT] ✓ TIER 2B SUCCESS - Found password via recursive search at: {path}.password")
                return obj["password"]
            
            # Search nested dicts
            for key, value in obj.items():
                result = find_password_recursive(value, depth + 1, f"{path}.{key}" if path else key)
                if result:
                    return result
        
        elif isinstance(obj, list):
            # Search list items
            for idx, item in enumerate(obj):
                result = find_password_recursive(item, depth + 1, f"{path}[{idx}]")
                if result:
                    return result
        
        return None
    
    pwd = find_password_recursive(user)
    if pwd:
        return pwd
    
    print(f"[PASSWORD_EXTRACT] ✗ ALL TIERS FAILED - Password not found anywhere in document")
    return None


async def _find_user_by_email_or_contact(email: str) -> Optional[Dict[str, Any]]:
    """
    Two-tier email lookup strategy:
    TIER 1: Fast - Search known/common email field locations using MongoDB query
    TIER 2: Fallback - Recursively scan entire document (if Tier 1 fails)
    
    This ensures robustness even if email is stored in unexpected nested locations.
    """
    email_lower = email.lower()
    
    # ========== TIER 1: Fast Path (Known Field Locations) ==========
    print(f"[FIND_USER] TIER 1: Searching known email field locations for: {email}")
    
    q1 = {"$or": [
        # Root-level fields
        {"email": {"$regex": f"^{email}$", "$options": "i"}}, 
        {"contactInfo.email": {"$regex": f"^{email}$", "$options": "i"}}, 
        {"contactInfo.phone": email},
        # New schema root-level fields
        {"patientDetails.email": {"$regex": f"^{email}$", "$options": "i"}},
        {"patientDetails.phone": email},
        # Nested in encounters array
        {"encounters.details.patientDetails.email": {"$regex": f"^{email}$", "$options": "i"}},
        # Nested in lastReception
        {"lastReception.patientDetails.email": {"$regex": f"^{email}$", "$options": "i"}},
        # Nested in visits array
        {"visits.stages.reception.data.patientDetails.email": {"$regex": f"^{email}$", "$options": "i"}}
    ]}
    
    user = await patients_collection.find_one(q1)
    
    if user:
        print(f"[FIND_USER] ✓ TIER 1 SUCCESS - Found user via known field locations")
        return user
    
    print(f"[FIND_USER] ✗ TIER 1 FAILED - Email not in known locations, trying TIER 2 fallback...")
    
    # ========== TIER 2: Fallback Path (Full Document Scan) ==========
    print(f"[FIND_USER] TIER 2: Scanning all patients for email anywhere in document...")
    
    try:
        cursor = patients_collection.find()
        async for patient in cursor:
            if _email_exists_in_document(patient, email):
                print(f"[FIND_USER] ✓ TIER 2 SUCCESS - Found user via recursive document scan")
                return patient
    except Exception as e:
        print(f"[FIND_USER] TIER 2 ERROR: {e}")
    
    print(f"[FIND_USER] ✗ TIER 2 FAILED - Email not found anywhere in database")
    return None


# ============ MEDICINE SCHEDULING HELPERS ============

def _parse_duration_days(duration_str: str, default_days: int = 7) -> int:
    """
    Parse duration strings like:
      "1 week", "2 weeks", "10 days", "1 month", "30 days"
    Returns number of days (int). Falls back to default_days on failure.
    """
    if not duration_str:
        return default_days
    s = str(duration_str).strip().lower()
    
    # months -> 30 days approximate
    m = re.search(r'(\d+)\s*(month|months|mo\b)', s)
    if m:
        return int(m.group(1)) * 30
    
    # weeks -> 7 days
    m = re.search(r'(\d+)\s*(week|weeks|wk|w\b)', s)
    if m:
        return int(m.group(1)) * 7
    
    # days
    m = re.search(r'(\d+)\s*(day|days|d\b)', s)
    if m:
        return int(m.group(1))
    
    # phrases: 'one week', 'two weeks' etc.
    word_map = {
        "one": 1, "two": 2, "three": 3, "four": 4, "five": 5,
        "six": 6, "seven": 7, "eight": 8, "nine": 9, "ten": 10
    }
    for w, n in word_map.items():
        if f"{w} week" in s or f"{w} weeks" in s:
            return n * 7
        if f"{w} day" in s or f"{w} days" in s:
            return n
        if f"{w} month" in s or f"{w} months" in s:
            return n * 30
    
    # fallback: return default
    return default_days


def _parse_start_date_from_prescription(prescription: dict, patient_doc: dict):
    """
    Choose start date for scheduling:
     - Prefer prescription.summary.datePrescribed (try ISO and common '1 dec 2025' style)
     - Else patient's lastVisit (if present)
     - Else now (IST)
    Returns aware datetime (IST).
    """
    def try_parse_date(s):
        if not s:
            return None
        if isinstance(s, datetime):
            if s.tzinfo is None:
                return s.replace(tzinfo=IST)
            return s.astimezone(IST)
        
        ss = str(s).strip()
        
        # try ISO first
        try:
            dt = datetime.fromisoformat(ss)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=IST)
            else:
                dt = dt.astimezone(IST)
            return dt
        except Exception:
            pass
        
        # try common formats: "1 dec 2025", "30 Jun 2025", "01-12-2025"
        fmts = ["%d %b %Y", "%d %B %Y", "%d-%m-%Y", "%d/%m/%Y", "%d %b %Y %H:%M", "%Y-%m-%d"]
        for f in fmts:
            try:
                dt = datetime.strptime(ss, f)
                dt = dt.replace(tzinfo=IST)
                return dt
            except Exception:
                continue
        
        return None

    # 1) prescription.summary.datePrescribed
    if isinstance(prescription, dict):
        summary = prescription.get("summary") or {}
        dp = summary.get("datePrescribed")
        dt = try_parse_date(dp)
        if dt:
            return dt

    # 2) patient's lastVisit (often ISO string)
    lv = patient_doc.get("lastVisit") or patient_doc.get("last_visit") or patient_doc.get("last_visit_date")
    dt = try_parse_date(lv)
    if dt:
        return dt

    # 3) now IST
    return datetime.now(IST)


def _make_send_datetimes_for_duration(times_list: List[time], start_dt_ist: datetime, duration_days: int) -> List[str]:
    """
    Given list of time objects (IST), start datetime (IST), and duration_days,
    return list of IST-aware ISO datetimes for scheduled reminders.

    Start from start_dt_ist.date() (if the start day's time has already passed,
    scheduling for that time moves to next day).
    """
    results = []
    start_date = start_dt_ist.date()
    end_date = start_date + timedelta(days=duration_days - 1)
    now_ist = datetime.now(IST)

    # iterate days from start_date to end_date inclusive
    day = start_date
    while day <= end_date:
        for t in times_list:
            dt_ist = datetime.combine(day, t, tzinfo=IST)
            # skip times already passed relative to now
            if dt_ist <= now_ist:
                continue
            results.append(dt_ist.isoformat())
        day = day + timedelta(days=1)
    
    return results


# Schedule common medicine times (IST)
_DAYSCHEDULES = {
    1: [time(8, 0, tzinfo=IST)],  # 8 AM
    2: [time(8, 0, tzinfo=IST), time(20, 0, tzinfo=IST)],  # 8 AM, 8 PM
    3: [time(8, 0, tzinfo=IST), time(14, 0, tzinfo=IST), time(20, 0, tzinfo=IST)],  # 8 AM, 2 PM, 8 PM
    4: [time(8, 0, tzinfo=IST), time(12, 0, tzinfo=IST), time(16, 0, tzinfo=IST), time(20, 0, tzinfo=IST)],  # 8 AM, 12 PM, 4 PM, 8 PM
}

def _dose_label_for_time(t: time) -> str:
    """Map a time-of-day to a human dose label.
    05:00–11:59 -> Morning
    12:00–16:59 -> Afternoon
    17:00–20:59 -> Evening
    otherwise   -> Bedtime
    """
    h = t.hour
    m = t.minute
    # Normalize into ranges in IST
    if 5 <= h <= 11:
        return "Morning"
    if 12 <= h <= 16:
        return "Afternoon"
    if 17 <= h <= 20:
        return "Evening"
    return "Bedtime"


def _parse_times_per_day_from_item(item: dict) -> int:
    """
    Extract times-per-day from frequency field. Common patterns:
      "Twice daily", "3 times daily", "Once daily", "Once a day", "2 times a day"
    Returns 1-4 (default 1).
    """
    freq = str(item.get("frequency") or "").lower()
    if "twice" in freq or "2 times" in freq or "bd" in freq:
        return 2
    if "thrice" in freq or "3 times" in freq or "td" in freq:
        return 3
    if "four" in freq or "4 times" in freq or "qid" in freq:
        return 4
    if "once" in freq or "1 time" in freq or "od" in freq or "daily" in freq:
        return 1
    return 1


# ============ END MEDICINE SCHEDULING HELPERS ============


# ---------- Routes --------
@app.post("/login")
async def login(request: LoginRequest):
    print(f"[LOGIN] FUNCTION CALLED - email: {request.email}, password: {repr(request.password)}")
    
    if not request.email or not request.password:
        print(f"[LOGIN] ERROR: Email or password is empty")
        raise HTTPException(status_code=400, detail="Email and password are required")

    print(f"[LOGIN] Attempting to find user with email: {request.email}")
    user = await _find_user_by_email_or_contact(request.email)
    print(f"[LOGIN] User found: {user is not None}")
    if not user:
        print(f"[LOGIN] User NOT found - raising 404")
        raise HTTPException(status_code=404, detail="Invalid email or password")

    # Use two-tier password extraction helper (robust to all schema variations)
    stored_pw = _extract_password_from_user(user)
    if not stored_pw:
        print(f"[LOGIN] ERROR: No valid password found in user document after searching all locations")
        raise HTTPException(status_code=401, detail="Invalid email or password")
    
    print(f"[LOGIN] DEBUG - Password extraction successful:")
    print(f"[LOGIN]   - stored_pw type: {type(stored_pw)}")
    print(f"[LOGIN]   - stored_pw length: {len(str(stored_pw)) if stored_pw else 0}")

    # DEBUG: Log request password
    print(f"[LOGIN] DEBUG - Request password:")
    print(f"[LOGIN]   - request.password: {repr(request.password)}")
    print(f"[LOGIN]   - request.password type: {type(request.password)}")
    print(f"[LOGIN]   - request.password length: {len(request.password)}")

    # Accept both hashed and legacy plain-text passwords.
    # If stored_pw is a passlib hash, pwd_context.identify will return a scheme name;
    # otherwise fall back to a direct string comparison (for legacy records).
    try:
        is_hash = pwd_context.identify(stored_pw)
        print(f"[LOGIN] DEBUG - Password verification:")
        print(f"[LOGIN]   - is_hash (pwd_context.identify): {is_hash}")
        
        if is_hash:
            print(f"[LOGIN]   - Using bcrypt verification (hash detected)")
            valid = pwd_context.verify(request.password, stored_pw)
            print(f"[LOGIN]   - Hash verification result: {valid}")
        else:
            # legacy plaintext password stored in DB
            print(f"[LOGIN]   - Using plaintext comparison (no hash detected)")
            print(f"[LOGIN]   - Comparing: {repr(request.password)} == {repr(stored_pw)}")
            valid = (request.password == stored_pw)
            print(f"[LOGIN]   - Plaintext comparison result: {valid}")
    except Exception as e:
        print(f"[LOGIN] DEBUG - Password verification EXCEPTION: {e}")
        valid = False

    if not valid:
        raise HTTPException(status_code=401, detail="Invalid email or password")

    user_safe = deepcopy(user)
    user_safe.pop("password", None)
    user_safe.pop("hashed_password", None)

    user_safe = _expose_compat_fields(user_safe)
    return serialize_doc(user_safe)


@app.get("/patients")
async def get_all_patients():
    patients: List[Dict[str, Any]] = []
    cursor = patients_collection.find()
    async for p in cursor:
        p_safe = _expose_compat_fields(serialize_doc(p))
        patients.append(p_safe)
    return patients


@app.get("/patients/{patient_id}")
async def get_patient(patient_id: str):
    try:
        oid = ObjectId(patient_id)
        q = {"_id": oid}
    except Exception:
        q = {"_id": patient_id}
    p = await patients_collection.find_one(q)
    if not p:
        raise HTTPException(status_code=404, detail="Patient not found")
    p_safe = _expose_compat_fields(serialize_doc(p))
    return p_safe

# --------------- Agora Call Endpoints ---------------
# Generate a temporary RTC token for a given channel/uid
@app.post("/call/token")
async def generate_agora_token(channel_name: str, role: int = 1, uid: int = 0):
    expiration_time_in_seconds = 3600
    current_timestamp = int(time_module.time())
    privilege_expired_ts = current_timestamp + expiration_time_in_seconds

    token = RtcTokenBuilder.buildTokenWithUid(
        AGORA_APP_ID,
        AGORA_APP_CERTIFICATE,
        channel_name,
        uid,
        role,
        privilege_expired_ts,
    )
    return {"token": token, "app_id": AGORA_APP_ID}

# High-priority call notification helper to ensure ringing on devices
async def _send_call_notification(token: str, channel_name: str, agora_token: str, doctor_id: str):
    """
    Sends a HIGH PRIORITY notification specifically for calls.
    Forces the 'high_importance_channel' so it rings on Android.
    """
    if not firebase_admin._apps:
        print("[ERROR] Firebase not initialized, cannot send call notification")
        return False

    data_payload = {
        "type": "incoming_call",
        "channel_name": channel_name,
        "agora_token": agora_token,
        "app_id": AGORA_APP_ID,
        "doctor_id": doctor_id,
    }

    message = messaging.Message(
        token=token,
        notification=messaging.Notification(
            title="Incoming Video Call",
            body="Tap to answer..."
        ),
        data=data_payload,
        android=messaging.AndroidConfig(
            priority='high',
            ttl=60,
            notification=messaging.AndroidNotification(
                channel_id='call_channel_id',  # MUST MATCH FLUTTER ID
                priority='max',
                visibility='public',
                sound='ring',  # File name without extension
                click_action='FLUTTER_NOTIFICATION_CLICK'
            )
        ),
        apns=messaging.APNSConfig(
            payload=messaging.APNSPayload(
                aps=messaging.Aps(
                    content_available=True,
                    sound='ring.mp3'  # File name with extension for iOS
                )
            )
        )
    )

    try:
        response = await asyncio.to_thread(lambda: messaging.send(message))
        print(f"[CALL_PUSH] Successfully sent call notification: {response}")
        return True
    except Exception as e:
        print(f"[CALL_PUSH] Failed to send call notification: {e}")
        return False

# Initiate a call: generate patient token and send FCM push with call info
@app.post("/call/initiate")
async def initiate_call(payload: CallInitiateRequest):
    # Generate token for patient (uid 0 by convention)
    expiration_time_in_seconds = 3600
    current_timestamp = int(time_module.time())
    privilege_expired_ts = current_timestamp + expiration_time_in_seconds

    patient_token = RtcTokenBuilder.buildTokenWithUid(
        AGORA_APP_ID,
        AGORA_APP_CERTIFICATE,
        payload.channel_name,
        0,
        1,
        privilege_expired_ts,
    )

    # Lookup patient's FCM token
    fcm_doc = await fcm_tokens_collection.find_one({"patient_id": payload.patient_id})
    if not fcm_doc or not fcm_doc.get("token"):
        raise HTTPException(status_code=404, detail="Patient is not online (No FCM token found)")

    target_token = fcm_doc["token"]

    # Send HIGH PRIORITY call notification
    await _send_call_notification(
        token=target_token,
        channel_name=payload.channel_name,
        agora_token=patient_token,
        doctor_id=payload.doctor_id,
    )

    return {"status": "calling", "channel": payload.channel_name}


@app.post('/patients', status_code=201)
async def create_patient(payload: Dict[str, Any]):
    """Create a new patient document.

    Accepts a full patient JSON document. If the payload contains an
    `_id` field that is a dict with `$oid`, that value will be used as the
    string id. The document is inserted as-is into the `patients` collection.
    """
    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail='invalid payload')
    # normalize _id if provided as {'$oid': '...'}
    pid = None
    if payload.get('_id') and isinstance(payload['_id'], dict) and payload['_id'].get('$oid'):
        pid = payload['_id']['$oid']
        payload['_id'] = pid
    # ensure created_at exists
    payload.setdefault('created_at', datetime.utcnow().isoformat())
    res = await patients_collection.insert_one(payload)
    return {'status': 'created', 'patient_id': str(res.inserted_id)}


@app.put('/patients/{patient_id}')
async def upsert_patient(patient_id: str, payload: Dict[str, Any]):
    """Update or insert (upsert) a patient document by patient_id.

    This treats the `patient_id` as the document `_id` key (string). If you
    prefer ObjectId semantics, pass an actual ObjectId string and the client
    can still read it back; the server will store the id value as provided.
    Accepts both old schema (top-level fields) and new schema (patientDetails).
    """
    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail='invalid payload')
    # set the _id in payload to the provided patient_id to keep things consistent
    payload['_id'] = patient_id
    payload.setdefault('updated_at', datetime.utcnow().isoformat())
    # Ensure lastUpdated is set for new schema
    payload.setdefault('lastUpdated', datetime.utcnow().isoformat())
    await patients_collection.update_one({'_id': patient_id}, {'$set': payload}, upsert=True)
    # return the upserted doc for convenience
    doc = await patients_collection.find_one({'_id': patient_id})
    if not doc:
        raise HTTPException(status_code=500, detail='failed to upsert patient')
    return _expose_compat_fields(serialize_doc(doc))


@app.post("/submissions", status_code=201)
async def create_submission(
    patient_id: str = Form(...),
    patient_name: str = Form(...),
    doctor_id: str = Form(...),
    pain_scale: int = Form(...),
    vision_blur: int = Form(0),
    swelling: int = Form(0),
    redness: int = Form(0),
    watering: int = Form(0),
    itching: int = Form(0),
    discharge: int = Form(0),
    comments: Optional[str] = Form(None),
    image: UploadFile = File(...),
):
    file_id = await gridfs_bucket.upload_from_stream(image.filename, image.file, metadata={"contentType": image.content_type})

    submission = PatientSubmission(
        patient_id=patient_id,
        patient_name=patient_name,
        doctor_id=doctor_id,
        pain_scale=pain_scale,
        vision_blur=vision_blur,
        swelling=swelling,
        redness=redness,
        watering=watering,
        itching=itching,
        discharge=discharge,
        comments=comments,
        image_file_id=str(file_id),
    )

    submission_dict = submission.dict(exclude={"id"}, by_alias=True)
    # add created timestamp if missing and keep flexible extra fields
    submission_dict.setdefault("created_at", datetime.utcnow().isoformat())
    result = await submissions_collection.insert_one(submission_dict)
    new_submission_id = str(result.inserted_id)

    # Auto-create a linked message so it appears in Notifications and links to the submission
    try:
        message_doc = {
            "patient_id": patient_id,
            "doctor_id": doctor_id,
            "sender": "system",
            "note_text": "Report received. Waiting for doctor review.",
            "submission_id": new_submission_id,
            "timestamp": datetime.utcnow(),
        }
        await messages_collection.insert_one(message_doc)
    except Exception as e:
        # Do not fail submission if message insertion fails
        print(f"[SUBMISSION] Linked message insert failed: {e}")

    return {"message": "Submission created", "submission_id": new_submission_id}


@app.get("/patients/{patient_id}/messages")
async def get_messages_for_patient(patient_id: str):
    """Return messages for a patient, enriched with linked submission details.

    If a message contains a `submission_id` (string), this endpoint joins the
    corresponding document from `patient_submissions` and returns it under
    `submission_details` so clients can show the original photo and symptom values.
    """
    pipeline = [
        {"$match": {"patient_id": patient_id}},
        {
            "$addFields": {
                "sub_oid": {
                    "$cond": [
                        {"$and": [{"$gt": ["$submission_id", None]}, {"$ne": ["$submission_id", ""]}]},
                        {"$toObjectId": "$submission_id"},
                        None,
                    ]
                }
            }
        },
        {
            "$lookup": {
                "from": "patient_submissions",
                "localField": "sub_oid",
                "foreignField": "_id",
                "as": "submission_details",
            }
        },
        {"$unwind": {"path": "$submission_details", "preserveNullAndEmptyArrays": True}},
        {"$sort": {"timestamp": -1}},
    ]

    messages: List[Dict[str, Any]] = []
    cursor = messages_collection.aggregate(pipeline)
    async for doc in cursor:
        messages.append(serialize_doc(doc))
    return messages

# utility: store token into fcm_tokens collection (unique)
@app.post("/patients/{patient_id}/fcm-token", status_code=201)
async def register_fcm_token(patient_id: str, payload: Dict[str, Any]):
    """
    payload JSON: { "token": "<fcm token>", "platform": "android|ios|web" (optional) }
    Stores token per patient in a dedicated collection.
    """
    token = payload.get("token") if isinstance(payload, dict) else None
    platform = payload.get("platform") if isinstance(payload, dict) else None
    if not token:
        raise HTTPException(status_code=400, detail="token required")
    now = datetime.utcnow().isoformat()
    doc = {
        "patient_id": patient_id,
        "token": token,
        "platform": platform,
        "created_at": now,
        "last_seen_at": now,
    }
    # upsert by token
    try:
        print(f"DEBUG: registering token for patient_id={patient_id} token={token[:20]}...")
    except Exception:
        print("DEBUG: registering token (could not format token)")
    await fcm_tokens_collection.update_one({"token": token}, {"$set": doc}, upsert=True)
    return {"status": "ok", "token": token}

async def _send_fcm_to_tokens(title: str, body: str, data: Dict[str, str], tokens: List[str]) -> Dict[str, Any]:
    """
    Send using firebase_admin.messaging.send_each_for_multicast (Required for v7.0+).
    """
    if not firebase_admin._apps:
        raise RuntimeError("Firebase admin not initialized")
    
    results_summary = {"success": 0, "failure": 0, "errors": []}
    
    async def _send_batch(batch):
        try:
            def sync_send():
                # 1. Create the MulticastMessage object
                message = messaging.MulticastMessage(
                    notification=messaging.Notification(title=title, body=body),
                    data=data or {},
                    tokens=batch,
                )
                # 2. Use the NEW method: send_each_for_multicast
                return messaging.send_each_for_multicast(message)
            
            # Run the sync function in a thread so it doesn't block the server
            resp = await asyncio.to_thread(sync_send)
            
            results_summary["success"] += resp.success_count
            results_summary["failure"] += resp.failure_count
            
            # 3. Collect errors
            for idx, resp_item in enumerate(resp.responses):
                if not resp_item.success:
                    results_summary["errors"].append({
                        "token": batch[idx], 
                        "error": str(resp_item.exception)
                    })
        except Exception as e:
            results_summary["errors"].append({"batch_error": str(e)})

    # Batch processing (FCM limit is 500 tokens per batch)
    batch_size = 500
    tasks = []
    for i in range(0, len(tokens), batch_size):
        batch = tokens[i:i+batch_size]
        tasks.append(_send_batch(batch))
    
    await asyncio.gather(*tasks)
    return results_summary

async def _collect_target_tokens(recipients: Dict[str, Any]) -> List[str]:
    """
    recipients can be:
      { "all": true }  -> all tokens
      { "patients": ["id1","id2"] } -> tokens for those patients
      { "emails": ["a@b.com", "c@d.com"] } -> lookup IDs from emails, then tokens
      { "tokens": ["t1","t2"] } -> explicit tokens
    """
    if not recipients:
        return []
    
    # 1. Handle "All"
    if recipients.get("all"):
        cursor = fcm_tokens_collection.find({}, {"token": 1})
        tokens = []
        async for d in cursor:
            t = d.get("token")
            if t: tokens.append(t)
        return tokens

    # 2. Handle "Patients" (IDs)
    if recipients.get("patients"):
        patients = recipients.get("patients") or []
        cursor = fcm_tokens_collection.find({"patient_id": {"$in": patients}}, {"token": 1})
        tokens = []
        async for d in cursor:
            t = d.get("token")
            if t: tokens.append(t)
        return tokens

    # 3. Handle "Emails" (NEW LOGIC)
    if recipients.get("emails"):
        email_list = recipients.get("emails") or []
        # Find patient IDs that match these emails (checking both old schema and new schema paths)
        patient_cursor = patients_collection.find({
            "$or": [
                {"email": {"$in": email_list}},
                {"contactInfo.email": {"$in": email_list}},
                {"patientDetails.email": {"$in": email_list}}
            ]
        }, {"_id": 1})
        
        found_ids = []
        async for p in patient_cursor:
            found_ids.append(str(p["_id"]))
            
        if found_ids:
            # Now find tokens for these IDs
            token_cursor = fcm_tokens_collection.find({"patient_id": {"$in": found_ids}}, {"token": 1})
            tokens = []
            async for d in token_cursor:
                t = d.get("token")
                if t: tokens.append(t)
            return tokens
            token_cursor = fcm_tokens_collection.find({"patient_id": {"$in": found_ids}}, {"token": 1})
            tokens = []
            async for d in token_cursor:
                t = d.get("token")
                if t: tokens.append(t)
            return tokens

    # 4. Handle Explicit "Tokens"
    if recipients.get("tokens"):
        return list(recipients.get("tokens"))

    return []


@app.post('/debug/send-to-token')
async def debug_send_to_token(payload: Dict[str, Any]):
    """Development helper: send a notification to a single token and return send_result.

    Payload: { "token": "<fcm token>", "title": "...", "body": "...", "data": { ... } }
    """
    token = payload.get('token') if isinstance(payload, dict) else None
    title = payload.get('title') if isinstance(payload, dict) else ''
    body = payload.get('body') if isinstance(payload, dict) else ''
    data = payload.get('data') if isinstance(payload, dict) else {}
    if not token:
        raise HTTPException(status_code=400, detail='token required')
    if not firebase_admin._apps:
        raise HTTPException(status_code=500, detail='firebase admin not initialized')
    try:
        result = await _send_fcm_to_tokens(title, body, data or {}, [token])
        return {"status": "sent", "result": result}
    except Exception as ex:
        raise HTTPException(status_code=500, detail=str(ex))


@app.get('/debug/notifications/recent')
async def debug_recent_notifications(limit: int = 20):
    """Return recent notifications (dev helper)."""
    docs = []
    cursor = notifications_collection.find().sort('created_at', -1).limit(limit)
    async for d in cursor:
        docs.append(serialize_doc(d))
    return {"count": len(docs), "notifications": docs}

# endpoint: create notification and auto-send
@app.post("/notifications", status_code=201)
async def create_notification(payload: Dict[str, Any]):
    """
    Insert notification doc and immediately send via FCM.
    Expected payload fields (example):
    {
      "doctor_id": "...",
      "doctor_name": "...",
      "title": "Title",
      "message": "Body",
      "recipients": { "all": true } or { "patients": ["id"] } or { "tokens": ["..."] },
      "image_file_ids": []
    }
    """
    doc = deepcopy(payload)
    doc.setdefault("created_at", datetime.utcnow().isoformat())
    doc.setdefault("sent", False)
    doc.setdefault("delivery", {})
    result = await notifications_collection.insert_one(doc)
    notif_id = str(result.inserted_id)
    # perform sending in background (do not block long)
    async def _send_and_update():
        try:
            recipients = doc.get("recipients", {})
            tokens = await _collect_target_tokens(recipients)
            # debug: log recipient resolution so we can diagnose missing tokens
            try:
                print(f"DEBUG: notification {notif_id} recipients={recipients} -> tokens_found={len(tokens)}")
            except Exception:
                # avoid logging errors from formatting
                print("DEBUG: notification recipients resolved (could not format details)")
            if not tokens:
                await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": False, "sent_at": None, "delivery": {"error": "no_tokens"}}})
                return
            data_payload = {"type": "doctor_notification", "notification_id": notif_id}
            send_result = await _send_fcm_to_tokens(doc.get("title",""), doc.get("message",""), data_payload, tokens)
            # debug: surface send result in server logs for quick diagnosis
            try:
                print(f"DEBUG: notification {notif_id} send_result={send_result}")
            except Exception:
                print("DEBUG: notification send_result available (could not format)")
            # prune tokens with unrecoverable errors (example contains "NotRegistered" or "InvalidArgument")
            to_remove = []
            for err in send_result.get("errors", []):
                e = err.get("error","")
                token = err.get("token")
                if token and ("NotRegistered" in e or "invalid" in e.lower() or "registration-token-not-registered" in e):
                    to_remove.append(token)
            if to_remove:
                await fcm_tokens_collection.delete_many({"token": {"$in": to_remove}})
            await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": True, "sent_at": datetime.utcnow().isoformat(), "delivery": send_result}})
        except Exception as ex:
            await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": False, "delivery": {"error": str(ex)}}})
    # schedule background task
    asyncio.create_task(_send_and_update())
    return {"status": "queued", "notification_id": notif_id}


@app.post("/patients/{patient_id}/schedule-meds", status_code=201)
async def schedule_patient_medicine_notifications(patient_id: str):
    """
    Schedule medicine notifications for a patient based on their prescription.
    
    For each medicine item:
    - Parses frequency to determine times per day (1x, 2x, 3x, 4x)
    - Parses duration to determine how many days to schedule
    - Uses prescription start date (datePrescribed) or patient's lastVisit or now (IST)
    - Creates notification documents with send_at times (IST)
    
    Returns list of created notification IDs.
    """
    # Load patient
    p = await patients_collection.find_one({"_id": patient_id})
    if not p:
        try:
            p = await patients_collection.find_one({"_id": ObjectId(patient_id)})
        except:
            p = None
    if not p:
        raise HTTPException(status_code=404, detail="patient not found")

    # Find prescription in visits (new schema) or top-level (old schema)
    prescription = None
    visits = p.get("visits") or []
    if visits:
        last = visits[-1]
        doc_stage = (last.get("stages") or {}).get("doctor") or {}
        data = doc_stage.get("data") or {}
        prescription = data.get("prescription")
    
    if not prescription:
        prescription = p.get("prescription")

    if not prescription:
        raise HTTPException(status_code=400, detail="no prescription found for patient")

    # Compute start date (IST)
    start_dt = _parse_start_date_from_prescription(prescription, p)

    created_docs = []
    items = prescription.get("items") or []
    
    for item in items:
        med_name = item.get("name") or item.get("drug") or "Medicine"
        
        # Parse times per day from frequency
        times_per_day = _parse_times_per_day_from_item(item)
        times_list = _DAYSCHEDULES.get(times_per_day, _DAYSCHEDULES.get(1))

        # Parse duration from item (or use default 7 days)
        duration_raw = item.get("duration") or (prescription.get("summary") or {}).get("duration")
        duration_days = _parse_duration_days(duration_raw, default_days=7)

        # Build send datetimes for duration starting at start_dt
        send_datetimes = _make_send_datetimes_for_duration(times_list, start_dt, duration_days)

        # Create notification document for each send time
        for send_at in send_datetimes:
            title = f"Medicine Reminder: {med_name}"
            dose = item.get("dosage") or ""
            freq = item.get("frequency") or ""
            message = f"{med_name} {dose} {freq}".strip()
            
            doc = {
                "patient_id": patient_id,
                "title": title,
                "message": message,
                "recipients": {"patients": [patient_id]},
                "send_at": send_at,
                "sent": False,
                "created_at": datetime.utcnow().isoformat(),
                "metadata": {"med_item": item},
            }
            res = await notifications_collection.insert_one(doc)
            created_docs.append(str(res.inserted_id))

            # Also upsert a dose record in medicine_doses (single source of truth for dose slots)
            try:
                dt = datetime.fromisoformat(str(send_at).replace("Z", "+00:00"))
            except Exception:
                # if parsing fails, skip dose creation for this time
                dt = None
            if dt:
                dt_ist = dt.astimezone(IST) if dt.tzinfo else dt.replace(tzinfo=IST)
                date_str = dt_ist.date().isoformat()
                time_str = dt_ist.strftime("%H:%M")
                label = _dose_label_for_time(dt_ist.timetz())
                dose_filter = {
                    "patient_id": patient_id,
                    "medicine_name": med_name,
                    "date": date_str,
                    "dose_label": label,
                }
                dose_doc = {
                    **dose_filter,
                    "doctor_id": p.get("doctor_id") or (p.get("doctor") or {}).get("_id"),
                    "scheduled_time": time_str,
                    "scheduled_iso": dt_ist.isoformat(),
                    "taken": False,
                    "taken_at": None,
                    "notified": False,
                    "created_at": datetime.utcnow().isoformat(),
                    "notification_id": str(res.inserted_id),
                }
                await medicine_doses_collection.update_one(
                    dose_filter,
                    {"$setOnInsert": dose_doc},
                    upsert=True,
                )

    return {
        "status": "scheduled",
        "patient_id": patient_id,
        "created_count": len(created_docs),
        "notification_ids": created_docs,
        "duration_days": duration_days,
        "start_date": start_dt.isoformat(),
    }


# --- Dose endpoints ---
@app.get("/patients/{patient_id}/today-doses")
async def get_today_doses(patient_id: str):
    """Return today's dose slots for a patient (IST).
    Lazily creates today's doses from latest prescription if none exist.
    """
    today_ist = datetime.now(IST).date().isoformat()
    cursor = medicine_doses_collection.find({"patient_id": patient_id, "date": today_ist}).sort("scheduled_iso", 1)
    doses = [serialize_doc(d) async for d in cursor]

    if not doses:
        # Attempt lazy creation: derive today's schedule from latest prescription
        p = await patients_collection.find_one({"_id": patient_id})
        if not p:
            try:
                p = await patients_collection.find_one({"_id": ObjectId(patient_id)})
            except Exception:
                p = None
        if not p:
            raise HTTPException(status_code=404, detail="patient not found")

        # Find latest prescription (visits preferred)
        prescription = None
        visits = p.get("visits") or []
        if isinstance(visits, list) and visits:
            last = visits[-1]
            doc_stage = (last.get("stages") or {}).get("doctor") or {}
            data = doc_stage.get("data") or {}
            prescription = data.get("prescription")
        if not prescription:
            prescription = p.get("prescription")

        if not isinstance(prescription, dict):
            return []
        items = prescription.get("items") or []
        # For today, generate dose slots for each item based on frequency
        for item in items:
            med_name = item.get("name") or item.get("drug") or "Medicine"
            times_per_day = _parse_times_per_day_from_item(item)
            times_list = _DAYSCHEDULES.get(times_per_day, _DAYSCHEDULES.get(1))
            for t in times_list:
                # Build today's datetime in IST
                dt_ist = datetime.combine(datetime.now(IST).date(), t, tzinfo=IST)
                label = _dose_label_for_time(dt_ist.timetz())
                dose_filter = {
                    "patient_id": patient_id,
                    "medicine_name": med_name,
                    "date": today_ist,
                    "dose_label": label,
                }
                dose_doc = {
                    **dose_filter,
                    "doctor_id": p.get("doctor_id") or (p.get("doctor") or {}).get("_id"),
                    "scheduled_time": dt_ist.strftime("%H:%M"),
                    "scheduled_iso": dt_ist.isoformat(),
                    "taken": False,
                    "taken_at": None,
                    "notified": False,
                    "created_at": datetime.utcnow().isoformat(),
                }
                await medicine_doses_collection.update_one(dose_filter, {"$setOnInsert": dose_doc}, upsert=True)

        # Re-fetch after creation
        cursor = medicine_doses_collection.find({"patient_id": patient_id, "date": today_ist}).sort("scheduled_iso", 1)
        doses = [serialize_doc(d) async for d in cursor]

    return doses


@app.post("/patients/{patient_id}/doses/{dose_id}/take")
async def take_dose(patient_id: str, dose_id: str):
    """Mark a specific dose as taken. Idempotent.
    Also writes to adherence_collection for analytics.
    """
    try:
        oid = ObjectId(dose_id)
    except Exception:
        raise HTTPException(status_code=400, detail="invalid dose_id")

    dose = await medicine_doses_collection.find_one({"_id": oid, "patient_id": patient_id})
    if not dose:
        raise HTTPException(status_code=404, detail="dose not found")

    if dose.get("taken"):
        # Already taken: return success without changes
        return {"status": "already_taken", "dose_id": dose_id}

    now_iso = datetime.utcnow().isoformat()
    await medicine_doses_collection.update_one({"_id": oid}, {"$set": {"taken": True, "taken_at": now_iso}})

    # Also record adherence for analytics
    try:
        doc = {
            "patient_id": patient_id,
            "patient_name": None,
            "doctor_id": dose.get("doctor_id"),
            "doctor_name": None,
            "medicine": dose.get("medicine_name"),
            "taken": 1,
            "created_at": now_iso,
        }
        await adherence_collection.insert_one(doc)
    except Exception:
        pass

    return {"status": "taken", "dose_id": dose_id}


@app.get("/patients/{patient_id}/next-dose")
async def next_dose(patient_id: str):
    """Return next upcoming untaken dose slot for a patient (IST)."""
    now_ist = datetime.now(IST).isoformat()
    cursor = medicine_doses_collection.find({
        "patient_id": patient_id,
        "taken": False,
        "scheduled_iso": {"$gte": now_ist},
    }).sort("scheduled_iso", 1).limit(1)
    doses = [serialize_doc(d) async for d in cursor]
    if not doses:
        return {"next": None}
    d = doses[0]
    return {"next": d}


# --- Debug endpoints (safe for development only) ---
@app.get('/debug/firebase')
async def debug_firebase():
    """Return firebase-admin initialization state for quick checks."""
    initialized = bool(firebase_admin._apps) or FIREBASE_INITIALIZED
    app_names = list(firebase_admin._apps.keys()) if bool(firebase_admin._apps) else []
    return {"firebase_initialized": initialized, "apps": app_names, "last_error": LAST_FIREBASE_ERROR}


@app.get('/debug/doctor-backend')
async def debug_doctor_backend():
    """Return the configured doctor backend URL and whether forwarding is enabled."""
    return {"doctor_backend_url": DOCTOR_BACKEND_URL or None, "forwarding_enabled": bool(DOCTOR_BACKEND_URL)}


@app.get('/debug/patient/{patient_id}/tokens')
async def debug_patient_tokens(patient_id: str):
    """Return tokens stored for a patient (development helper)."""
    tokens = []
    cursor = fcm_tokens_collection.find({"patient_id": patient_id})
    async for d in cursor:
        tokens.append({"token": d.get("token"), "platform": d.get("platform"), "last_seen_at": d.get("last_seen_at")})
    return {"patient_id": patient_id, "count": len(tokens), "tokens": tokens}


@app.post('/tokens', status_code=201)
async def push_token(payload: Dict[str, Any]):
    """Generic endpoint to push an FCM token into MongoDB.

    Accepts JSON: { "patient_id": "<id>", "token": "<fcm token>", "platform": "android|ios|web" }
    This is a convenience endpoint for clients that can't call the patient-scoped route.
    """
    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail="invalid payload")
    patient_id = payload.get("patient_id")
    token = payload.get("token")
    platform = payload.get("platform")
    if not patient_id or not token:
        raise HTTPException(status_code=400, detail="patient_id and token required")
    now = datetime.utcnow().isoformat()
    doc = {
        "patient_id": patient_id,
        "token": token,
        "platform": platform,
        "created_at": now,
        "last_seen_at": now,
    }
    try:
        print(f"DEBUG: push_token patient_id={patient_id} token={token[:20]}...")
    except Exception:
        print("DEBUG: push_token (could not format token)")
    await fcm_tokens_collection.update_one({"token": token}, {"$set": doc}, upsert=True)
    return {"status": "ok", "token": token}


@app.post('/adherence', status_code=201)
async def record_adherence(payload: Dict[str, Any]):
    """Record medicine adherence for a patient.

    Expected JSON:
    {
      "patient_id": "...",
      "patient_name": "...",
      "doctor_id": "...",
      "doctor_name": "...",
      "medicine": "Paracetamol",
      "taken": 1  # 1 = taken, 0 = not taken
    }
    """
    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail="invalid payload")
    patient_id = payload.get("patient_id")
    medicine = payload.get("medicine")
    taken = payload.get("taken")
    if not patient_id or medicine is None or taken is None:
        raise HTTPException(status_code=400, detail="patient_id, medicine and taken are required")
    now = datetime.utcnow().isoformat()
    doc = {
        "patient_id": patient_id,
        "patient_name": payload.get("patient_name"),
        "doctor_id": payload.get("doctor_id"),
        "doctor_name": payload.get("doctor_name"),
        "medicine": medicine,
        "taken": int(taken),
        "created_at": now,
    }
    res = await adherence_collection.insert_one(doc)
    return {"status": "ok", "id": str(res.inserted_id)}


@app.post('/vision-tests', status_code=201)
async def create_vision_test(payload: Dict[str, Any]):
    """Accept vision test results JSON and store in `visiontests` collection.

    Expected JSON shape:
    {
      "patient_id": "...",
      "patient_name": "...",
      "timestamp": "2025-11-28T...",
      "logMAR_levels": [0.0, 0.1, ...],
      "sessions": [ {"session":0, "level":0.0, "correct": true}, ... ]
    }
    """
    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail='invalid payload')
    patient_id = payload.get('patient_id')
    patient_name = payload.get('patient_name')
    if not patient_id or not patient_name:
        raise HTTPException(status_code=400, detail='patient_id and patient_name required')

    doc = {
        'patient_id': patient_id,
        'patient_name': patient_name,
        'timestamp': payload.get('timestamp') or datetime.utcnow().isoformat(),
        'logMAR_levels': payload.get('logMAR_levels') or [],
        'sessions': payload.get('sessions') or [],
        'created_at': datetime.utcnow().isoformat(),
    }
    result = await visiontests_collection.insert_one(doc)
    return {'status': 'created', 'vision_test_id': str(result.inserted_id)}


@app.get('/adherence/patient/{patient_id}')
async def get_adherence_for_patient(patient_id: str, limit: int = 200):
    docs = []
    cursor = adherence_collection.find({"patient_id": patient_id}).sort('created_at', -1).limit(limit)
    async for d in cursor:
        docs.append(serialize_doc(d))
    return {"patient_id": patient_id, "count": len(docs), "adherence": docs}

# manual send endpoint for existing notification document
@app.post("/notifications/{notif_id}/send")
async def send_existing_notification(notif_id: str):
    n = await notifications_collection.find_one({"_id": ObjectId(notif_id)})
    if not n:
        raise HTTPException(status_code=404, detail="notification not found")
    # If a doctor backend URL is configured, forward this send request to it.
    if DOCTOR_BACKEND_URL:
        forward_url = DOCTOR_BACKEND_URL.rstrip('/') + f'/notifications/{notif_id}/send'
        try:
            # Convert ObjectId to string for JSON serialization
            n_serializable = deepcopy(n)
            if "_id" in n_serializable:
                n_serializable["_id"] = str(n_serializable["_id"])
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.post(forward_url, json=n_serializable)
                body_text = None
                try:
                    body_text = resp.json()
                except Exception:
                    body_text = resp.text
                delivery = {"forwarded_to_doctor": True, "status_code": resp.status_code, "response": body_text}
                sent_flag = resp.status_code in (200, 201)
                await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": sent_flag, "sent_at": datetime.utcnow().isoformat() if sent_flag else None, "delivery": delivery}})
                return {"status": "forwarded", "doctor_status": resp.status_code, "doctor_response": body_text}
        except Exception as fe:
            await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": False, "delivery": {"error": f"forward_failed: {fe}"}}})
            raise HTTPException(status_code=500, detail=f"forward_failed: {fe}")

    # Fallback to local send if doctor backend not configured
    recipients = n.get("recipients", {})
    tokens = await _collect_target_tokens(recipients)
    if not tokens:
        raise HTTPException(status_code=400, detail="no tokens found for recipients")
    try:
        data_payload = {"type": "doctor_notification", "notification_id": notif_id}
        send_result = await _send_fcm_to_tokens(n.get("title", ""), n.get("message", ""), data_payload, tokens)
        # remove bad tokens
        to_remove = []
        for err in send_result.get("errors", []):
            e = err.get("error", "")
            token = err.get("token")
            if token and ("NotRegistered" in e or "invalid" in e.lower() or "registration-token-not-registered" in e):
                to_remove.append(token)
        if to_remove:
            await fcm_tokens_collection.delete_many({"token": {"$in": to_remove}})
        await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": True, "sent_at": datetime.utcnow().isoformat(), "delivery": send_result}})
        return {"status": "sent", "result": send_result}
    except Exception as ex:
        await notifications_collection.update_one({"_id": ObjectId(notif_id)}, {"$set": {"sent": False, "delivery": {"error": str(ex)}}})
        raise HTTPException(status_code=500, detail=str(ex))


# ==========================================
# DIAGNOSTIC ENDPOINTS (DEVELOPMENT ONLY)
# ==========================================

@app.get('/debug/find-email/{email}')
async def debug_find_email(email: str):
    """
    Search entire patients collection for a given email string.
    Scans ALL documents and returns the structure where email is found.
    Used to diagnose login failures due to unknown email field locations.
    """
    print(f"[DEBUG_FIND_EMAIL] Searching for: {email}")
    
    # Get all patients and search programmatically
    all_patients = []
    cursor = patients_collection.find()
    async for p in cursor:
        all_patients.append(p)
    
    print(f"[DEBUG_FIND_EMAIL] Total patients in DB: {len(all_patients)}")
    
    found_in = []
    for patient in all_patients:
        # Convert to JSON string and search
        patient_str = str(patient).lower()
        email_lower = email.lower()
        
        if email_lower in patient_str:
            # Found it! Now figure out which field
            patient_id = str(patient.get("_id", "unknown"))
            name = patient.get("name", patient.get("patientDetails", {}).get("name", "Unknown"))
            
            # Check each known field
            locations = []
            if patient.get("email", "").lower() == email_lower:
                locations.append("email")
            if patient.get("contactInfo", {}).get("email", "").lower() == email_lower:
                locations.append("contactInfo.email")
            if patient.get("patientDetails", {}).get("email", "").lower() == email_lower:
                locations.append("patientDetails.email")
            
            found_in.append({
                "patient_id": patient_id,
                "name": name,
                "found_in_fields": locations,
                "full_patient_doc": serialize_doc(patient) if len(all_patients) < 10 else "..."  # Don't return huge docs
            })
    
    return {
        "search_email": email,
        "total_patients_in_db": len(all_patients),
        "matches_found": len(found_in),
        "matches": found_in
    }


# ==========================================
# BACKGROUND NOTIFICATION DISPATCHER

# ==========================================

async def notification_dispatcher_loop():
    """
    Background task that automatically sends medicine reminders at their scheduled time.
    Runs every 60 seconds to check for due notifications.
    """
    print("[DISPATCHER] Starting notification dispatcher background task...")
    while True:
        try:
            await asyncio.sleep(60)  # Check every 60 seconds
            now = datetime.now(IST)
            now_str = now.isoformat()  # Convert to ISO string for comparison

            # First: check dose slots that are due and not yet notified
            try:
                due_doses = await medicine_doses_collection.find({
                    "scheduled_iso": {"$lte": now_str},
                    "taken": False,
                    "notified": False,
                }).to_list(None)
                for dose in due_doses:
                    patient_id = dose.get("patient_id")
                    med_name = dose.get("medicine_name") or "Medicine"
                    label = dose.get("dose_label") or "Dose"
                    title = f"Time to take: {med_name}"
                    message = f"{label} dose now"
                    notif_doc = {
                        "patient_id": patient_id,
                        "title": title,
                        "message": message,
                        "recipients": {"patients": [patient_id]},
                        "send_at": now_str,
                        "sent": False,
                        "created_at": datetime.utcnow().isoformat(),
                        "metadata": {"type": "dose", "dose_id": str(dose.get("_id"))},
                    }
                    nres = await notifications_collection.insert_one(notif_doc)

                    tokens = await _collect_target_tokens({"patients": [patient_id]})
                    if tokens:
                        data_payload = {"type": "medicine_dose", "dose_id": str(dose.get("_id")), "notification_id": str(nres.inserted_id)}
                        result = await _send_fcm_to_tokens(title, message, data_payload, tokens)
                        await notifications_collection.update_one(
                            {"_id": nres.inserted_id},
                            {"$set": {"sent": True, "sent_at": datetime.utcnow().isoformat(), "delivery": result}}
                        )
                    # mark dose as notified regardless to avoid spamming
                    await medicine_doses_collection.update_one({"_id": dose.get("_id")}, {"$set": {"notified": True, "notified_at": datetime.utcnow().isoformat()}})
            except Exception as e:
                print(f"[DISPATCHER] [ERROR] Dose scan error: {e}")
            
            # Find notifications that are due (send_at <= now) and not yet sent
            # Note: send_at is stored as string in ISO format
            due_notifications = await notifications_collection.find({
                "send_at": {"$lte": now_str},
                "sent": False
            }).to_list(None)
            
            for notif in due_notifications:
                try:
                    notif_id = notif["_id"]
                    recipients = notif.get("recipients", {})
                    
                    # Collect FCM tokens for recipients
                    tokens = await _collect_target_tokens(recipients)
                    
                    if tokens:
                        # Send via FCM
                        data_payload = {
                            "type": "medicine_reminder",
                            "notification_id": str(notif_id)
                        }
                        result = await _send_fcm_to_tokens(
                            notif.get("title", ""),
                            notif.get("message", ""),
                            data_payload,
                            tokens
                        )
                        
                        # Remove bad tokens from database
                        to_remove = []
                        for err in result.get("errors", []):
                            e = err.get("error", "")
                            token = err.get("token")
                            if token and ("NotRegistered" in e or "invalid" in e.lower() or "registration-token-not-registered" in e):
                                to_remove.append(token)
                        if to_remove:
                            await fcm_tokens_collection.delete_many({"token": {"$in": to_remove}})
                        
                        # Mark notification as sent
                        await notifications_collection.update_one(
                            {"_id": notif_id},
                            {"$set": {
                                "sent": True,
                                "sent_at": datetime.utcnow().isoformat(),
                                "delivery": result
                            }}
                        )
                        
                        print(f"[DISPATCHER] [SENT] Notification {notif_id}: {notif.get('title', '')[:50]}")
                    else:
                        print(f"[DISPATCHER] [WARN] No tokens found for notification {notif_id}")
                        
                except Exception as e:
                    print(f"[DISPATCHER] [ERROR] Error sending notification: {e}")
                    
        except Exception as e:
            print(f"[DISPATCHER] [ERROR] Background task error: {e}")
            await asyncio.sleep(10)


# ==========================================
# WEEKLY ADHERENCE STATS ENDPOINT
# ==========================================

def _infer_expected_daily_meds_from_prescription(prescription: Dict[str, Any]) -> int:
    """Count distinct medicines expected daily based on prescription items.
    Frequency keywords interpreted as daily presence (OD/BD/TID/QID -> present).
    """
    if not isinstance(prescription, dict):
        return 0
    items = prescription.get("items") or []
    names = set()
    for it in items:
        if not isinstance(it, dict):
            # allow simple strings
            if isinstance(it, str) and it.strip():
                names.add(it.strip())
            continue
        name = (it.get("name") or it.get("drug") or it.get("medicine") or "").strip()
        freq = str(it.get("frequency") or "").lower()
        # treat any frequency indicating daily regimen as expected
        daily = False
        if any(k in freq for k in ["od", "once", "daily"]):
            daily = True
        if any(k in freq for k in ["bd", "twice", "2 times"]):
            daily = True
        if any(k in freq for k in ["tid", "thrice", "3 times"]):
            daily = True
        if any(k in freq for k in ["qid", "four", "4 times"]):
            daily = True
        # if frequency missing, assume daily presence
        if not freq:
            daily = True
        if daily and name:
            names.add(name)
    return len(names)


def _date_ist(dt: datetime) -> datetime:
    return dt.astimezone(IST)


@app.get("/adherence/stats/week/{patient_id}")
async def adherence_stats_week(patient_id: str, start: Optional[str] = None):
    """Return 7-day adherence stats (IST-based) for a patient.
    - For each day: taken = distinct medicines with taken==1 in adherence.
    - expected per day = distinct daily medicines from prescription.
    """
    # determine 7-day window
    now_ist = datetime.now(IST)
    if start:
        try:
            start_dt = datetime.fromisoformat(start)
            if start_dt.tzinfo is None:
                start_dt = start_dt.replace(tzinfo=IST)
            start_dt = start_dt.astimezone(IST)
        except Exception:
            raise HTTPException(status_code=400, detail="invalid start date")
    else:
        start_dt = _date_ist(now_ist).replace(hour=0, minute=0, second=0, microsecond=0) - timedelta(days=6)

    # end inclusive: start + 6 days
    days = [start_dt.date() + timedelta(days=i) for i in range(7)]

    # fetch adherence docs in window
    start_iso = datetime.combine(days[0], time(0,0), tzinfo=IST).isoformat()
    end_iso = datetime.combine(days[-1], time(23,59,59,999000), tzinfo=IST).isoformat()
    cursor = adherence_collection.find({
        "patient_id": patient_id,
        "$or": [
            {"created_at": {"$gte": start_iso, "$lte": end_iso}},
            {"timestamp": {"$gte": start_iso, "$lte": end_iso}},
        ]
    })

    per_day_taken: Dict[str, set] = {d.isoformat(): set() for d in days}

    async for doc in cursor:
        created = doc.get("created_at") or doc.get("timestamp")
        try:
            dt = datetime.fromisoformat(str(created).replace("Z", "+00:00"))
        except Exception:
            continue
        dt_ist = dt.astimezone(IST) if dt.tzinfo else dt.replace(tzinfo=IST)
        day_key = dt_ist.date().isoformat()
        taken = doc.get("taken")
        med = str(doc.get("medicine") or "").strip()
        if taken == 1 and med:
            if day_key in per_day_taken:
                per_day_taken[day_key].add(med)

    # compute expected per day from latest prescription
    prescription = None
    patient = await patients_collection.find_one({"_id": patient_id})
    if not patient:
        try:
            patient = await patients_collection.find_one({"_id": ObjectId(patient_id)})
        except Exception:
            patient = None
    if patient:
        visits = patient.get("visits") or []
        if isinstance(visits, list) and visits:
            last = visits[-1]
            doc_stage = (last.get("stages") or {}).get("doctor") or {}
            data = doc_stage.get("data") or {}
            prescription = data.get("prescription")
        if not prescription:
            prescription = patient.get("prescription")

    expected_daily = _infer_expected_daily_meds_from_prescription(prescription or {})

    weekly = []
    for d in days:
        key = d.isoformat()
        taken_count = len(per_day_taken.get(key, set()))
        weekly.append({
            "day": WEEKDAY_LABELS[d.weekday()],
            "date": key,
            "taken": taken_count,
            "expected": expected_daily,
        })

    summary = {
        "taken_total": sum(x["taken"] for x in weekly),
        "expected_total": expected_daily * 7,
        "adherence_rate": (sum(x["taken"] for x in weekly) / (expected_daily * 7)) if expected_daily > 0 else None,
    }

    return {
        "criteria": {
            "patient_id": patient_id,
            "start": days[0].isoformat(),
            "end": days[-1].isoformat(),
            "tz": "IST",
        },
        "weekly": weekly,
        "summary": summary,
    }


@app.on_event("startup")
async def start_background_tasks():
    """Start background notification dispatcher on app startup"""
    try:
        loop = asyncio.get_running_loop()
        task = loop.create_task(notification_dispatcher_loop())
        print("[DISPATCHER] Background task initialized")
        # Add a done callback to log if task exits unexpectedly
        def task_done_callback(t):
            try:
                t.result()
            except asyncio.CancelledError:
                pass  # Normal shutdown
            except Exception as e:
                print(f"[DISPATCHER] Task error: {e}")
        task.add_done_callback(task_done_callback)
    except Exception as e:
        print(f"[DISPATCHER] Startup error: {e}")


@app.on_event("shutdown")
async def shutdown_event():
    """Log when app is shutting down"""
    print("[SHUTDOWN] Application shutting down")