import subprocess
import time
import requests
import json
import sys
import os

BASE_URL = "http://localhost:8080"
PDF_PATH = "Java_Programming_Fundamentals.pdf"
JAVA_HOME = "/Users/krishnagarg/homebrew/Cellar/openjdk/21.0.2/libexec/openjdk.jdk/Contents/Home"
JAR_PATH = "target/learning-assistant-1.0.0-SNAPSHOT.jar"

def log(msg):
    print(f"[VERIFY] {msg}", flush=True)

def start_server():
    log("Starting Spring Boot application process...")
    env = os.environ.copy()
    env["JAVA_HOME"] = JAVA_HOME
    env["PATH"] = f"{JAVA_HOME}/bin:" + env.get("PATH", "")
    
    log_file = open("server_rag.log", "w")
    proc = subprocess.Popen(
        [f"{JAVA_HOME}/bin/java", "-jar", JAR_PATH],
        stdout=log_file,
        stderr=subprocess.STDOUT,
        env=env,
        text=True
    )
    
    # Wait for server ready
    start_time = time.time()
    while time.time() - start_time < 60:
        try:
            res = requests.get(f"{BASE_URL}/v3/api-docs", timeout=2)
            if res.status_code == 200:
                log("Spring Boot application is UP and healthy!")
                return proc
        except Exception:
            pass
        time.sleep(2)
    
    log("Server failed to start within 60 seconds!")
    proc.kill()
    sys.exit(1)

def main():
    if not os.path.exists(PDF_PATH):
        log(f"PDF file {PDF_PATH} not found!")
        sys.exit(1)
        
    proc = start_server()
    
    try:
        # Step 1: Register/Login Student
        student_email = f"student_rag_{int(time.time())}@learnpulse.ai"
        student_password = "Password123!"
        
        log(f"Registering student: {student_email}")
        reg_payload = {
            "firstName": "RAG",
            "lastName": "Student",
            "email": student_email,
            "password": student_password,
            "role": "STUDENT"
        }
        res = requests.post(f"{BASE_URL}/api/auth/register", json=reg_payload)
        assert res.status_code in (200, 201), f"Student registration failed: {res.text}"
        
        log("Logging in student...")
        login_res = requests.post(f"{BASE_URL}/api/auth/login", json={"email": student_email, "password": student_password})
        assert login_res.status_code == 200, f"Student login failed: {login_res.text}"
        student_data = login_res.json()["data"]
        student_token = student_data["accessToken"]
        student_headers = {"Authorization": f"Bearer {student_token}"}
        
        # Step 2: Register/Login Teacher
        teacher_email = f"teacher_rag_{int(time.time())}@learnpulse.ai"
        log(f"Registering teacher: {teacher_email}")
        requests.post(f"{BASE_URL}/api/auth/register", json={
            "firstName": "RAG",
            "lastName": "Teacher",
            "email": teacher_email,
            "password": student_password,
            "role": "TEACHER"
        })
        t_login = requests.post(f"{BASE_URL}/api/auth/login", json={"email": teacher_email, "password": student_password})
        teacher_token = t_login.json()["data"]["accessToken"]
        teacher_headers = {"Authorization": f"Bearer {teacher_token}"}

        # Step 3: Upload document via /api/teacher/upload-pdf
        log("Uploading educational PDF document...")
        with open(PDF_PATH, "rb") as f:
            files = {"file": (PDF_PATH, f, "application/pdf")}
            upload_res = requests.post(
                f"{BASE_URL}/api/teacher/upload-pdf",
                headers=teacher_headers,
                files=files
            )
                
        assert upload_res.status_code in (200, 201), f"Upload failed: {upload_res.text}"
        doc_json = upload_res.json()
        doc_data = doc_json.get("data", doc_json)
        document_id = doc_data.get("id") or doc_data.get("documentId")
        log(f"Uploaded Document ID (UUID): {document_id}")
        
        # Verify text extraction
        log("Verifying text extraction...")
        doc_info_res = requests.get(f"{BASE_URL}/api/documents/{document_id}", headers=teacher_headers)
        if doc_info_res.status_code == 200:
            info = doc_info_res.json()["data"]
            extracted = info.get("extractedText", "")
            log(f"Extracted Text Length: {len(extracted)} characters")
            assert len(extracted) > 100, "Text extraction returned insufficient text!"

        # Step 4: Perform Real RAG Positive Questions
        positive_queries = [
            "What is inheritance?",
            "What is polymorphism?",
            "Explain interfaces.",
            "What is exception handling?"
        ]
        
        perf_data = []
        log("\n==========================================")
        log("RUNNING POSITIVE RAG QUERIES")
        log("==========================================")
        
        for q in positive_queries:
            t0 = time.time()
            ask_payload = {
                "documentId": document_id,
                "question": q
            }
            log(f"\n[QUERY]: '{q}'")
            try:
                ask_res = requests.post(f"{BASE_URL}/api/ai/ask-document", headers=student_headers, json=ask_payload, timeout=300)
                t1 = time.time()
                elapsed_ms = round((t1 - t0) * 1000, 2)
                log(f"Response Time: {elapsed_ms} ms")
                log(f"HTTP Status: {ask_res.status_code}")
                if ask_res.status_code != 200:
                    log(f"Response Text: {ask_res.text}")
                assert ask_res.status_code == 200, f"RAG Query failed ({ask_res.status_code}): {ask_res.text}"
                res_body = ask_res.json()["data"]
                
                answer = res_body.get("answer", "")
                sources = res_body.get("sources", [])
                fallback = res_body.get("fallback", False)
                
                log(f"Fallback Triggered: {fallback}")
                log(f"Sources Count: {len(sources)}")
                for idx, src in enumerate(sources):
                    log(f"  Source [{idx+1}]: Chunk {src.get('chunkIndex')}, Score: {src.get('similarityScore')}, Content preview: {str(src.get('contentPreview'))[:80]}...")
                log(f"Grounded Answer:\n{answer}\n")
                
                assert not fallback, f"Fallback should NOT be triggered for positive query: '{q}'"
                assert len(sources) > 0, f"No sources returned for query: '{q}'"
                assert len(answer) > 20, f"Answer too short: '{answer}'"
                
                perf_data.append({
                    "question": q,
                    "latency_ms": elapsed_ms,
                    "sources_count": len(sources),
                    "fallback": fallback
                })
            except Exception as e:
                log(f"Exception/Assertion Failure during query '{q}': {e}")
                import traceback
                traceback.print_exc()
                raise e

        # Step 5: Perform Real RAG Negative Query (No-Context)
        log("\n==========================================")
        log("RUNNING NEGATIVE RAG QUERY (UNRELATED QUESTION)")
        log("==========================================")
        neg_query = "What is the capital of France?"
        log(f"[QUERY]: '{neg_query}'")
        t0 = time.time()
        neg_res = requests.post(f"{BASE_URL}/api/ai/ask-document", headers=student_headers, json={"documentId": document_id, "question": neg_query}, timeout=300)
        t1 = time.time()
        neg_elapsed = round((t1 - t0) * 1000, 2)
        
        assert neg_res.status_code == 200, f"Negative RAG Query failed ({neg_res.status_code}): {neg_res.text}"
        neg_body = neg_res.json()["data"]
        
        neg_answer = neg_body["answer"]
        neg_fallback = neg_body.get("fallback", False)
        neg_sources = neg_body.get("sources", [])
        
        log(f"Response Time: {neg_elapsed} ms")
        log(f"Fallback Field: {neg_fallback}")
        log(f"Sources Count: {len(neg_sources)}")
        log(f"Answer: {neg_answer}")
        
        assert len(neg_sources) == 0, f"Expected 0 sources for unrelated query, got {len(neg_sources)}"
        assert "could not be found" in neg_answer, f"Answer does not match no-context wording: {neg_answer}"

        # Step 6: Verify Authorization Security
        log("\n==========================================")
        log("VERIFYING SECURITY & ACCESS CONTROL")
        log("==========================================")
        # Unauthenticated
        unauth_res = requests.post(f"{BASE_URL}/api/ai/ask-document", json={"documentId": document_id, "question": "What is inheritance?"})
        log(f"Unauthenticated request status code: {unauth_res.status_code} (Expected 401/403)")
        assert unauth_res.status_code in (401, 403), "Security failure: Unauthenticated access permitted!"
        
        # Invalid document UUID
        fake_uuid = "00000000-0000-0000-0000-000000000000"
        invalid_doc_res = requests.post(f"{BASE_URL}/api/ai/ask-document", headers=student_headers, json={"documentId": fake_uuid, "question": "What is inheritance?"})
        log(f"Invalid document UUID status code: {invalid_doc_res.status_code} (Expected 404)")
        assert invalid_doc_res.status_code == 404, f"Expected 404 for missing document, got {invalid_doc_res.status_code}"

        log("\n==========================================")
        log("E2E RAG VERIFICATION COMPLETED SUCCESSFULLY!")
        log("==========================================")

    finally:
        log("Stopping Spring Boot application process...")
        proc.terminate()
        proc.wait()

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"[FATAL ERROR]: {e}", flush=True)
        import traceback
        traceback.print_exc()
        sys.exit(1)
