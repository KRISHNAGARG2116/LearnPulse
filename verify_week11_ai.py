#!/usr/bin/env python3
"""
Live End-to-End Verification Script for LearnPulse Week 11 Supplemental AI Learning Features.
Tests actual live running Spring Boot backend connected to local Ollama/Qwen (port 11434) and PostgreSQL (postgres-pgvector).
"""

import sys
import json
import time
import requests
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080"

# Colors for terminal printing
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
BLUE = "\033[94m"
RESET = "\033[0m"

def log_info(msg):
    print(f"{BLUE}[INFO]{RESET} {msg}")

def log_success(msg):
    print(f"{GREEN}[PASS]{RESET} {msg}")

def log_error(msg):
    print(f"{RED}[FAIL]{RESET} {msg}")

def log_warn(msg):
    print(f"{YELLOW}[WARN]{RESET} {msg}")

def get_auth_token(email, password, role="STUDENT"):
    login_url = f"{BASE_URL}/api/auth/login"
    login_data = {"email": email, "password": password}
    
    try:
        res = requests.post(login_url, json=login_data, timeout=5)
        if res.status_code == 200:
            data = res.json()
            token = data.get("data", {}).get("accessToken")
            if token:
                log_info(f"Logged in existing user ({email}) successfully.")
                return token
    except Exception as e:
        log_warn(f"Login attempt for {email} failed: {e}")

    # Register user if login fails
    register_url = f"{BASE_URL}/api/auth/register"
    register_data = {
        "email": email,
        "password": password,
        "firstName": "Test",
        "lastName": "User",
        "role": role
    }
    try:
        reg_res = requests.post(register_url, json=register_data, timeout=5)
        if reg_res.status_code in (200, 201):
            data = reg_res.json()
            token = data.get("data", {}).get("accessToken")
            log_info(f"Registered and logged in new user ({email}) with role {role}.")
            return token
        else:
            log_error(f"Registration failed for {email}: {reg_res.status_code} {reg_res.text}")
    except Exception as e:
        log_error(f"Registration request error for {email}: {e}")

    return None

def verify_week11_live():
    print("=" * 70)
    print("  LEARNPULSE AI LMS — WEEK 11 LIVE E2E VERIFICATION SCRIPT")
    print("=" * 70)
    
    # 1. User Authentication
    timestamp = int(time.time())
    student_email = f"student_live_{timestamp}@learnpulse.ai"
    teacher_email = f"teacher_live_{timestamp}@learnpulse.ai"
    password = "Password123!"

    log_info("Authenticating STUDENT and TEACHER accounts...")
    student_token = get_auth_token(student_email, password, role="STUDENT")
    teacher_token = get_auth_token(teacher_email, password, role="TEACHER")

    if not student_token:
        log_error("Failed to obtain STUDENT token. Ensure Spring Boot backend is running on http://localhost:8080")
        sys.exit(1)
    if not teacher_token:
        log_error("Failed to obtain TEACHER token.")
        sys.exit(1)

    headers_student = {
        "Authorization": f"Bearer {student_token}",
        "Content-Type": "application/json"
    }
    headers_teacher = {
        "Authorization": f"Bearer {teacher_token}",
        "Content-Type": "application/json"
    }

    results = []

    # 2. Security Test: Unauthenticated (401) & Non-STUDENT (403)
    log_info("Testing Security & RBAC constraints...")
    
    # Unauthenticated -> 401
    res_unauth = requests.post(f"{BASE_URL}/api/ai/summarize", json={"content": "test"}, timeout=5)
    if res_unauth.status_code == 401:
        log_success("Unauthenticated request correctly returned 401 Unauthorized.")
        results.append(("Security: 401 Unauthenticated", "PASS", f"{res_unauth.elapsed.total_seconds()*1000:.1f} ms"))
    else:
        log_error(f"Unauthenticated request returned {res_unauth.status_code} (expected 401).")
        results.append(("Security: 401 Unauthenticated", "FAIL", f"{res_unauth.status_code}"))

    # TEACHER -> 403
    res_teacher = requests.post(f"{BASE_URL}/api/ai/summarize", json={"content": "test content"}, headers=headers_teacher, timeout=5)
    if res_teacher.status_code == 403:
        log_success("TEACHER role request correctly returned 403 Forbidden.")
        results.append(("Security: 403 Non-STUDENT Access", "PASS", f"{res_teacher.elapsed.total_seconds()*1000:.1f} ms"))
    else:
        log_error(f"TEACHER role request returned {res_teacher.status_code} (expected 403).")
        results.append(("Security: 403 Non-STUDENT Access", "FAIL", f"{res_teacher.status_code}"))

    # 3. Input Validation Test: Blank input (400)
    res_invalid = requests.post(f"{BASE_URL}/api/ai/summarize", json={"content": "   "}, headers=headers_student, timeout=5)
    if res_invalid.status_code == 400:
        log_success("Blank content request correctly returned 400 Bad Request.")
        results.append(("Validation: 400 Blank Content", "PASS", f"{res_invalid.elapsed.total_seconds()*1000:.1f} ms"))
    else:
        log_error(f"Blank content request returned {res_invalid.status_code} (expected 400).")
        results.append(("Validation: 400 Blank Content", "FAIL", f"{res_invalid.status_code}"))

    # 4. Feature 1: POST /api/ai/summarize
    log_info("Executing live POST /api/ai/summarize against Ollama/Qwen...")
    summarize_payload = {
        "content": (
            "Object-Oriented Programming (OOP) is a fundamental programming paradigm structured around objects "
            "rather than actions, and data rather than logic. The three main pillars of OOP are Encapsulation, "
            "Inheritance, and Polymorphism. Encapsulation seals data and methods into a single unit. Inheritance allows "
            "subclasses to inherit properties from parent classes. Polymorphism enables dynamic method binding."
        )
    }
    
    t0 = time.time()
    try:
        res_sum = requests.post(f"{BASE_URL}/api/ai/summarize", json=summarize_payload, headers=headers_student, timeout=180)
        latency_sum = (time.time() - t0) * 1000

        if res_sum.status_code == 200:
            body = res_sum.json()
            data = body.get("data", {})
            if data.get("summary") and isinstance(data.get("keyTakeaways"), list) and isinstance(data.get("keywords"), list):
                log_success(f"Summarize succeeded in {latency_sum:.1f} ms! Summary length: {len(data['summary'])} chars, Takeaways: {len(data['keyTakeaways'])}, Keywords: {len(data['keywords'])}")
                results.append(("POST /api/ai/summarize", "PASS", f"{latency_sum:.1f} ms"))
            else:
                log_error(f"Summarize output missing required fields: {body}")
                results.append(("POST /api/ai/summarize", "FAIL", "Invalid DTO Structure"))
        else:
            log_error(f"Summarize request failed with status {res_sum.status_code}: {res_sum.text}")
            results.append(("POST /api/ai/summarize", "FAIL", f"HTTP {res_sum.status_code}"))
    except Exception as e:
        log_error(f"Summarize request exception: {e}")
        results.append(("POST /api/ai/summarize", "FAIL", f"Exception: {e}"))

    # 5. Feature 2: POST /api/ai/explain-code
    log_info("Executing live POST /api/ai/explain-code against Ollama/Qwen...")
    code_payload = {
        "code": "public class Main { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
        "language": "Java"
    }

    t0 = time.time()
    try:
        res_code = requests.post(f"{BASE_URL}/api/ai/explain-code", json=code_payload, headers=headers_student, timeout=180)
        latency_code = (time.time() - t0) * 1000

        if res_code.status_code == 200:
            body = res_code.json()
            data = body.get("data", {})
            if data.get("purpose") and data.get("timeComplexity") and data.get("spaceComplexity") and isinstance(data.get("stepByStepLogic"), list):
                log_success(f"Code Explanation succeeded in {latency_code:.1f} ms! Purpose: '{data['purpose'][:60]}...', Time Complexity: {data['timeComplexity']}")
                results.append(("POST /api/ai/explain-code", "PASS", f"{latency_code:.1f} ms"))
            else:
                log_error(f"Code Explanation output missing required fields: {body}")
                results.append(("POST /api/ai/explain-code", "FAIL", "Invalid DTO Structure"))
        else:
            log_error(f"Code Explanation request failed with status {res_code.status_code}: {res_code.text}")
            results.append(("POST /api/ai/explain-code", "FAIL", f"HTTP {res_code.status_code}"))
    except Exception as e:
        log_error(f"Code Explanation request exception: {e}")
        results.append(("POST /api/ai/explain-code", "FAIL", f"Exception: {e}"))

    # 6. Feature 3: POST /api/ai/study-plan
    log_info("Executing live POST /api/ai/study-plan against Ollama/Qwen...")
    future_exam_date = (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d")
    available_hours = 4.0
    plan_payload = {
        "examDate": future_exam_date,
        "subjects": ["Java", "PostgreSQL"],
        "availableHoursPerDay": available_hours
    }

    t0 = time.time()
    try:
        res_plan = requests.post(f"{BASE_URL}/api/ai/study-plan", json=plan_payload, headers=headers_student, timeout=180)
        latency_plan = (time.time() - t0) * 1000

        if res_plan.status_code == 200:
            body = res_plan.json()
            data = body.get("data", {})
            exam_date_res = data.get("examDate")
            days_rem = data.get("daysRemaining")
            schedule = data.get("plan", [])

            hours_exceeded = False
            for day in schedule:
                day_total = sum(task.get("hours", 0) for task in day.get("tasks", []))
                if day_total > available_hours + 0.01:
                    hours_exceeded = True
                    log_error(f"Business rule violation on date {day.get('date')}: Allocated {day_total}h exceeds limit {available_hours}h!")

            if exam_date_res == future_exam_date and days_rem == 7 and len(schedule) > 0 and not hours_exceeded:
                log_success(f"Study Plan succeeded in {latency_plan:.1f} ms! Days remaining: {days_rem}, Daily schedules: {len(schedule)}, Hours limit validated.")
                results.append(("POST /api/ai/study-plan", "PASS", f"{latency_plan:.1f} ms"))
            else:
                log_error(f"Study Plan validation failed. Exam date match: {exam_date_res == future_exam_date}, Hours exceeded: {hours_exceeded}")
                results.append(("POST /api/ai/study-plan", "FAIL", "Validation or Business Rule Failure"))
        else:
            log_error(f"Study Plan request failed with status {res_plan.status_code}: {res_plan.text}")
            results.append(("POST /api/ai/study-plan", "FAIL", f"HTTP {res_plan.status_code}"))
    except Exception as e:
        log_error(f"Study Plan request exception: {e}")
        results.append(("POST /api/ai/study-plan", "FAIL", f"Exception: {e}"))

    # 7. Feature 4: POST /api/ai/flashcards
    log_info("Executing live POST /api/ai/flashcards against Ollama/Qwen...")
    flashcards_payload = {
        "content": (
            "PostgreSQL is an open-source database system. "
            "pgvector enables vector similarity search using HNSW and IVFFlat indexes."
        ),
        "cardCount": 2
    }

    t0 = time.time()
    try:
        res_flash = requests.post(f"{BASE_URL}/api/ai/flashcards", json=flashcards_payload, headers=headers_student, timeout=180)
        latency_flash = (time.time() - t0) * 1000

        if res_flash.status_code == 200:
            body = res_flash.json()
            data = body.get("data", {})
            cards = data.get("flashcards", [])
            
            questions = [c.get("question", "").strip().lower() for c in cards]
            unique_questions = set(questions)
            is_deduped = len(questions) == len(unique_questions)

            if len(cards) > 0 and is_deduped:
                log_success(f"Flashcards succeeded in {latency_flash:.1f} ms! Generated {len(cards)} flashcards, Deduplication verified.")
                results.append(("POST /api/ai/flashcards", "PASS", f"{latency_flash:.1f} ms"))
            else:
                log_error(f"Flashcards validation failed: {cards}")
                results.append(("POST /api/ai/flashcards", "FAIL", "Deduplication or Card Count Failure"))
        else:
            log_error(f"Flashcards request failed with status {res_flash.status_code}: {res_flash.text}")
            results.append(("POST /api/ai/flashcards", "FAIL", "HTTP " + str(res_flash.status_code)))
    except Exception as e:
        log_error(f"Flashcards request exception: {e}")
        results.append(("POST /api/ai/flashcards", "FAIL", f"Exception: {e}"))

    # Summary Table
    print("\n" + "=" * 70)
    print("  LIVE E2E VERIFICATION RESULTS SUMMARY")
    print("=" * 70)
    print(f"{'Endpoint / Test Case':<35} | {'Result':<10} | {'Latency / Details':<18}")
    print("-" * 70)
    for test_name, status, details in results:
        color = GREEN if status == "PASS" else RED
        print(f"{test_name:<35} | {color}{status:<10}{RESET} | {details:<18}")
    print("=" * 70)

    # Return True if all tests passed
    all_passed = all(status == "PASS" for _, status, _ in results)
    return all_passed

if __name__ == "__main__":
    success = verify_week11_live()
    sys.exit(0 if success else 1)
