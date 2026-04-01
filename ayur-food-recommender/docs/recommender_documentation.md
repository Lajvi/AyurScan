# Technical Documentation: `recommender.py`

> **Project:** Ayurvedic Food Recommendation System
> **Module:** `recommender.py`
> **Class:** `AyurRecommender`
> **Version:** 1.0.0
> **Language:** Python 3.8+

---

## 1. Introduction

`recommender.py` implements the **Ayurvedic Food Recommendation Engine** — the core intelligence layer of an Ayurveda-based dietary guidance system. Its primary purpose is to match foods from a structured dataset to a user's Ayurvedic *dosha* (body constitution type), while further refining results based on dietary preferences such as vegetarianism and protein requirements.

The module exposes a primary class `AyurRecommender` and a convenience wrapper function `get_recommendations()`. Internally, it delegates all data loading and food retrieval operations to a `FoodProcessor` dependency, keeping the recommendation logic cleanly separated from data access concerns.

Within the broader project architecture, this module sits at the **recommendation service layer** — it receives high-level user preferences, queries the processed food data, applies multi-stage filtering and fallback logic, and returns ranked, enriched food dictionaries ready for consumption by a frontend or API layer.

---

## 2. Mathematics Behind It

The recommendation algorithm does not employ machine learning or probabilistic models. Instead, it applies **deterministic rule-based filtering with ordinal ranking**, grounded in the principles of Ayurvedic dietary science.

### 2.1 Suitability Classification

Each food item carries a pre-assigned suitability label for each dosha:

$$
\text{Suitability}(f, d) \in \{\text{yes}, \text{suitable}, \text{moderately\_suitable}, \text{not\_suitable}\}
$$

where $f$ denotes a food item and $d \in \{\text{vata}, \text{pitta}, \text{kapha}\}$ denotes the dosha.

### 2.2 Minimum Rating Threshold Filter

A food $f$ is admitted to the candidate set only if its dosha-specific rating $r(f, d)$ satisfies:

$$
r(f, d) \geq r_{\min}
$$

where $r_{\min}$ is the caller-supplied `min_rating` parameter and $r(f, d) \in [1, 5]$.

### 2.3 Priority-Based Set Construction

Qualifying foods are partitioned into two ordered priority sets:

$$
S_1 = \{ f \mid \text{Suitability}(f, d) \in \{\text{yes}, \text{suitable}\} \;\wedge\; r(f,d) \geq r_{\min} \}
$$

$$
S_2 = \{ f \mid \text{Suitability}(f, d) = \text{moderately\_suitable} \;\wedge\; r(f,d) \geq r_{\min} \}
$$

### 2.4 Dietary Constraint Filters

Let $\mathbb{1}_{\text{veg}}(f)$ and $\mathbb{1}_{\text{protein}}(f)$ be indicator functions for the optional dietary constraints:

$$
\mathbb{1}_{\text{veg}}(f) = \begin{cases} 1 & \text{if } \text{food\_type}(f) \in \{\text{veg}, \text{vegan}\} \\ 0 & \text{otherwise} \end{cases}
$$

$$
\mathbb{1}_{\text{protein}}(f) = \begin{cases} 1 & \text{if } \text{protein\_g}(f) \geq 8 \\ 0 & \text{otherwise} \end{cases}
$$

The effective filter condition for a food to pass is:

$$
\text{Pass}(f) = \bigl(\neg\, \texttt{veg\_only} \;\vee\; \mathbb{1}_{\text{veg}}(f)\bigr) \;\wedge\; \bigl(\neg\, \texttt{high\_protein} \;\vee\; \mathbb{1}_{\text{protein}}(f)\bigr)
$$

### 2.5 Ranking

Within each priority set, foods are ranked by descending dosha rating:

$$
\text{rank}(f_i) < \text{rank}(f_j) \iff r(f_i, d) > r(f_j, d)
$$

### 2.6 Result Selection

The final result list $R$ is determined by the following cascade:

$$
R = \begin{cases}
S_1^{\text{filtered}}[0 : \texttt{limit}] & \text{if } |S_1^{\text{filtered}}| > 0 \\
S_2^{\text{filtered}}[0 : 5] & \text{else if } |S_2^{\text{filtered}}| > 0 \\
S_{\text{all}}^{\text{filtered}}[0 : 5] & \text{otherwise (final fallback)}
\end{cases}
$$

---

## 3. How It Works

### 3.1 Initialization

When `AyurRecommender()` is instantiated, the constructor immediately creates a `FoodProcessor` object and calls `load_foods()`, caching the entire food dataset in `self.foods` as a list of dictionaries. This eager loading ensures that subsequent calls to `recommend()` are fast, as no I/O occurs during recommendation.

### 3.2 `recommend()` — Step-by-Step Execution Flow

```
INPUT: body_type, veg_only, high_protein, min_rating, limit
         │
         ▼
[1] Normalize & Validate body_type
    → Raises ValueError for invalid values
         │
         ▼
[2] Resolve Column Mapping
    → Maps dosha name to dataset column names
      (suitable_col, rating_col, reason_col)
         │
         ▼
[3] Partition Foods by Suitability + Rating
    → suitable_foods   : suitability ∈ {yes, suitable} AND rating ≥ min_rating
    → moderately_suitable_foods : suitability = moderately_suitable AND rating ≥ min_rating
         │
         ▼
[4] Apply Dietary Filters (inner function: apply_dietary_filters)
    → veg_only   : exclude non-veg / non-vegan
    → high_protein: exclude protein_g < 8
    → Applied independently to BOTH partitions
         │
         ▼
[5] Sort Both Partitions by dosha rating (descending)
         │
         ▼
[6] Build Results List
    → Primary:  top `limit` items from suitable_foods
    → Fallback 1: top 5 from moderately_suitable_foods (if primary empty)
    → Fallback 2: top 5 from all filtered foods (if fallback 1 also empty)
         │
         ▼
[7] Enrich Each Result Dict
    → Copies original food dict
    → Adds: dosha_rating, dosha_reason, recommendation_type
    → Fallbacks additionally add: fallback_message
         │
         ▼
OUTPUT: List[Dict] (max `limit` items)
```

### 3.3 `get_food_details()`

Delegates directly to `FoodProcessor.get_food_by_name()`. Returns a single food dictionary or `None`.

### 3.4 `get_categories()`

Iterates `self.foods`, collects unique non-null `category` values into a set, and returns a sorted list.

### 3.5 `get_nutrition_stats()`

Performs a single pass over the provided food list, accumulating sums for five macronutrient fields, then computes per-item averages. Both totals and averages are included in the returned dictionary, rounded to one decimal place.

---

## 4. Dependencies

| Dependency | Type | Version | Purpose |
|---|---|---|---|
| `typing` | Standard Library | Python 3.5+ | Type hints (`List`, `Dict`, `Optional`) |
| `sys` | Standard Library | Built-in | Runtime path manipulation |
| `pathlib.Path` | Standard Library | Python 3.4+ | Filesystem path resolution |
| `food_processor.FoodProcessor` | Internal Module | Project-specific | Food data loading and lookup |

> **Note:** `FoodProcessor` is loaded dynamically by appending the parent's `data/` directory to `sys.path` at import time. It is not a pip-installable package and must exist at `<project_root>/data/food_processor.py`.

---

## 5. Input Arguments

### 5.1 `AyurRecommender.recommend()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `body_type` | `str` | ✅ Required | — | Ayurvedic dosha / body constitution type | Must be one of: `"vata"`, `"pitta"`, `"kapha"` (case-insensitive). Raises `ValueError` otherwise. |
| `veg_only` | `bool` | ❌ Optional | `True` | When `True`, excludes all foods whose `food_type` is not `"veg"` or `"vegan"` | — |
| `high_protein` | `bool` | ❌ Optional | `False` | When `True`, excludes foods with `protein_g < 8` | — |
| `min_rating` | `int` | ❌ Optional | `4` | Minimum dosha-specific suitability rating a food must have to be considered | Integer in range `[1, 5]`. Values outside this range are not explicitly validated but will behave correctly given the data's rating scale. |
| `limit` | `int` | ❌ Optional | `12` | Maximum number of food items to return in the result list | Positive integer. Applied as a hard cap on the primary suitable set; fallback sets are capped at `min(5, available)`. |

### 5.2 `AyurRecommender.get_food_details()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `food_name` | `str` | ✅ Required | — | The name of the food to look up | Must match the dataset's naming convention; behavior for partial matches is determined by `FoodProcessor`. |

### 5.3 `AyurRecommender.get_nutrition_stats()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `foods_list` | `List[Dict]` | ✅ Required | — | A list of food dictionaries, typically the output of `recommend()` | If an empty list is provided, the function returns `{}` immediately. |

### 5.4 `get_recommendations()` (Convenience Function)

Accepts the same parameters as `AyurRecommender.recommend()`. Refer to Section 5.1.

---

## 6. Output Format

### 6.1 `recommend()` → `List[Dict]`

Returns a list of food dictionaries. Each dictionary contains all original fields from the food dataset, plus the following **injected fields**:

| Field | Type | Present When | Description |
|---|---|---|---|
| `dosha_rating` | `int` or `float` | Always | The numerical suitability rating for the requested dosha (1–5). |
| `dosha_reason` | `str` | Always | Human-readable explanation of why this food suits (or is appropriate for) the given dosha. |
| `recommendation_type` | `str` | Always | One of `"highly_suitable"`, `"moderately_suitable"`, or `"general_fallback"`. Indicates which tier of the selection cascade produced this result. |
| `fallback_message` | `str` | Fallback tiers only | Explanatory message present only for `moderately_suitable` and `general_fallback` results, informing the caller that ideal matches were unavailable. |

**Key original food fields expected in each dictionary (from the dataset):**

| Field | Type | Description |
|---|---|---|
| `food_name` | `str` | Display name of the food |
| `food_type` | `str` | Dietary category: `"veg"`, `"vegan"`, or non-vegetarian type |
| `category` | `str` | Food group / category (e.g., `"Legumes"`, `"Dairy"`) |
| `protein_g` | `float` | Protein content in grams |
| `carbs_g` | `float` | Carbohydrate content in grams |
| `fat_g` | `float` | Fat content in grams |
| `fiber_g` | `float` | Dietary fiber content in grams |
| `calories_kcal` | `float` | Caloric value in kilocalories |

### 6.2 `get_food_details()` → `Optional[Dict]`

Returns a single food dictionary (same schema as dataset entries) or `None` if the food is not found.

### 6.3 `get_categories()` → `List[str]`

Returns a sorted list of unique category name strings. Example: `["Dairy", "Fruits", "Grains", "Legumes", "Vegetables"]`.

### 6.4 `get_nutrition_stats()` → `Dict`

| Key | Type | Description |
|---|---|---|
| `total_foods` | `int` | Number of food items in the input list |
| `avg_protein_g` | `float` | Mean protein per food item (grams), rounded to 1 decimal |
| `avg_carbs_g` | `float` | Mean carbohydrates per food item (grams), rounded to 1 decimal |
| `avg_fat_g` | `float` | Mean fat per food item (grams), rounded to 1 decimal |
| `avg_fiber_g` | `float` | Mean dietary fiber per food item (grams), rounded to 1 decimal |
| `avg_calories_kcal` | `float` | Mean calories per food item (kcal), rounded to 1 decimal |
| `total_protein_g` | `float` | Sum of protein across all foods (grams), rounded to 1 decimal |
| `total_carbs_g` | `float` | Sum of carbohydrates across all foods (grams), rounded to 1 decimal |
| `total_fat_g` | `float` | Sum of fat across all foods (grams), rounded to 1 decimal |
| `total_fiber_g` | `float` | Sum of dietary fiber across all foods (grams), rounded to 1 decimal |
| `total_calories_kcal` | `float` | Sum of calories across all foods (kcal), rounded to 1 decimal |

Returns `{}` (empty dict) if `foods_list` is empty.

---

## 7. Example

### 7.1 Basic Recommendation for Vata Body Type

```python
from recommender import AyurRecommender

recommender = AyurRecommender()

results = recommender.recommend(
    body_type="vata",
    veg_only=True,
    high_protein=False,
    min_rating=4,
    limit=3
)

for food in results:
    print(f"Food: {food['food_name']}")
    print(f"  Rating     : {food['dosha_rating']}/5")
    print(f"  Reason     : {food['dosha_reason']}")
    print(f"  Type       : {food['recommendation_type']}")
    print(f"  Category   : {food.get('category', 'N/A')}")
    print(f"  Protein    : {food.get('protein_g', 'N/A')}g")
    print()
```

**Expected Output:**

```
Food: Sesame Seeds
  Rating     : 5/5
  Reason     : Warming, nourishing, and grounding — ideal for balancing dry, light Vata
  Type       : highly_suitable
  Category   : Seeds & Nuts
  Protein    : 17.7g

Food: Ghee
  Rating     : 5/5
  Reason     : Deeply nourishing, lubricates joints and tissues, pacifies Vata
  Type       : highly_suitable
  Category   : Dairy
  Protein    : 0.0g

Food: Moong Dal
  Rating     : 4/5
  Reason     : Easy to digest and warming when cooked well, suitable for Vata
  Type       : highly_suitable
  Category   : Legumes
  Protein    : 24.0g
```

---

### 7.2 High-Protein Recommendation for Pitta Type

```python
results = recommender.recommend(
    body_type="pitta",
    veg_only=True,
    high_protein=True,   # Only foods with protein_g >= 8
    min_rating=3,
    limit=5
)

print(f"Found {len(results)} high-protein recommendations for Pitta type.")
```

---

### 7.3 Using the Convenience Function

```python
from recommender import get_recommendations

recommendations = get_recommendations(
    body_type="kapha",
    veg_only=False,
    high_protein=True,
    min_rating=4,
    limit=10
)
```

---

### 7.4 Nutrition Statistics for a Recommendation Set

```python
recommender = AyurRecommender()

foods = recommender.recommend(body_type="pitta", limit=12)
stats = recommender.get_nutrition_stats(foods)

print(f"Avg Protein : {stats['avg_protein_g']}g")
print(f"Avg Calories: {stats['avg_calories_kcal']} kcal")
print(f"Total Foods : {stats['total_foods']}")
```

**Expected Output:**

```
Avg Protein : 9.3g
Avg Calories: 187.4 kcal
Total Foods : 12
```

---

## 8. Pros / Advantages

**Clear Separation of Concerns**
Data access is fully encapsulated within `FoodProcessor`. The `AyurRecommender` class is purely concerned with filtering logic and result enrichment, making both components independently testable and replaceable.

**Robust Three-Tier Fallback Design**
The cascade from `highly_suitable` → `moderately_suitable` → `general_fallback` ensures the system never returns an empty result set (assuming the food database is non-empty). This is critical for user-facing applications where a blank recommendation page would be a poor experience.

**Transparent Result Typing**
The `recommendation_type` and optional `fallback_message` fields give downstream consumers full visibility into the quality and confidence level of each recommendation — enabling UI layers to display appropriate caveats without additional logic.

**Immutable Source Data**
Each result is created via `food.copy()`, so the cached `self.foods` list is never mutated during recommendation. This is essential for correctness across multiple `recommend()` calls in the same session.

**Extensible Column Mapping**
The `body_type_mapping` dictionary makes it straightforward to add new doshas or rename dataset columns without touching the filtering logic.

**Dietary Filter Composability**
The `apply_dietary_filters` inner function is applied uniformly to all food sets (suitable, moderately suitable, and fallback), guaranteeing that dietary constraints are always honoured regardless of which fallback tier is activated.

**Zero External Runtime Dependencies**
The module relies exclusively on Python's standard library and a single internal module, making deployment lightweight and environment-agnostic.

---

## 9. Edge Cases & Limitations

### Edge Cases Handled

| Scenario | Behaviour |
|---|---|
| Invalid `body_type` value | Raises `ValueError` with a descriptive message listing valid options. |
| Empty `foods_list` passed to `get_nutrition_stats()` | Returns `{}` immediately without raising a `ZeroDivisionError`. |
| No foods meet primary suitability criteria | Automatically falls back to moderately suitable foods (up to 5). |
| No foods meet any suitability criteria | Falls back to top-rated foods from the entire filtered dataset (up to 5), ignoring suitability labels. |
| Missing nutritional fields in food dict | `food.get("protein_g", 0)` and similar calls default missing values to `0`, preventing `KeyError` exceptions. |
| `body_type` passed in uppercase or mixed case | Normalized via `.lower()` before processing. |

### Known Limitations

**No Pagination Support**
The `limit` parameter caps the total number of results, but there is no offset or cursor mechanism for fetching the next page of recommendations. This constrains use in paginated API contexts.

**Fallback Cap is Hardcoded**
The fallback tiers are limited to 5 items regardless of the caller-supplied `limit`. If a caller requests `limit=20` and only moderately suitable foods exist, they will receive at most 5 results.

**No Validation on `min_rating` Range**
Passing `min_rating=99` would silently return no results from the suitability filter rather than raising an error. An explicit range check (`1 ≤ min_rating ≤ 5`) is absent.

**Protein Threshold is Hardcoded**
The high-protein cutoff of `8g` is not configurable. Different use cases (e.g., athlete vs. sedentary diet) might require different thresholds.

**Suitability Labels Depend on Dataset Consistency**
The filter logic checks for exact string matches (`"yes"`, `"suitable"`, `"moderately_suitable"`). Any typos or alternative labels in the dataset would silently exclude those foods.

**No Multi-Dosha Support**
Users with dual or tri-doshic constitutions (e.g., Vata-Pitta) cannot run a combined recommendation in a single call.

**Eager Loading**
The full food dataset is loaded into memory at instantiation. For very large datasets, this could be a memory concern, though it is appropriate for typical food database sizes.

**No Caching of `get_recommendations()` Convenience Function**
The convenience function instantiates a new `AyurRecommender` (and reloads the dataset) on every invocation. In high-throughput scenarios, callers should prefer reusing a single `AyurRecommender` instance.

---

## 10. Future Improvements

**Configurable Protein Threshold**
Expose `high_protein_threshold_g: float = 8.0` as a parameter to `recommend()` to accommodate varied dietary needs without hardcoding the cutoff.

**Configurable Fallback Limits**
Allow the fallback result cap (currently hardcoded to `5`) to be driven by the `limit` parameter for consistent behaviour across all tiers.

**Input Validation for `min_rating`**
Add an explicit guard:
```python
if not (1 <= min_rating <= 5):
    raise ValueError("min_rating must be between 1 and 5.")
```

**Multi-Dosha Recommendation**
Accept a list of doshas and compute an intersection-based or weighted score across multiple constitutions to support dual/tri-doshic users.

**Pagination Support**
Add `offset: int = 0` to `recommend()` to enable cursor-based pagination for API consumers.

**Scoring Function over Hard Filtering**
Replace binary suitability partitioning with a continuous composite score incorporating dosha rating, protein content, category diversity, and other nutritional factors. This would produce a ranked list rather than a filtered set, and naturally handle the moderately suitable tier.

**Lazy / On-Demand Data Loading**
Replace eager loading in `__init__` with lazy loading so that the dataset is only fetched when `recommend()` is first called. This would reduce startup overhead if the recommender is instantiated but not used.

**Dataset Label Normalisation**
Centralise suitability label normalisation (e.g., strip whitespace, lowercase, map synonyms) inside `FoodProcessor` to decouple `AyurRecommender` from the raw string values present in the dataset.

**Result Caching**
Implement LRU caching on `recommend()` results keyed on `(body_type, veg_only, high_protein, min_rating, limit)` to serve repeated identical queries without reprocessing the dataset.

**Unit Test Suite**
Introduce a comprehensive test suite covering: valid/invalid `body_type` values, empty database scenarios, each fallback tier activation, dietary filter combinations, and nutrition statistics accuracy.

---

*Documentation generated for `recommender.py` — Ayurvedic Food Recommendation Engine.*
