
import requests

BASE_URL = "http://localhost:8080"

def test_login_and_feed():
    session = requests.Session()

    # 1. Try to login
    login_data = {
        "email": "test@example.com",
        "password": "password"
    }

    print("Attempting login...")
    response = session.post(f"{BASE_URL}/api/auth/login", data=login_data)

    print(f"Login status: {response.status_code}")
    print(f"Login response: {response.text}")

    if response.status_code == 200:
        # 2. Try to access feed
        print("Attempting to access feed...")
        response = session.get(f"{BASE_URL}/api/posts/feed")
        print(f"Feed status: {response.status_code}")
        print(f"Feed response: {response.text}")

        if response.status_code == 403 or response.status_code == 302:
             print("FAILURE: Session lost or access denied.")
        else:
             print("SUCCESS: Session maintained.")
    else:
        print("Login failed, cannot test feed.")

if __name__ == "__main__":
    test_login_and_feed()
