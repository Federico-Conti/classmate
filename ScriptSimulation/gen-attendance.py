import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, timedelta
import random

cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred)
db = firestore.client()

def populate_attendance(child_id):
    attendance_ref = db.collection('childrens').document(child_id).collection('attendance')
    start_date = datetime(2025, 1, 1)
    print(f"Starting population for child: {child_id}...")
    records_to_add = []
    current_date = start_date
    count = 0
    while count < 56:
        if current_date.weekday() < 5:
            entry_hour = random.randint(7, 9)
            entry_minute = random.randint(0, 59)
            entry_time = current_date.replace(hour=entry_hour, minute=entry_minute)
            exit_hour = random.randint(15, 18)
            exit_minute = random.randint(0, 59)
            exit_time = current_date.replace(hour=exit_hour, minute=exit_minute)
            records_to_add.append({
                'type': 'entry',
                'time': entry_time
            })
            records_to_add.append({
                'type': 'exit',
                'time': exit_time
            })
            count += 1
        current_date += timedelta(days=1)
    records_to_add.sort(key=lambda x: x['time'])
    for record in records_to_add:
        attendance_ref.add(record)
    print(f"Population completed successfully! Added {count} enter/exit pairs ({count * 2} total records).")

if __name__ == "__main__":
    child_id = input("Enter the child ID to generate attendance for: ").strip()
    if child_id:
        populate_attendance(child_id)
    else:
        print("Invalid ID. Operation cancelled.")