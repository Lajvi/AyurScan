# Technical Documentation: `main.py`

> **Project:** Ayurvedic Recommendation System
> **Module:** `main.py`
> **Framework:** FastAPI 
> **Version:** 1.0.0
> **Language:** Python 3.8+

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Mathematics Behind It](#2-mathematics-behind-it)
3. [How It Works](#3-how-it-works)
4. [Dependencies](#4-dependencies)
5. [Input Arguments](#5-input-arguments)
6. [Output Format](#6-output-format)
7. [Example](#7-example)
8. [Pros / Advantages](#8-pros--advantages)
9. [Edge Cases & Limitations](#9-edge-cases--limitations)
10. [Future Improvements](#10-future-improvements)

---

## 1. Introduction

`main.py` is the **HTTP API Gateway** of the Ayurvedic Recommendation System. It exposes the food and medicine recommendation engines — `AyurRecommender` and `AyurMedicineRecommender` — as a RESTful JSON API built on the **FastAPI** framework, making the system accessible to any HTTP client such as a React frontend, a mobile app, or third-party services.

The module is responsible for:

- **Request validation** — Enforcing type constraints and value ranges on all query parameters via FastAPI's built-in Pydantic integration and `Query()` field descriptors.
- **Routing** — Mapping HTTP GET requests to the appropriate recommendation or data retrieval logic.
- **Response shaping** — Packaging raw recommendation outputs into structured, schema-validated Pydantic response models.
- **Error handling** — Translating internal exceptions into appropriate HTTP status codes and developer-friendly error messages.
- **CORS management** — Permitting cross-origin requests from the React frontend running on `localhost:3000`.

Within the broader project architecture, `main.py` sits at the **topmost service layer**, acting as the single entry point between external consumers and the recommendation and data processing layers beneath it.

```
React Frontend (localhost:3000)
         │  HTTP GET requests
         ▼
┌─────────────────────────────┐
│         main.py             │  ← FastAPI API Gateway (this module)
│  CORS · Routing · Validation│
└────────────┬────────────────┘
             │
      ┌──────┴───────┐
      ▼              ▼
AyurRecommender   AyurMedicineRecommender
      │              │
      ▼              ▼
FoodProcessor   MedicineProcessor
      │              │
      ▼              ▼
cleaned_foods  cleaned_medicines
    .json           .json
```

---

## 2. Mathematics Behind It

Not applicable. `main.py` contains no mathematical logic of its own. It is a pure orchestration and transport layer: all recommendation scoring, filtering, and ranking mathematics are encapsulated within `AyurRecommender` and `AyurMedicineRecommender`. Refer to the documentation for those modules for the relevant mathematical formulations.

---

## 3. How It Works

### 3.1 Application Startup

On server start, the module performs two actions before serving any requests:

1. Appends `<project_root>/models/` to `sys.path` to enable imports of `AyurRecommender` and `AyurMedicineRecommender` without package installation.
2. Instantiates `food_recommender = AyurRecommender()` and `medicine_recommender = AyurMedicineRecommender()` as **module-level singletons**. Both constructors eagerly load their respective datasets from JSON cache (or trigger a full processing pipeline on first run). This means data loading occurs once at startup, and all incoming requests share these pre-loaded instances.

### 3.2 CORS Configuration

The `CORSMiddleware` is configured to accept cross-origin requests from `http://localhost:3000` and `http://127.0.0.1:3000` — the default addresses for a React development server. All HTTP methods and headers are permitted from these origins, and credentials are allowed.

### 3.3 Request Lifecycle (per endpoint)

```
HTTP GET Request
      │
      ▼
[1] FastAPI parses query parameters
    → Pydantic validates types and ranges (ge/le constraints on Query())
    → Raises HTTP 422 Unprocessable Entity automatically on type/constraint violations
      │
      ▼
[2] Route handler executes
    ├─ Manual body_type validation → HTTP 400 on invalid value
    ├─ Calls singleton recommender method
    └─ Constructs response message based on result content
      │
      ▼
[3] Response dict assembled
    → Pydantic response_model validates and serialises output
    → Returns HTTP 200 with JSON body
      │
      ▼
[4] Exception fallback (catch-all)
    → Any unhandled exception → HTTP 500 with detail=str(e)
```

### 3.4 Endpoint Inventory

The application exposes **13 HTTP GET endpoints** across two functional domains:

**System / Utility:**

| Endpoint | Handler | Purpose |
|---|---|---|
| `GET /` | `root()` | Liveness probe — confirms API is running |
| `GET /health` | `health_check()` | Health check — returns status and service name |

**Food Domain:**

| Endpoint | Handler | Purpose |
|---|---|---|
| `GET /api/recommend` | `get_recommendations()` | Core food recommendation by dosha and preferences |
| `GET /api/foods` | `get_all_foods()` | Retrieve full food catalogue |
| `GET /api/categories` | `get_categories()` | List all food categories |
| `GET /api/food/{food_name}` | `get_food_details()` | Detailed record for a specific food |

**Medicine Domain:**

| Endpoint | Handler | Purpose |
|---|---|---|
| `GET /api/recommend/medicine` | `get_medicine_recommendations()` | Core medicine recommendation by dosha and disease |
| `GET /api/medicines` | `get_all_medicines()` | Retrieve full medicine catalogue |
| `GET /api/medicine/formulation-types` | `get_medicine_formulation_types()` | List all formulation types |
| `GET /api/medicine/body-systems` | `get_medicine_body_systems()` | List all body systems |
| `GET /api/medicine/{medicine_name}` | `get_medicine_details()` | Detailed record for a specific medicine |
| `GET /api/medicine/common-conditions` | `get_common_conditions()` | List of searchable condition strings |

> ⚠️ **Route Conflict Warning:** `GET /api/medicine/common-conditions` and `GET /api/medicine/{medicine_name}` share the same URL pattern prefix. FastAPI resolves routes in **declaration order** — `common-conditions` must be declared before `{medicine_name}` in the source file to avoid the literal string `"common-conditions"` being captured as a path parameter. In the current implementation this ordering is **not respected** (see Section 9).

### 3.5 Dynamic Response Messaging

Both recommendation endpoints inspect the content of the returned results to determine a human-readable `message` field:

**Food Recommendations (`/api/recommend`):**

```
results[0]["recommendation_type"]
  == "highly_suitable"    → "Highly suitable foods for {BodyType} type"
  == "moderately_suitable"→ "Moderately suitable foods for {BodyType} type"
  == other/unknown        → "General food recommendations for {BodyType} type"
  (empty list)            → "No recommendations found for {BodyType} type"
```

**Medicine Recommendations (`/api/recommend/medicine`):**

```
any("fallback_note" in rec for rec in results)
  == True   → "General medicine recommendations for {BodyType} with {disease}"
  == False  → "Targeted medicines for {BodyType} with {disease}"
  (empty list) → "No medicine recommendations found for {BodyType} with {disease}"
```

---

## 4. Dependencies

| Dependency | Type | Version | Purpose |
|---|---|---|---|
| `fastapi` | Third-party | ≥ 0.95.0 recommended | Web framework: routing, request parsing, dependency injection, OpenAPI docs |
| `pydantic` | Third-party | v1 or v2 (FastAPI-compatible) | Request/response schema definition and validation via `BaseModel` |
| `uvicorn` | Third-party | ≥ 0.20.0 recommended | ASGI server for running the FastAPI app (used in `__main__` block) |
| `starlette` | Third-party (transitive) | Bundled with FastAPI | `CORSMiddleware` implementation |
| `typing` | Standard Library | Python 3.5+ | Type hints (`List`, `Optional`, `Dict`, `Any`) |
| `sys` | Standard Library | Built-in | Runtime `sys.path` manipulation |
| `pathlib.Path` | Standard Library | Python 3.4+ | Module-relative path resolution for `sys.path` injection |
| `AyurRecommender` | Internal | Project-specific | Food recommendation engine |
| `AyurMedicineRecommender` | Internal | Project-specific | Medicine recommendation engine |

> **Installation:** `pip install fastapi uvicorn`
>
> **Running the server:** `uvicorn main:app --host 127.0.0.1 --port 8000 --reload`
> or directly: `python main.py`

---

## 5. Input Arguments

### 5.1 `GET /api/recommend` — Query Parameters

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `body_type` | `str` | ✅ Required | — | Ayurvedic dosha type | Must be `"vata"`, `"pitta"`, or `"kapha"` (case-insensitive). HTTP 400 if invalid. |
| `veg_only` | `bool` | ❌ Optional | `true` | Filter for vegetarian/vegan foods only | FastAPI accepts `true`/`false` (case-insensitive) |
| `high_protein` | `bool` | ❌ Optional | `false` | Filter for foods with protein ≥ 8g | FastAPI accepts `true`/`false` |
| `min_rating` | `int` | ❌ Optional | `4` | Minimum dosha suitability rating | Integer in `[1, 5]`. HTTP 422 if outside range. |
| `limit` | `int` | ❌ Optional | `12` | Maximum number of food results | Integer in `[1, 50]`. HTTP 422 if outside range. |

### 5.2 `GET /api/recommend/medicine` — Query Parameters

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `body_type` | `str` | ✅ Required | — | Ayurvedic dosha type | Must be `"vata"`, `"pitta"`, or `"kapha"`. HTTP 400 if invalid. |
| `disease` | `str` | ✅ Required | — | Health condition or symptom to match against therapeutic profiles | Free-form string; lowercased internally |
| `limit` | `int` | ❌ Optional | `8` | Maximum number of medicine results | Integer in `[1, 20]`. HTTP 422 if outside range. |

### 5.3 `GET /api/food/{food_name}` — Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| `food_name` | `str` | ✅ Required | URL-encoded food name for exact (case-insensitive) lookup |

### 5.4 `GET /api/medicine/{medicine_name}` — Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| `medicine_name` | `str` | ✅ Required | URL-encoded medicine name for exact (case-insensitive) lookup |

### 5.5 Parameterless Endpoints

The following endpoints accept no parameters: `GET /`, `GET /health`, `GET /api/foods`, `GET /api/categories`, `GET /api/medicines`, `GET /api/medicine/formulation-types`, `GET /api/medicine/body-systems`, `GET /api/medicine/common-conditions`.

---

## 6. Output Format

All endpoints return `application/json`. The following describes each response schema.

---

### 6.1 `GET /api/recommend` → `RecommendationResponse`

```json
{
  "body_type": "vata",
  "recommended_foods": [ /* List of FoodItem objects */ ],
  "total_found": 12,
  "message": "Highly suitable foods for Vata type",
  "filters_applied": {
    "veg_only": true,
    "high_protein": false,
    "min_rating": 4,
    "limit": 12
  }
}
```

**`FoodItem` Schema:**

| Field | Type | Nullable | Description |
|---|---|---|---|
| `food_name` | `str` | No | Display name of the food |
| `food_type` | `str` | No | Dietary type (`"veg"`, `"vegan"`, etc.) |
| `category` | `str` | No | Food group category |
| `protein_g` | `float` | No | Protein content (grams) |
| `carbs_g` | `float` | No | Carbohydrate content (grams) |
| `fat_g` | `float` | No | Fat content (grams) |
| `fiber_g` | `float` | No | Dietary fiber (grams) |
| `calories_kcal` | `float` | No | Caloric value (kcal) |
| `vitamins` | `str` | No | Vitamin profile |
| `minerals` | `str` | No | Mineral profile |
| `health_tags` | `str` | No | Health benefit tags |
| `vaat_suitable` | `str` | Yes | Vata suitability label |
| `pit_suitable` | `str` | Yes | Pitta suitability label |
| `kapha_suitable` | `str` | Yes | Kapha suitability label |
| `vaat_rating` | `int` | Yes | Vata rating (1–5) |
| `pit_rating` | `int` | Yes | Pitta rating (1–5) |
| `kapha_rating` | `int` | Yes | Kapha rating (1–5) |
| `suitability_reason_vaat` | `str` | Yes | Vata suitability explanation |
| `suitability_reason_pit` | `str` | Yes | Pitta suitability explanation |
| `suitability_reason_kapha` | `str` | Yes | Kapha suitability explanation |
| `dosha_rating` | `int` | Yes | Injected dosha-specific rating for current request |
| `dosha_reason` | `str` | Yes | Injected dosha-specific reason for current request |
| `recommendation_type` | `str` | Yes | `"highly_suitable"`, `"moderately_suitable"`, or `"general_fallback"` |
| `fallback_message` | `str` | Yes | Present only for fallback-tier results |

---

### 6.2 `GET /api/recommend/medicine` → `MedicineRecommendationResponse`

```json
{
  "body_type": "vata",
  "disease": "insomnia",
  "recommended_medicines": [ /* List of MedicineItem objects */ ],
  "total_found": 8,
  "message": "Targeted medicines for Vata with insomnia",
  "filters_applied": {
    "body_type": "vata",
    "disease": "insomnia",
    "limit": 8
  }
}
```

**`MedicineItem` Schema:**

| Field | Type | Nullable | Description |
|---|---|---|---|
| `medicine_name` | `str` | No | Display name of the medicine |
| `formulation_type` | `str` | No | Physical form (e.g., `"Churna"`, `"Vati"`) |
| `rasa_taste` | `str` | No | Ayurvedic taste classification |
| `virya_potency` | `str` | No | Thermal potency (`"Hot"` / `"Cold"`) |
| `dosage` | `str` | No | Recommended dosage instructions |
| `anupana` | `str` | No | Vehicle / co-administration substance |
| `dosha_match_score` | `int` | No | Dosha affinity score for the requested body type |
| `disease_relevance` | `int` | No | Keyword match score against stated disease |
| `therapeutic_uses` | `str` | No | Free-text therapeutic use description |
| `reason` | `str` | No | Auto-generated recommendation reason |
| `fallback_note` | `str` | Yes | Present only for Tridosha fallback results |

---

### 6.3 `GET /api/foods` → `FoodsListResponse`

```json
{
  "foods": [ /* List of FoodItem objects */ ],
  "total_count": 90,
  "categories": ["Dairy", "Fruits", "Grains", "Legumes", "Vegetables"]
}
```

### 6.4 `GET /api/categories`

```json
{
  "categories": ["Dairy", "Fruits", "Grains", "Legumes", "Vegetables"],
  "total_count": 5
}
```

### 6.5 `GET /api/food/{food_name}`

Returns the raw food dictionary from the dataset (unvalidated by Pydantic). HTTP 404 if not found.

### 6.6 `GET /api/medicines`

```json
{
  "medicines": [ /* List of raw medicine dicts */ ],
  "total_count": 120,
  "formulation_types": ["Churna", "Ghrita", "Kwath", "Tablet", "Vati"],
  "body_systems": ["Digestive", "Nervous", "Respiratory"]
}
```

### 6.7 `GET /api/medicine/formulation-types`

```json
{
  "formulation_types": ["Churna", "Ghrita", "Kwath", "Tablet", "Vati"],
  "total_count": 5
}
```

### 6.8 `GET /api/medicine/body-systems`

```json
{
  "body_systems": ["Digestive", "Musculoskeletal", "Nervous", "Respiratory", "Skin"],
  "total_count": 5
}
```

### 6.9 `GET /api/medicine/{medicine_name}`

Returns the raw medicine dictionary from the dataset. HTTP 404 if not found.

### 6.10 `GET /api/medicine/common-conditions`

```json
{
  "conditions": ["insomnia", "acidity", "joint pain", "cough", "headache", "..."],
  "total_count": 14
}
```

### 6.11 Error Responses

| HTTP Status | Trigger | Response Body |
|---|---|---|
| `400 Bad Request` | Invalid `body_type` value | `{"detail": "body_type must be one of: vata, pitta, kapha"}` |
| `404 Not Found` | Food or medicine name not found | `{"detail": "Food 'xyz' not found"}` |
| `422 Unprocessable Entity` | Parameter type mismatch or out-of-range value | FastAPI's default validation error schema |
| `500 Internal Server Error` | Any unhandled exception in handler | `{"detail": "<exception message>"}` |

---

## 7. Example

### 7.1 Food Recommendation Request

```bash
curl "http://localhost:8000/api/recommend?body_type=vata&veg_only=true&min_rating=4&limit=3"
```

**Expected Response:**

```json
{
  "body_type": "vata",
  "recommended_foods": [
    {
      "food_name": "Sesame Seeds",
      "food_type": "veg",
      "category": "Seeds & Nuts",
      "protein_g": 17.7,
      "carbs_g": 23.4,
      "fat_g": 49.7,
      "fiber_g": 11.8,
      "calories_kcal": 573.0,
      "vitamins": "B1, B2, E",
      "minerals": "Calcium, Iron, Magnesium",
      "health_tags": "anti-inflammatory, bone-health",
      "dosha_rating": 5,
      "dosha_reason": "Warming, nourishing, and grounding — ideal for balancing dry Vata",
      "recommendation_type": "highly_suitable",
      "fallback_message": null
    }
  ],
  "total_found": 3,
  "message": "Highly suitable foods for Vata type",
  "filters_applied": {
    "veg_only": true,
    "high_protein": false,
    "min_rating": 4,
    "limit": 3
  }
}
```

---

### 7.2 Medicine Recommendation Request

```bash
curl "http://localhost:8000/api/recommend/medicine?body_type=pitta&disease=acidity&limit=3"
```

**Expected Response:**

```json
{
  "body_type": "pitta",
  "disease": "acidity",
  "recommended_medicines": [
    {
      "medicine_name": "Avipattikar Churna",
      "formulation_type": "Churna",
      "rasa_taste": "Sweet, Bitter",
      "virya_potency": "Cold",
      "dosage": "3-6g with warm water before meals",
      "anupana": "Warm water",
      "dosha_match_score": 9,
      "disease_relevance": 8,
      "therapeutic_uses": "Hyperacidity, GERD, gastritis, heartburn",
      "reason": "Excellent for Pitta and helps with acid (Cold potency, Sweet, Bitter taste).",
      "fallback_note": null
    }
  ],
  "total_found": 3,
  "message": "Targeted medicines for Pitta with acidity",
  "filters_applied": {
    "body_type": "pitta",
    "disease": "acidity",
    "limit": 3
  }
}
```

---

### 7.3 Invalid Body Type — HTTP 400

```bash
curl "http://localhost:8000/api/recommend?body_type=fire"
```

```json
{
  "detail": "body_type must be one of: vata, pitta, kapha"
}
```

---

### 7.4 Rating Out of Range — HTTP 422

```bash
curl "http://localhost:8000/api/recommend?body_type=vata&min_rating=9"
```

```json
{
  "detail": [
    {
      "loc": ["query", "min_rating"],
      "msg": "ensure this value is less than or equal to 5",
      "type": "value_error.number.not_le",
      "ctx": {"limit_value": 5}
    }
  ]
}
```

---

### 7.5 Food Not Found — HTTP 404

```bash
curl "http://localhost:8000/api/food/Dragonfire%20Pepper"
```

```json
{
  "detail": "Food 'Dragonfire Pepper' not found"
}
```

---

### 7.6 Interactive API Documentation

FastAPI automatically generates Swagger UI and ReDoc documentation accessible at runtime:

- **Swagger UI:** `http://localhost:8000/docs`
- **ReDoc:** `http://localhost:8000/redoc`
- **OpenAPI JSON:** `http://localhost:8000/openapi.json`

---

### 7.7 Running the Server

```bash
# Via uvicorn directly (recommended for production-like testing)
uvicorn main:app --host 127.0.0.1 --port 8000 --reload

# Via Python directly (uses __main__ block)
python main.py
```

---

## 8. Pros / Advantages

**Singleton Recommenders for Zero Per-Request Overhead**
Both `AyurRecommender` and `AyurMedicineRecommender` are instantiated once at module load time and shared across all requests. This means dataset loading occurs only once per server lifetime rather than once per request, significantly reducing latency on all API calls.

**Automatic Schema Validation via Pydantic**
Defining `FoodItem`, `MedicineItem`, `RecommendationResponse`, and `MedicineRecommendationResponse` as Pydantic `BaseModel` classes means FastAPI validates and serialises all responses automatically. This prevents malformed data from reaching the client and provides type-safe contracts between the API and its consumers.

**Built-in `ge`/`le` Constraints on Query Parameters**
Using `Query(4, ge=1, le=5)` delegates range validation entirely to FastAPI, which returns a structured HTTP 422 error with field-level detail before the handler even executes. This eliminates boilerplate validation code inside handler functions.

**Layered Error Handling**
The dual-layer error handling — FastAPI's automatic HTTP 422 for parameter violations, explicit HTTP 400 for semantic validation (`body_type`), HTTP 404 for missing resources, and catch-all HTTP 500 — gives API consumers clear and actionable error signals at each failure mode.

**Context-Aware Response Messaging**
The dynamic `message` field in recommendation responses reflects the actual quality tier of results returned (highly suitable, moderately suitable, fallback, or empty), allowing frontend clients to display appropriate context without additional logic.

**Auto-Generated API Documentation**
FastAPI generates live, interactive Swagger UI and ReDoc documentation from route and schema definitions. With docstrings defined on each handler, the documentation is immediately useful to frontend developers and API consumers without any additional effort.

**Clean HTTPException Re-raising in Detail Routes**
In `get_food_details()` and `get_medicine_details()`, the pattern `except HTTPException: raise` ensures that intentional HTTP 404 responses are not swallowed by the outer `except Exception` catch, preserving correct status codes.

---

## 9. Edge Cases & Limitations

### Edge Cases Handled

| Scenario | Behaviour |
|---|---|
| `body_type` in uppercase (e.g., `"VATA"`) | Normalised via `.lower()` before validation and forwarding |
| `disease` with leading/trailing whitespace | Normalised via `.lower()` and forwarded; `AyurMedicineRecommender` applies `.strip()` internally |
| Food or medicine name not found | HTTP 404 with descriptive `detail` message |
| Recommender returns empty list | HTTP 200 with `total_found: 0` and appropriate `message` string — not a 404 |
| Parameter constraint violation (`min_rating=9`) | HTTP 422 with FastAPI's structured field-level error |
| Unhandled internal exception | HTTP 500 with `detail=str(e)` |

### Known Limitations

**Critical Route Conflict: `/api/medicine/common-conditions` vs `/api/medicine/{medicine_name}`**
In FastAPI, routes are matched in the order they are declared in the source file. The parameterised route `GET /api/medicine/{medicine_name}` is declared **before** `GET /api/medicine/common-conditions`. This means a request to `/api/medicine/common-conditions` will be captured by the `{medicine_name}` handler and attempt to look up a medicine named `"common-conditions"`, returning HTTP 404 instead of the conditions list. The static route must be declared first to resolve correctly.

**CORS Restricted to Local Development Origins**
The `allow_origins` list contains only `localhost:3000` and `127.0.0.1:3000`. Deploying to any other environment (staging, production, or a different port) requires manual updates to this list. There is no environment-variable-based configuration.

**Internal Exceptions Exposed in HTTP 500 Responses**
The catch-all `except Exception as e: raise HTTPException(status_code=500, detail=str(e))` pattern exposes raw Python exception messages to API consumers. This may leak internal implementation details (file paths, variable names, library errors) in production environments, which is both a security concern and a poor UX.

**`GET /api/medicines` and `GET /api/foods` Return Full Unfiltered Datasets**
These endpoints return every record in the database with no pagination, search, or filtering. For large datasets, this can produce very large response payloads and increase latency noticeably.

**No Authentication or Rate Limiting**
All endpoints are publicly accessible with no API key, token, or rate limiting mechanism. This is suitable for local development but makes the API unsuitable for public deployment without an authentication layer.

**`dosha_match_score` Typed as `int` in `MedicineItem`**
The `MedicineItem` Pydantic model declares `dosha_match_score: int`, but the underlying recommender can produce `float` values (e.g., the fallback synthetic score of `5` and dataset values coerced via `pd.to_numeric()`). Pydantic will coerce compatible floats like `5.0` to `5`, but values like `7.5` would be truncated rather than rejected, potentially altering data.

**Double `body_type` Validation**
Both recommendation handlers manually check `body_type.lower() not in ["vata", "pitta", "kapha"]` and raise HTTP 400, despite the recommender classes raising `ValueError` for the same condition. This redundancy is harmless but means the validation logic is duplicated rather than centralised.

**No Request Logging**
There is no logging of incoming requests, parameter values, or response outcomes. This makes debugging production issues and monitoring usage patterns difficult.

---

## 10. Future Improvements

**Fix the Route Conflict for `/api/medicine/common-conditions`**
Move the static route declaration above the parameterised route in the source file:

```python
# CORRECT ORDER — static routes before parameterised routes
@app.get("/api/medicine/common-conditions")
async def get_common_conditions(): ...

@app.get("/api/medicine/{medicine_name}")   # Must come AFTER
async def get_medicine_details(medicine_name: str): ...
```

**Environment-Driven CORS Configuration**
Replace the hardcoded origins list with an environment variable to support multi-environment deployment:

```python
import os
origins = os.getenv("CORS_ORIGINS", "http://localhost:3000").split(",")
app.add_middleware(CORSMiddleware, allow_origins=origins, ...)
```

**Sanitise HTTP 500 Error Responses**
Avoid leaking internal exception details to clients in production. Use a logging call to capture the full exception internally, and return a generic message externally:

```python
import logging
logger = logging.getLogger(__name__)

except Exception as e:
    logger.exception("Unhandled error in endpoint")
    raise HTTPException(status_code=500, detail="An internal error occurred.")
```

**Add Pagination to List Endpoints**
Introduce `offset` and `limit` parameters to `GET /api/foods` and `GET /api/medicines` to avoid returning unbounded datasets:

```python
@app.get("/api/foods")
async def get_all_foods(offset: int = Query(0, ge=0), limit: int = Query(50, ge=1, le=200)):
    all_foods = food_recommender.foods
    return {"foods": all_foods[offset:offset+limit], "total_count": len(all_foods)}
```

**Centralise `body_type` Validation**
Extract the dosha validation into a shared FastAPI dependency to eliminate duplication across the two recommendation handlers:

```python
from fastapi import Depends

def validate_body_type(body_type: str = Query(...)) -> str:
    if body_type.lower() not in ["vata", "pitta", "kapha"]:
        raise HTTPException(status_code=400, detail="body_type must be one of: vata, pitta, kapha")
    return body_type.lower()

@app.get("/api/recommend")
async def get_recommendations(body_type: str = Depends(validate_body_type), ...):
    ...
```

**Fix `dosha_match_score` Type in `MedicineItem`**
Change the type annotation to `float` to correctly accommodate dataset scores and avoid silent truncation:

```python
class MedicineItem(BaseModel):
    dosha_match_score: float   # was: int
    disease_relevance: int
```

**Add Request Logging Middleware**
Implement a lightweight logging middleware to record request method, path, query parameters, response status, and processing time for every request:

```python
import time, logging
logger = logging.getLogger("api")

@app.middleware("http")
async def log_requests(request, call_next):
    start = time.time()
    response = await call_next(request)
    duration = round((time.time() - start) * 1000, 1)
    logger.info(f"{request.method} {request.url.path} → {response.status_code} ({duration}ms)")
    return response
```

**Authentication Layer**
Add API key authentication for non-development deployments using FastAPI's `Security` dependency with an `APIKeyHeader` scheme, protecting endpoints from unauthorised access.

**POST Endpoint for Recommendations**
Consider supplementing the `GET` recommendation endpoints with `POST` equivalents that accept request bodies (JSON), which is more ergonomic for complex filter combinations and avoids URL length limitations.

**Lifespan-Based Startup with `asynccontextmanager`**
Replace module-level singleton instantiation with FastAPI's `lifespan` context manager (introduced in FastAPI 0.93) for cleaner startup/shutdown lifecycle management:

```python
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.food_recommender = AyurRecommender()
    app.state.medicine_recommender = AyurMedicineRecommender()
    yield

app = FastAPI(lifespan=lifespan)
```

---

*Documentation generated for `main.py` — Ayurvedic Recommendation System API Gateway.*
