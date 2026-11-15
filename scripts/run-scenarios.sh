#!/usr/bin/env bash
# Smoke test / demo driver: runs the main workflow scenarios through the REST API and prints the outcome of each.
# Usage: scripts/run-scenarios.sh [backend-url]      (default http://localhost:8081)
# Needs: curl, python3. Uses the seeded demo data (fresh database recommended).
set -euo pipefail

API="${1:-http://localhost:8081}/api"
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
post() { curl -fsS -X POST "$API$1" -H 'Content-Type: application/json' -d "$2"; }
member() { # regNo -> {"regNo","name","email"} from the student directory
  curl -fsS "$API/students/$1" | python3 -c "import json,sys; s=json.load(sys.stdin); print(json.dumps({'regNo':s['regNo'],'name':s['name'],'email':s['email']}))"
}
status() { curl -fsS "$API/registrations/$1" | json "d['status'] + ' | ' + (d['statusReason'] or '')"; }
wait_for() { # registration id, expected status, timeout seconds
  for _ in $(seq 1 "$3"); do
    s=$(curl -fsS "$API/registrations/$1" | json "d['status']")
    [ "$s" = "$2" ] && return 0
    sleep 1
  done
  echo "  !! $1 is $s, expected $2"; return 1
}
task_for() { # process instance of registration -> open user task key
  pik=$(curl -fsS "$API/registrations/$1" | json "d['processInstanceKey']")
  for _ in $(seq 1 20); do
    k=$(curl -fsS "$API/registrations/$1/tracker" | json "d['openTasks'][0]['userTaskKey'] if d['openTasks'] else ''")
    [ -n "$k" ] && { echo "$k"; return; }
    sleep 1
  done
}

echo "Engine: $(curl -fsS "$API/dashboard" | json "d['engine']")"
curl -fsS -X PUT "$API/admin/settings" -H 'Content-Type: application/json' -d '{"slaPreset":"DEMO","studentRecordsOutage":false,"forceMemberConflict":false}' >/dev/null

echo; echo "1. Happy path: valid team, FA approves"
R1=$(post /registrations "{\"eventId\":1,\"mode\":\"TEAM\",\"teamName\":\"Byte Busters\",\"facultyAdvisorId\":\"FA-CSE-01\",\"members\":[$(member RA2411003010001),$(member RA2411003010002),$(member RA2411003010003)]}" | json "d['id']")
wait_for "$R1" PENDING_FA_REVIEW 30
K=$(task_for "$R1")
post "/tasks/$K/completion" '{"actorId":"FA-CSE-01","decision":"APPROVED","remarks":"Well balanced team."}' >/dev/null
wait_for "$R1" APPROVED 30 && echo "  $R1 -> $(status "$R1")"

echo; echo "2. Rule violation: two members, FA advises neither"
R2=$(post /registrations "{\"eventId\":1,\"mode\":\"TEAM\",\"teamName\":\"Duo\",\"facultyAdvisorId\":\"FA-ECE-01\",\"members\":[$(member RA2411003010005),$(member RA2411003010006)]}" | json "d['id']")
wait_for "$R2" INVALID 30 && echo "  $R2 -> $(status "$R2")"

echo; echo "3. Ineligible (suspended) member"
R3=$(post /registrations "{\"eventId\":1,\"mode\":\"TEAM\",\"teamName\":\"Null Pointers\",\"facultyAdvisorId\":\"FA-CSE-02\",\"members\":[$(member RA2411003010007),$(member RA2411003010008),$(member RA2411003010023)]}" | json "d['id']")
wait_for "$R3" INVALID 30 && echo "  $R3 -> $(status "$R3")"

echo; echo "4. Unknown student (REST connector 404)"
R4=$(post /registrations "{\"eventId\":1,\"mode\":\"TEAM\",\"teamName\":\"Phantoms\",\"facultyAdvisorId\":\"FA-CSE-01\",\"members\":[$(member RA2411003010004),$(member RA2411003010021),{\"regNo\":\"RA2411003099999\",\"name\":\"Unknown Person\",\"email\":\"unknown@campus.example.edu\"}]}" | json "d['id']")
wait_for "$R4" INVALID 30 && echo "  $R4 -> $(status "$R4")"

echo; echo "5. FA rejects with remarks"
R5=$(post /registrations "{\"eventId\":2,\"mode\":\"TEAM\",\"teamName\":\"Circuit Breakers\",\"facultyAdvisorId\":\"FA-IT-01\",\"members\":[$(member RA2411003010009),$(member RA2411003010010)]}" | json "d['id']")
wait_for "$R5" PENDING_FA_REVIEW 30
K=$(task_for "$R5")
post "/tasks/$K/completion" '{"actorId":"FA-IT-01","decision":"REJECTED","remarks":"Please add a member with hardware experience."}' >/dev/null
wait_for "$R5" REJECTED 30 && echo "  $R5 -> $(status "$R5")"

echo; echo "6. Random allocation: 5 individuals for Tech Quiz Bowl (teams of 2-3), then close registration"
for n in 16 17 18 19 20; do post /registrations "{\"eventId\":3,\"mode\":\"INDIVIDUAL\",\"members\":[$(member RA24110030100$n)]}" >/dev/null; done
sleep 8
post "/events/3/close?actor=coordinator" '' >/dev/null
sleep 12
curl -fsS "$API/events/3/allocation" | python3 -c "
import json,sys; a=json.load(sys.stdin)
print('  rounds:', [(r['round'], r['studentsInPool'], r['teamsCreated']) for r in a['runs']])
for r in a['randomTeams']: print('  ', r['teamName'], r['status'], r['facultyAdvisorId'], [m['regNo'][-2:] for m in r['members']])"

echo; echo "Done. Follow the instances in the portal, Camunda Operate and the Mailpit inbox."
