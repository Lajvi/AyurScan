# Technical Documentation: `medicine_recommender.py`

> **Project:** Ayurvedic Medicine Recommendation System
> **Module:** `medicine_recommender.py`
> **Class:** `AyurMedicineRecommender`
> **Version:** 1.0.0
> **Language:** Python 3.8+

---

## 1. Introduction

`medicine_recommender.py` implements the **Ayurvedic Medicine Recommendation Engine** — a multi-stage, rule-based recommendation system that suggests Ayurvedic medicines to users based on two primary inputs: their *dosha* (Ayurvedic body constitution type) and a stated health condition or symptom.

The module exposes the primary class `AyurMedicineRecommender` and a convenience wrapper function `get_medicine_recommendations()`. Internally, it delegates all raw data access to a `MedicineProcessor` dependency, keeping the recommendation and scoring logic cleanly decoupled from persistence concerns.

Within the broader project architecture, this module sits at the **medicine recommendation service layer**, parallel to the food recommendation layer (`recommender.py`). It accepts structured user inputs, applies a sequential pipeline of filtering, keyword-based scoring, sorting, result enrichment, and fallback logic, and returns ranked medicine dictionaries ready for consumption by a frontend interface or API endpoint.

A key design distinction from the food recommender is the introduction of a **disease relevance scoring mechanism**, which enables the system to cross-reference a user's stated condition against medicine therapeutic profiles using an extensible keyword mapping system.

---

## 2. Mathematics Behind It

The recommendation pipeline combines **threshold-based filtering** with a **multi-signal additive scoring model**, followed by **lexicographic sorting**.

### 2.1 Dosha Score Threshold Filter

A medicine $m$ passes the body type filter if any of the following conditions hold for dosha $d \in \{\text{vata}, \text{pitta}, \text{kapha}\}$:

$$
\text{Pass}_{\text{dosha}}(m, d) = \bigl[\text{score}(m, d) \geq \theta\bigr] \;\vee\; \bigl[d \in \text{dosha\_label}(m)\bigr] \;\vee\; \bigl[\text{"tridosha"} \in \text{dosha\_label}(m)\bigr]
$$

where the score threshold $\theta = 6$ and $\text{score}(m, d) \in \mathbb{R}_{\geq 0}$ is the pre-assigned dosha affinity score for medicine $m$.

### 2.2 Keyword Extraction

Given a raw disease string $q$, the system extracts a set of canonical keywords $K(q)$ by tokenising the input and mapping each token against a predefined synonym dictionary $\mathcal{M}$:

$$
K(q) = \bigcup_{\,w \,\in\, \text{tokens}(q)} \;\Bigl(\mathcal{M}(w) \;\cup\; \{w \mid |w| > 2,\; w \notin \mathcal{M}\}\Bigr)
$$

where $\mathcal{M}(w)$ returns the expansion set for token $w$ if it maps to a known medical concept, or $\emptyset$ otherwise. Duplicate keywords are removed via set conversion.

### 2.3 Disease Relevance Score

For each medicine $m$ passing the dosha filter, a disease relevance score $R(m, q)$ is computed by checking each keyword $k \in K(q)$ against two text fields:

$$
R(m, q) = \sum_{k \in K(q)} \Bigl(2 \cdot \mathbb{1}[k \in \text{therapeutic\_uses}(m)] + 1 \cdot \mathbb{1}[k \in \text{body\_system}(m)]\Bigr)
$$

The weights reflect the relative importance of a direct therapeutic match (weight $= 2$) over a general body system match (weight $= 1$).

### 2.4 Composite Sort Key

Medicines are ranked using a **lexicographic (tuple) sort** on three signals in descending priority order:

$$
\text{sort\_key}(m) = \Bigl(\text{score}(m, d),\; R(m, q),\; -\text{ord}\bigl(\text{name}(m)\bigr)\Bigr)
$$

In practice, this means:

1. **Primary:** Dosha match score — higher is better.
2. **Secondary:** Disease relevance score — higher is better.
3. **Tertiary:** Medicine name — alphabetical, as a stable tie-breaker.

### 2.5 Fallback Scoring

When the primary pipeline yields no results, Tridosha medicines receive a fixed synthetic dosha match score:

$$
\text{score}_{\text{fallback}}(m) = 5 \quad \forall\; m \in \mathcal{T}
$$

where $\mathcal{T}$ denotes the set of all Tridosha medicines. These are then re-scored by $R(m, q)$ and sorted identically to the primary pipeline, capped at 5 results.

---

## 3. How It Works

### 3.1 Initialization

When `AyurMedicineRecommender()` is instantiated, the constructor creates a `MedicineProcessor` object and eagerly loads the entire medicine dataset via `load_medicines()`, caching it in `self.medicines`. All subsequent recommendation calls operate entirely in-memory.

### 3.2 `recommend_medicines()` — Pipeline Execution Flow

```
INPUT: body_type, disease, limit
         │
         ▼
[1] Normalize Inputs
    → body_type.lower(), disease.lower().strip()
    → Raises ValueError for invalid body_type
         │
         ▼
[2] _filter_by_body_type(body_type)
    → For each medicine, retrieve dosha-specific score
    → Admit if: score ≥ 6 OR body_type in dosha_label OR "tridosha" in dosha_label
    → Attach dosha_match_score to each admitted medicine
         │
         ▼
[3] _score_by_disease_relevance(medicines, disease, body_type)
    → Call _extract_keywords(disease) → keyword set K(q)
    → For each medicine, scan therapeutic_uses (+2) and body_system (+1) per keyword
    → Attach disease_relevance_score and matched_keywords to each medicine
         │
         ▼
[4] _sort_medicines(medicines, body_type)
    → Sort descending by (dosha_match_score, disease_relevance_score, medicine_name)
         │
         ▼
[5] _prepare_results(sorted[:limit], body_type, disease)
    → Project selected fields into clean result dicts
    → Call _generate_reason() for each medicine → human-readable explanation
         │
         ▼
[6] Fallback Check
    → If results list is empty → _get_fallback_recommendations()
       → Collect all Tridosha medicines
       → Re-score by disease relevance
       → Sort, prepare (up to 5), add fallback_note field
         │
         ▼
OUTPUT: List[Dict] (max `limit` items, or ≤ 5 in fallback)
```

### 3.3 `_extract_keywords()` — Keyword Expansion

Tokenises the input disease string using `re.findall(r'\b\w+\b', ...)`, then maps each token against a 10-category synonym dictionary. If a token matches a known medical concept (or is itself a variation), the full expansion set for that concept is added to the keyword pool. Unmapped tokens longer than 2 characters are added as-is. Duplicates are removed via `set()`.

### 3.4 `_generate_reason()` — Dynamic Reason Generation

Constructs a human-readable recommendation reason by combining three components in a template string:

- **Dosha strength label:** `"Excellent"` (score ≥ 7), `"Suitable"` (score ≥ 5), or `"Good"` (otherwise).
- **Disease relevance phrase:** Derived from the first matched keyword, or a generic phrase if keywords are present but unlabeled.
- **Property parenthetical:** Appends `virya_potency` and `rasa_taste` if available in the medicine record.

### 3.5 Utility Methods

| Method | Behaviour |
|---|---|
| `get_medicine_details(name)` | Delegates to `MedicineProcessor.get_medicine_by_name()`. Returns `Optional[Dict]`. |
| `get_formulation_types()` | Delegates to `MedicineProcessor.get_formulation_types()`. Returns `List[str]`. |
| `get_body_systems()` | Delegates to `MedicineProcessor.get_body_systems()`. Returns `List[str]`. |
| `get_common_conditions()` | Returns a hardcoded list of 14 commonly searchable condition strings. |

---

## 4. Dependencies

| Dependency | Type | Version | Purpose |
|---|---|---|---|
| `typing` | Standard Library | Python 3.5+ | Type hints (`List`, `Dict`, `Optional`, `Tuple`) |
| `sys` | Standard Library | Built-in | Runtime `sys.path` manipulation |
| `pathlib.Path` | Standard Library | Python 3.4+ | Filesystem path resolution |
| `re` | Standard Library | Built-in | Regex tokenisation in `_extract_keywords()` |
| `medicine_processor.MedicineProcessor` | Internal Module | Project-specific | Medicine data loading and lookup |

> **Note:** `MedicineProcessor` is loaded dynamically by appending `<project_root>/data/` to `sys.path` at import time. It must exist at `<project_root>/data/medicine_processor.py` and is not pip-installable.

> **Note:** `Tuple` is imported from `typing` but not used in any function signature in the current implementation. It is available for future use.

---

## 5. Input Arguments

### 5.1 `AyurMedicineRecommender.recommend_medicines()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `body_type` | `str` | ✅ Required | — | Ayurvedic dosha / body constitution type | Must be one of `"vata"`, `"pitta"`, `"kapha"` (case-insensitive). Raises `ValueError` otherwise. |
| `disease` | `str` | ✅ Required | — | Health condition, symptom, or disease name to match against medicine therapeutic profiles | Free-form string; lowercased and stripped internally. Multi-word inputs (e.g., `"joint pain"`) are supported. |
| `limit` | `int` | ❌ Optional | `8` | Maximum number of medicine results to return from the primary pipeline | Positive integer. The fallback pipeline is independently capped at `5` regardless of this value. |

### 5.2 `AyurMedicineRecommender.get_medicine_details()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `medicine_name` | `str` | ✅ Required | — | Exact or matching name of the medicine to look up | Match behaviour (exact vs. partial) is determined by `MedicineProcessor`. |

### 5.3 `get_medicine_recommendations()` (Convenience Function)

Accepts the same parameters as `recommend_medicines()`. Refer to Section 5.1.

---

## 6. Output Format

### 6.1 `recommend_medicines()` → `List[Dict]`

Each dictionary in the returned list is a **projected and enriched** medicine record containing the following fields:

| Field | Type | Present When | Description |
|---|---|---|---|
| `medicine_name` | `str` | Always | Display name of the Ayurvedic medicine. |
| `formulation_type` | `str` | Always | Physical form of the medicine (e.g., `"Churna"`, `"Kwath"`, `"Tablet"`, `"Ghrita"`). |
| `rasa_taste` | `str` | Always | Ayurvedic taste classification (e.g., `"Bitter"`, `"Sweet"`, `"Pungent"`). |
| `virya_potency` | `str` | Always | Thermal potency of the medicine (`"Hot"` or `"Cold"`). |
| `dosage` | `str` | Always | Recommended dosage instructions (as stored in the dataset). |
| `anupana` | `str` | Always | Recommended vehicle/co-administration substance (e.g., honey, warm water). |
| `dosha_match_score` | `int` or `float` | Always | Dosha affinity score for the requested body type. Range: dataset-dependent; fallback medicines receive a synthetic score of `5`. |
| `disease_relevance` | `int` | Always | Additive keyword match score against the stated disease. Value of `0` means no keyword match was found. |
| `therapeutic_uses` | `str` | Always | Raw therapeutic uses text from the dataset. |
| `reason` | `str` | Always | Auto-generated, human-readable explanation combining dosha fitness, disease relevance, and medicine properties. |
| `fallback_note` | `str` | Fallback only | Present only when the result was produced by the Tridosha fallback pipeline. Value: `"Tridosha medicine suitable for all body types"`. |

### 6.2 `get_medicine_details()` → `Optional[Dict]`

Returns a full medicine dictionary as stored in the dataset, or `None` if not found. The schema is dataset-dependent and is not projected or filtered.

### 6.3 `get_formulation_types()` → `List[str]`

Returns a list of unique formulation type strings as provided by `MedicineProcessor`.

### 6.4 `get_body_systems()` → `List[str]`

Returns a list of unique body system strings as provided by `MedicineProcessor`.

### 6.5 `get_common_conditions()` → `List[str]`

Returns a static list of 14 condition strings:

```python
["insomnia", "acidity", "joint pain", "cough", "headache",
 "digestion", "stress", "skin problems", "respiratory issues",
 "immune support", "fever", "fatigue", "anxiety", "depression"]
```

---

## 7. Example

### 7.1 Basic Recommendation — Vata Body Type with Insomnia

```python
from medicine_recommender import AyurMedicineRecommender

recommender = AyurMedicineRecommender()

results = recommender.recommend_medicines(
    body_type="vata",
    disease="insomnia",
    limit=3
)

for i, med in enumerate(results, 1):
    print(f"{i}. {med['medicine_name']} ({med['formulation_type']})")
    print(f"   Dosha Score : {med['dosha_match_score']}")
    print(f"   Relevance   : {med['disease_relevance']}")
    print(f"   Dosage      : {med['dosage']}")
    print(f"   Anupana     : {med['anupana']}")
    print(f"   Reason      : {med['reason']}")
    print()
```

**Expected Output:**

```
1. Ashwagandha Churna (Churna)
   Dosha Score : 9
   Relevance   : 6
   Dosage      : 3-6g with warm milk before bed
   Anupana     : Warm milk or ghee
   Reason      : Excellent for Vata and helps with sleep (Hot potency, Bitter taste).

2. Brahmi Ghrita (Ghrita)
   Dosha Score : 8
   Relevance   : 4
   Dosage      : 1 tsp twice daily
   Anupana     : Warm water or milk
   Reason      : Excellent for Vata and helps with insomnia (Cold potency, Bitter taste).

3. Tagara Churna (Churna)
   Dosha Score : 7
   Relevance   : 4
   Dosage      : 1-3g with warm water
   Anupana     : Warm water
   Reason      : Excellent for Vata and helps with sleep (Hot potency, Pungent taste).
```

---

### 7.2 Recommendation with Fallback — Rare Condition

```python
results = recommender.recommend_medicines(
    body_type="pitta",
    disease="rare tropical fever",  # Unlikely to match dataset keywords
    limit=5
)

for med in results:
    print(f"{med['medicine_name']} | Relevance: {med['disease_relevance']}")
    if 'fallback_note' in med:
        print(f"  [FALLBACK] {med['fallback_note']}")
```

**Expected Output (fallback tier):**

```
Triphala Churna | Relevance: 2
  [FALLBACK] Tridosha medicine suitable for all body types
Guduchi Kwath | Relevance: 2
  [FALLBACK] Tridosha medicine suitable for all body types
Amalaki Churna | Relevance: 0
  [FALLBACK] Tridosha medicine suitable for all body types
```

---

### 7.3 Using the Convenience Function

```python
from medicine_recommender import get_medicine_recommendations

recommendations = get_medicine_recommendations(
    body_type="kapha",
    disease="cough",
    limit=5
)

for rec in recommendations:
    print(f"{rec['medicine_name']}: {rec['reason']}")
```

---

### 7.4 Inspecting Available Metadata

```python
recommender = AyurMedicineRecommender()

print("Formulation Types:", recommender.get_formulation_types())
print("Body Systems     :", recommender.get_body_systems())
print("Common Conditions:", recommender.get_common_conditions())
print("Total Medicines  :", len(recommender.medicines))
```

**Expected Output:**

```
Formulation Types: ['Churna', 'Ghrita', 'Kwath', 'Tablet', 'Vati', 'Asava']
Body Systems     : ['Digestive', 'Nervous', 'Respiratory', 'Musculoskeletal', 'Skin']
Common Conditions: ['insomnia', 'acidity', 'joint pain', 'cough', ...]
Total Medicines  : 120
```

---

## 8. Pros / Advantages

**Structured Multi-Stage Pipeline**
The recommendation logic is decomposed into five clearly named private methods (`_filter_by_body_type`, `_score_by_disease_relevance`, `_extract_keywords`, `_sort_medicines`, `_prepare_results`), each with a single responsibility. This makes the pipeline easy to trace, test, and extend independently.

**Additive Scoring with Field-Weighted Matching**
By assigning a weight of `2` to `therapeutic_uses` matches and `1` to `body_system` matches, the system encodes domain knowledge that a direct therapeutic match is more clinically meaningful than a general body system correspondence. This avoids treating all text fields equally.

**Extensible Keyword Synonym System**
The `_extract_keywords()` method's `keyword_mapping` dictionary cleanly separates medical synonym management from the scoring logic. Adding support for a new condition (e.g., `"diabetes"`) requires only a new key-value pair in that dictionary, with no changes to the scoring or filtering logic.

**Transparent Result Enrichment**
Injecting `dosha_match_score`, `disease_relevance`, `reason`, and `fallback_note` into each result gives downstream consumers full observability into why a medicine was recommended and at what confidence level — enabling UI layers to display caveats or sorting indicators without additional logic.

**Tridosha Fallback Safety Net**
The Tridosha fallback guarantees that a medically conservative set of results (medicines considered safe for all body types) is returned even when the combination of body type and disease condition yields no strong matches. This prevents blank responses in edge-case queries.

**Data Isolation via `medicine.copy()`**
Every medicine dictionary is copied before mutation, preserving `self.medicines` as an immutable in-memory cache across multiple `recommend_medicines()` calls in the same session.

**Zero External Runtime Dependencies**
The module relies exclusively on Python's standard library (`typing`, `sys`, `pathlib`, `re`) and one internal module, making it portable and deployable without dependency management.

---

## 9. Edge Cases & Limitations

### Edge Cases Handled

| Scenario | Behaviour |
|---|---|
| Invalid `body_type` value | Raises `ValueError` with a clear message listing valid options. |
| Disease string with extra whitespace | Stripped via `.strip()` before processing; does not affect keyword extraction. |
| Multi-word disease input (e.g., `"joint pain"`) | Tokenised by `re.findall(r'\b\w+\b', ...)` — each word is processed independently and may match separate keyword expansions. |
| Medicine missing optional fields | All field reads use `.get(field, default)` with safe defaults (`0` for scores, `''` for strings), preventing `KeyError` exceptions. |
| No primary pipeline results | Automatically activates Tridosha fallback pipeline, returning up to 5 results with an explanatory `fallback_note`. |
| Disease string maps to no known keywords | Short tokens ($\leq 2$ characters) are discarded; longer unmapped tokens are used verbatim, producing a valid (possibly zero-scoring) keyword set. |

### Known Limitations

**Hardcoded Score Threshold**
The dosha filter threshold $\theta = 6$ is hardcoded inside `_filter_by_body_type()` with no parameter to override it. A strict dataset where many medicines score below 6 for all doshas could produce sparse initial results.

**Fallback Cap Ignores `limit`**
The Tridosha fallback pipeline is hardcoded to return at most 5 medicines regardless of the caller-supplied `limit`. A caller requesting `limit=10` will receive at most 5 items in the fallback scenario.

**No Input Validation on `limit`**
Passing `limit=0` or a negative integer is not validated and will silently produce an empty list from `sorted_medicines[:0]`. There is no guard or warning for non-positive values.

**Shallow Keyword Mapping Coverage**
The `keyword_mapping` dictionary covers only 10 medical categories. Disease strings falling outside these categories (e.g., `"diabetes"`, `"hypertension"`, `"thyroid"`) produce no mapped expansions, relying entirely on verbatim token matching against dataset text.

**`Tuple` Import is Unused**
`Tuple` is imported from `typing` but never referenced in any function signature in the current codebase. This is a minor code hygiene issue.

**No Deduplication Across Matched Keywords**
If a keyword appears in both `therapeutic_uses` and `body_system` of the same medicine, it is counted in both fields and its label is appended twice to `matched_keywords`. While the score is correct, the duplicate keyword in `matched_keywords` may produce a repeated word in the generated `reason` string.

**Generated Reason Uses Only the First Matched Keyword**
`_generate_reason()` uses only `matched_keywords[0]` for the disease relevance phrase, discarding all other matched terms. This may produce a reason that does not fully reflect the medicine's overall disease relevance.

**`get_common_conditions()` is Static**
The list of common conditions is hardcoded and not derived from the dataset. It may diverge from the actual conditions that yield meaningful results as the medicine database evolves.

**Case-Sensitive Dataset Label Checks**
`body_type in dosha_label` performs a substring check on a lowercased label string. If the dataset contains labels like `"Vata-Pitta"` (hyphenated), the check will still function correctly after lowercasing. However, if the label format changes significantly, the substring match may produce false positives or false negatives.

---

## 10. Future Improvements

**Expose Score Threshold as a Parameter**
Promote the hardcoded `score_threshold = 6` to a configurable parameter of `recommend_medicines()` or the class constructor, enabling callers to widen or narrow the dosha filter without modifying source code.

**Align Fallback Cap with `limit`**
Replace the hardcoded fallback cap of `5` with `min(limit, len(sorted_tridosha))` to ensure consistent behaviour between the primary and fallback pipelines.

**Input Validation for `limit`**
Add an explicit guard to prevent non-positive values:
```python
if limit <= 0:
    raise ValueError("limit must be a positive integer.")
```

**Expand Keyword Mapping Coverage**
Extend `keyword_mapping` to cover a broader range of conditions — particularly lifestyle diseases (diabetes, hypertension, thyroid), mental health conditions (depression, PTSD), and women's health conditions — to improve disease relevance scoring for a wider user base.

**Remove Unused `Tuple` Import**
Clean up the import statement to remove `Tuple` as it is not currently referenced, improving code clarity:
```python
from typing import List, Dict, Optional  # Remove Tuple
```

**Deduplicate `matched_keywords`**
After assembling `matched_keywords`, convert to a set or use an ordered deduplication strategy to prevent duplicate entries:
```python
medicine['matched_keywords'] = list(dict.fromkeys(matched_keywords))
```

**Enrich `_generate_reason()` with Multiple Keywords**
Surface the top 2–3 matched keywords in the reason string rather than only the first, providing a more informative explanation:
```python
primary_keywords = matched_keywords[:3]
disease_reason = f" and helps with {', '.join(primary_keywords)}"
```

**Data-Driven `get_common_conditions()`**
Derive the common conditions list dynamically from the dataset's `therapeutic_uses` field using TF-IDF or frequency analysis, rather than relying on a static hardcoded list. This ensures the list remains accurate as the medicine database grows.

**Weighted Scoring Configurability**
Expose the field weights (currently `2` for `therapeutic_uses` and `1` for `body_system`) as class-level constants or constructor parameters so they can be tuned based on clinical feedback without touching the scoring logic:
```python
THERAPEUTIC_WEIGHT = 2
BODY_SYSTEM_WEIGHT = 1
```

**Result Caching via LRU Cache**
Cache `recommend_medicines()` results keyed on `(body_type, disease, limit)` using `functools.lru_cache` or a dictionary-based cache to avoid reprocessing the same query. Particularly valuable since `get_medicine_recommendations()` creates a new instance on every call.

**Unit Test Suite**
Introduce a comprehensive test suite covering: valid and invalid body types, disease strings with no keyword matches, multi-word disease inputs, fallback pipeline activation, `disease_relevance_score` calculation correctness, and `_generate_reason()` output format validation.

---

*Documentation generated for `medicine_recommender.py` — Ayurvedic Medicine Recommendation Engine.*
