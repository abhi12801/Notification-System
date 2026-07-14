#!/usr/bin/env bash
# Step-by-step curl walkthrough of the full pipeline: create -> Kafka produce ->
# consume -> random outcome -> retry -> dashboard. Every command below was run
# live against a real Postgres+Kafka stack while writing this script.
#
# Import into Postman: File > Import > paste any curl command below, or use
# postman/Smart-Notification-System.postman_collection.json for the full
# collection with pre-filled examples.

BASE=http://localhost:8080

echo "=== 0. Health check ==="
curl -s "$BASE/actuator/health"
echo -e "\n"

echo "=== 1. Create a notification (201 expected, status=PENDING) ==="
curl -s --location "$BASE/api/notifications" \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": 301,
    "type": "EMAIL",
    "message": "Verify full pipeline end to end",
    "scheduleTime": "2026-07-15T10:00:00"
  }'
echo -e "\n(note the \"id\" in the response above — use it in place of {id} below)\n"

echo "=== 2. Wait ~2 seconds for the Kafka consumer to process it, then fetch by id ==="
sleep 2
curl -s "$BASE/api/notifications/{id}"
echo -e "\n(status should now be SENT or FAILED, never still PENDING)\n"

echo "=== 3. Duplicate detection: repeat the exact same create within 5 minutes (expect 409) ==="
curl -s -w "\nSTATUS:%{http_code}\n" --location "$BASE/api/notifications" \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": 301,
    "type": "EMAIL",
    "message": "Verify full pipeline end to end"
  }'
echo -e "\n"

echo "=== 4. Repeated-word validation: word repeated 4x consecutively (expect 400) ==="
curl -s -w "\nSTATUS:%{http_code}\n" --location "$BASE/api/notifications" \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": 302,
    "type": "SMS",
    "message": "hello hello hello hello"
  }'
echo -e "\n"

echo "=== 5. 404 on unknown id ==="
curl -s -w "\nSTATUS:%{http_code}\n" "$BASE/api/notifications/999999"
echo -e "\n"

echo "=== 6. If step 2 showed FAILED: retry immediately (expect 409, cooldown not elapsed) ==="
curl -s -w "\nSTATUS:%{http_code}\n" -X POST "$BASE/api/notifications/{id}/retry"
echo -e "\n"

echo "=== 7. Wait 2+ minutes, then retry again (expect 200, status becomes RETRYING) ==="
echo "(run this manually after waiting -- see README for why the wait is real, not simulated)"
echo "curl -s -X POST \"$BASE/api/notifications/{id}/retry\""
echo -e "\n"

echo "=== 8. A few seconds after a successful retry, fetch by id again ==="
echo "(status should have moved from RETRYING to SENT or FAILED again)"
echo "curl -s \"$BASE/api/notifications/{id}\""
echo -e "\n"

echo "=== 9. List, filtered + paginated + sorted by createdAt (default sort) ==="
curl -s "$BASE/api/notifications?page=0&size=10&status=FAILED&type=EMAIL"
echo -e "\n"

echo "=== 10. List, unfiltered ==="
curl -s "$BASE/api/notifications?page=0&size=20"
echo -e "\n"

echo "=== 11. Dashboard statistics ==="
curl -s "$BASE/api/dashboard"
echo -e "\n"
