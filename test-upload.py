#!/usr/bin/env python3
"""
Simple script to test server upload and see validation logging
"""
import requests
import json

# Read the XML file
with open('test_reminder.xml', 'r', encoding='utf-8') as f:
    xml_content = f.read()

# Prepare the upload request
upload_data = {
    "filename": "test_reminder.xml",
    "content": xml_content
}

print("=" * 60)
print("TESTING SERVER UPLOAD")
print("=" * 60)
print(f"Server URL: http://localhost:8080/s-emulator/api/upload")
print(f"File: test_reminder.xml")
print(f"Content length: {len(xml_content)} characters")
print()
print("Uploading...")
print()

try:
    # Upload to server
    response = requests.post(
        'http://localhost:8080/s-emulator/api/upload',
        json=upload_data,
        headers={'Content-Type': 'application/json'}
    )
    
    print("=" * 60)
    print("RESPONSE FROM SERVER")
    print("=" * 60)
    print(f"Status Code: {response.status_code}")
    print()
    print("Response Body:")
    print(json.dumps(response.json(), indent=2))
    print()
    print("=" * 60)
    print("CHECK THE SERVER CONSOLE FOR VALIDATION LOGGING!")
    print("=" * 60)
    
except requests.exceptions.ConnectionError:
    print("❌ ERROR: Could not connect to server!")
    print("   Make sure the server is running:")
    print("   cd S-emulator/s-server")
    print("   ./run-server.bat")
    
except Exception as e:
    print(f"❌ ERROR: {e}")

