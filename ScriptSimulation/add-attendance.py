import sys
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime

# 1. Firebase Initialization
cred = credentials.Certificate('serviceAccountKey.json')
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

db = firestore.client()

def record_attendance():
    if len(sys.argv) < 3:
        print("Error: Missing arguments. Usage: python file.py <child_id> en/ex")
        return

    child_id = sys.argv[1].strip()
    choice = sys.argv[2].lower().strip()

    if choice == 'en':
        event_type = "entry"
    elif choice == 'ex':
        event_type = "exit"
    else:
        print(f"Error: '{choice}' is not a valid command. Use only 'en' or 'ex'.")
        return

    now = datetime.now()

    try:
        db.collection('childrens').document(child_id).collection('attendance').add({
            'type': event_type,
            'time': now
        })
        print(f"Recorded: {event_type} at {now.strftime('%H:%M:%S')} on {now.strftime('%d/%m/%Y')}")
    except Exception as e:
        print(f"Error while saving: {e}")

if __name__ == "__main__":
    record_attendance()