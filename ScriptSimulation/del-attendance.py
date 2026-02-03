import sys
import firebase_admin
from firebase_admin import credentials, firestore

cred = credentials.Certificate('serviceAccountKey.json')
if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

db = firestore.client()

def delete_attendance(child_id):
    attendance_ref = db.collection('childrens').document(child_id).collection('attendance')
    docs = attendance_ref.stream()
    deleted_count = 0
    try:
        for doc in docs:
            doc.reference.delete()
            deleted_count += 1
        if deleted_count > 0:
            print(f"Success: Deleted {deleted_count} records for child {child_id}.")
        else:
            print(f"No records found for child {child_id}.")
    except Exception as e:
        print(f"Error during deletion: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Error: Missing child ID. Usage: python file.py <child_id>")
    else:
        target_id = sys.argv[1].strip()
        confirm = input(f"Are you sure you want to delete ALL attendance records for {target_id}? (y/n): ")
        if confirm.lower() == 'y':
            delete_attendance(target_id)
        else:
            print("Operation cancelled.")