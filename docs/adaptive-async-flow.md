# Adaptive Async Flow

Acest document descrie flow-ul recomandat pentru adaptive learning: `job creation -> polling -> session submit`.

## Overview

Flow-ul async evită request-urile lungi și timeout-urile de la AI.

Pași:

1. studentul creează un job de generare adaptive
2. frontend-ul face polling după status
3. când job-ul ajunge `DONE`, backend-ul returnează sesiunea completă
4. studentul trimite răspunsurile pentru sesiune

Endpointul vechi sincron `/api/v1/adaptive/start` nu este flow-ul recomandat.

## Auth

Toate endpointurile adaptive cer utilizator autentificat cu rol `STUDENT`.

Header folosit:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Pentru endpointurile adaptive nu este necesar CSRF.

## 1. Create Adaptive Job

`POST /api/v1/adaptive/jobs`

### Request

```json
{
  "subjectId": 1,
  "topicId": 2,
  "count": 4
}
```

### Response

`202 Accepted`

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING"
}
```

### Notes

- `jobId` este ID-ul local din backend
- `status` inițial este de obicei `PENDING`

## 2. Poll Job Status

`GET /api/v1/adaptive/jobs/{jobId}`

Frontend-ul face polling până când `status` devine `DONE` sau `FAILED`.

### Running response

`200 OK`

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "error": null,
  "session": null
}
```

### Done response

`200 OK`

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DONE",
  "error": null,
  "session": {
    "sessionId": "9a1d6a26-a837-4d4f-bfef-9d2f2ebf7c3b",
    "expiresAt": "2026-05-20T10:45:00",
    "exercises": [
      {
        "exerciseId": "ex-1",
        "text": "What is the capital of France?",
        "type": "SINGLE_CHOICE",
        "answers": ["Paris", "Lyon", "Marseille", "Bordeaux"]
      }
    ]
  }
}
```

### Failed response

`200 OK`

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "error": "Adaptive AI returned an invalid response.",
  "session": null
}
```

### Notes

- când `status = DONE`, frontend-ul trebuie să ia `session.sessionId`
- `exercises[*]` nu expun `correctAnswers`
- dacă `status = FAILED`, UI-ul trebuie să oprească polling-ul și să afișeze `error`

## 3. Submit Adaptive Session

`POST /api/v1/adaptive/sessions/{sessionId}/submit`

### Request

```json
{
  "answers": [
    {
      "exerciseId": "ex-1",
      "givenAnswers": ["Paris"],
      "timeSpent": 18
    }
  ]
}
```

### Response

`200 OK`

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "totalScore": 1.0,
  "clientResults": [
    {
      "mlExerciseId": "ex-1",
      "correct": true,
      "score": 1.0,
      "correctAnswers": ["Paris"],
      "givenAnswers": ["Paris"]
    }
  ],
  "feedbackSent": true
}
```

### Notes

- `exerciseId` din submit trebuie să fie cel primit în `session.exercises[*].exerciseId`
- exercițiile lipsă sunt tratate ca unanswered
- `feedbackSent` arată dacă sincronizarea feedback-ului AI a reușit

## Status Semantics

### Job status

- `PENDING` - job creat, încă neprocesat complet
- `RUNNING` - AI încă generează exercițiile
- `DONE` - sesiunea a fost materializată și poate fi folosită
- `FAILED` - jobul a eșuat; vezi câmpul `error`

### Session constraints

La submit, sesiunea trebuie să fie:

- a studentului autentificat
- `ACTIVE`
- neexpirată

Altfel backend-ul poate răspunde cu:

- `404 Not Found` - sesiunea nu există sau nu aparține studentului
- `409 Conflict` - sesiunea nu mai este activă sau a expirat

## Suggested Frontend Flow

1. apel `POST /api/v1/adaptive/jobs`
2. salvează `jobId`
3. pornește polling la `GET /api/v1/adaptive/jobs/{jobId}`
4. dacă statusul este `PENDING` sau `RUNNING`, continuă polling-ul
5. dacă statusul este `DONE`, oprește polling-ul și afișează `session.exercises`
6. la final, trimite `POST /api/v1/adaptive/sessions/{sessionId}/submit`
7. afișează `totalScore`, `clientResults`, `feedbackSent`

## Postman Smoke Test

### Create job

```http
POST /api/v1/adaptive/jobs
Authorization: Bearer <studentToken>
Content-Type: application/json
```

```json
{
  "subjectId": 1,
  "topicId": 2,
  "count": 1
}
```

### Poll

```http
GET /api/v1/adaptive/jobs/{jobId}
Authorization: Bearer <studentToken>
```

### Submit

```http
POST /api/v1/adaptive/sessions/{sessionId}/submit
Authorization: Bearer <studentToken>
Content-Type: application/json
```

```json
{
  "answers": [
    {
      "exerciseId": "ex-1",
      "givenAnswers": ["A"],
      "timeSpent": 10
    }
  ]
}
```

## Relevant Endpoints

- `POST /api/v1/adaptive/jobs`
- `GET /api/v1/adaptive/jobs/{jobId}`
- `POST /api/v1/adaptive/sessions/{sessionId}/submit`
