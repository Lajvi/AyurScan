# Technical Documentation: Data Processing Layer

> **Project:** Ayurvedic Recommendation System
> **Modules:** `food_processor.py` · `medicine_processor.py`
> **Classes:** `FoodProcessor` · `MedicineProcessor`
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

`food_processor.py` and `medicine_processor.py` form the **Data Processing Layer** of the Ayurvedic Recommendation System. They serve as the foundational data pipeline beneath the higher-level recommendation engines (`recommender.py` and `medicine_recommender.py`), handling all raw data ingestion, transformation, normalisation, persistence, and structured retrieval.

Both modules follow a near-identical architectural pattern — referred to throughout this document as the **Processor Pattern** — and are therefore documented together for clarity and to highlight both their symmetry and their differences.

### Module Responsibilities

| Concern | `FoodProcessor` | `MedicineProcessor` |
|---|---|---|
| Raw data source | CSV file | Excel file (`.xlsx`, Sheet1) |
| Cleaned cache format | `cleaned_foods.json` | `cleaned_medicines.json` |
| Unique key | `food_name` | `medicine_name` |
| Pivot / reshape step | Yes — long-to-wide per dosha | No — one row per medicine already |
| Column normalisation | Minimal | Extensive (multi-format mapping) |
| Numeric cleaning | Via pandas averaging | Via `pd.to_numeric(..., errors='coerce')` |

### Role in the Project Architecture

```
Raw Data Sources
     │
     ├── food_recommendation_dataset.csv  ──►  FoodProcessor  ──►  cleaned_foods.json
     │                                                                      │
     └── Ayurvedic_Medicine_Dataset.xlsx  ──►  MedicineProcessor ──► cleaned_medicines.json
                                                                            │
                                                          ┌─────────────────┘
                                                          ▼
                                              AyurRecommender / AyurMedicineRecommender
                                                          │
                                                          ▼
                                                   API / Frontend
```

The processors act as a **write-once, read-many cache layer**: raw data is processed and persisted to JSON on first run; all subsequent reads bypass the expensive CSV/Excel parsing and load directly from the JSON cache.

---

## 2. Mathematics Behind It

### 2.1 Nutritional Averaging in `FoodProcessor`

The raw food CSV may contain multiple rows for the same food item (one per dosha body type). When consolidating these into a single record, the five numerical nutritional fields are averaged across all rows belonging to that food:

$$
\bar{x}_{f, \,\text{nutrient}} = \frac{1}{N_f} \sum_{i=1}^{N_f} x_{f,i,\,\text{nutrient}}
$$

where $N_f$ is the number of rows for food $f$ and $x_{f,i,\,\text{nutrient}}$ is the nutritional value for that food in the $i$-th row. This is applied to:

$$
\text{nutrients} \in \{\text{protein\_g},\; \text{carbs\_g},\; \text{fat\_g},\; \text{fiber\_g},\; \text{calories\_kcal}\}
$$

### 2.2 Long-to-Wide Pivot (`FoodProcessor`)

The raw CSV is in **long format** — multiple rows per food, one per body type. `process_data()` performs a manual **pivot transformation** to produce a single wide-format record per food:

$$
\text{Wide}(f) = \text{Base}(f) \;\cup\; \bigoplus_{d \;\in\; \{\text{Vaat},\, \text{Pit},\, \text{Cough (Kapha)}\}} \text{DoshaFields}(f, d)
$$

where $\oplus$ denotes field union and $\text{Base}(f)$ contains the food-level fields (type, category, nutritional averages, vitamins, minerals, health tags).

### 2.3 Dosha Score Coercion (`MedicineProcessor`)

Dosha scores from the Excel source are coerced to numeric type using:

$$
\text{score}(m, d) = \begin{cases} \text{numeric value} & \text{if parseable} \\ \text{NaN} & \text{otherwise (non-numeric or missing)} \end{cases}
$$

This is applied via `pd.to_numeric(series, errors='coerce')` for each of `vata_score`, `pitta_score`, and `kapha_score`.

---

## 3. How It Works

### 3.1 The Processor Pattern (Shared Architecture)

Both processors implement identical lifecycle logic:

```
FIRST RUN (no JSON cache):
  load_raw_data()          ← Read CSV / Excel from disk
       │
       ▼
  process_data()           ← Transform, pivot/normalise, clean
       │
       ▼
  save_cleaned_data()      ← Serialise to JSON cache
       │
       ▼
  load_foods() /
  load_medicines()         ← Read from JSON → return List[Dict]

SUBSEQUENT RUNS (JSON cache exists):
  load_foods() /
  load_medicines()         ← Read directly from JSON → return List[Dict]
```

The `load_foods()` / `load_medicines()` methods act as the **primary entry point** for all consuming code. They transparently trigger the full processing pipeline on cache miss and return fast-loaded JSON data on cache hit.

---

### 3.2 `FoodProcessor` — Internal Logic

#### `load_raw_data()`
Opens the CSV file at the resolved absolute path (`Path(__file__).parent / csv_path`). Raises `FileNotFoundError` with a descriptive path message on failure.

#### `process_data()`
Performs the long-to-wide transformation in four stages:

```
[1] Load raw CSV (N rows, multiple rows per food)
         │
         ▼
[2] For each unique food_name:
    ├─ Compute average nutritional values across all rows
    ├─ Take first-occurrence values for categorical fields
    │  (food_type, category, vitamins, minerals, health_tags)
    └─ Pivot dosha-specific columns:
         body_type == "Vaat"           → vaat_suitable, vaat_rating, suitability_reason_vaat
         body_type == "Pit"            → pit_suitable, pit_rating, suitability_reason_pit
         body_type == "Cough (Kapha)"  → kapha_suitable, kapha_rating, suitability_reason_kapha
         │
         ▼
[3] Assemble into DataFrame; drop_duplicates on food_name (keep first)
         │
         ▼
[4] Ensure all 19 required columns exist (fill missing with None)
    → Store result in self.cleaned_data
```

**Raw Column Mapping (Dosha Pivot):**

| Raw `body_type` value | Raw suitability column | Mapped output field |
|---|---|---|
| `"Vaat"` | `vaat_suitable` | `vaat_suitable` |
| `"Vaat"` | `vaat_rating` | `vaat_rating` |
| `"Pit"` | `pit_suitable` | `pit_suitable` |
| `"Pit"` | `pit_rating` | `pit_rating` |
| `"Cough (Kapha)"` | `cough_suitable` | `kapha_suitable` |
| `"Cough (Kapha)"` | `cough_rating` | `kapha_rating` |

#### `save_cleaned_data()`
Converts `self.cleaned_data` (a DataFrame) to a list of dictionaries via `to_dict('records')` and serialises to JSON with `indent=2` and `ensure_ascii=False` (preserving Unicode characters in food names or descriptions).

#### `load_foods()`
Attempts to read `cleaned_foods.json`. On `FileNotFoundError`, transparently calls `process_data()` → `save_cleaned_data()` → `load_foods()` (recursive self-call). Returns `List[Dict]`.

---

### 3.3 `MedicineProcessor` — Internal Logic

#### `load_raw_data()`
Opens the Excel file using `pd.read_excel(..., sheet_name="Sheet1")`. Raises `FileNotFoundError` for missing files and a generic `Exception` (with message) for other Excel read errors.

#### `process_data()`
Unlike `FoodProcessor`, the raw Excel data is already in **wide format** (one row per medicine). Processing focuses on standardisation:

```
[1] Load raw Excel (one row per medicine)
         │
         ▼
[2] Generate medicine_id column (1-based sequential integer)
    if not already present in source data
         │
         ▼
[3] Rename columns via 42-entry mapping dictionary
    → Handles 3 naming conventions per field:
      "Medicine Name" | "Medicine_Name" | "medicine_name" → "medicine_name"
         │
         ▼
[4] Ensure all 15 required columns exist (fill missing with None)
         │
         ▼
[5] _clean_medicine_data():
    ├─ Coerce vata_score, pitta_score, kapha_score → numeric (NaN on failure)
    └─ Strip whitespace from all text columns; replace 'nan' strings with None
         │
         ▼
[6] drop_duplicates on medicine_name (keep first); reset_index
    → Store result in self.cleaned_data
```

#### `_clean_medicine_data()`
Applies two cleaning passes:
- **Numeric columns:** `pd.to_numeric(..., errors='coerce')` silently converts unparseable values to `NaN`.
- **Text columns:** `.astype(str).str.strip()` followed by `.replace('nan', None)` — the latter is necessary because pandas serialises missing values as the string `'nan'` during `astype(str)`.

---

### 3.4 Query Methods (Both Processors)

Both classes expose a symmetric set of read methods that always call `load_foods()` / `load_medicines()` internally, triggering cache-loading on each invocation:

| Method | `FoodProcessor` | `MedicineProcessor` |
|---|---|---|
| Load all as list | `load_foods()` | `load_medicines()` |
| Load as DataFrame | `get_foods_dataframe()` | `get_medicines_dataframe()` |
| Lookup by name | `get_food_by_name(name)` | `get_medicine_by_name(name)` |
| Filter by category/type | `get_foods_by_category(cat)` | `get_medicines_by_formulation(type)` |
| Filter by body system | — | `get_medicines_by_body_system(sys)` |
| Filter by dosha | `get_suitable_foods(dosha)` | `get_medicines_by_dosha(dosha)` |
| List available types | — | `get_formulation_types()` |
| List body systems | — | `get_body_systems()` |

---

## 4. Dependencies

### `FoodProcessor`

| Dependency | Type | Version | Purpose |
|---|---|---|---|
| `pandas` | Third-party | ≥ 1.3.0 recommended | CSV loading (`read_csv`), DataFrame operations, averaging, deduplication |
| `json` | Standard Library | Built-in | JSON serialisation and deserialisation of cleaned data |
| `pathlib.Path` | Standard Library | Python 3.4+ | Absolute path resolution relative to the module file |
| `typing` | Standard Library | Python 3.5+ | Type hints (`List`, `Dict`, `Union`) |

### `MedicineProcessor`

| Dependency | Type | Version | Purpose |
|---|---|---|---|
| `pandas` | Third-party | ≥ 1.3.0 recommended | Excel loading (`read_excel`), DataFrame operations, type coercion, deduplication |
| `openpyxl` | Third-party (implicit) | ≥ 3.0.0 | Required by `pandas.read_excel()` to parse `.xlsx` files; must be installed separately |
| `json` | Standard Library | Built-in | JSON serialisation and deserialisation of cleaned data |
| `pathlib.Path` | Standard Library | Python 3.4+ | Absolute path resolution relative to the module file |
| `typing` | Standard Library | Python 3.5+ | Type hints (`List`, `Dict`, `Union`) |

> **Note:** `Union` is imported in both modules but not referenced in any function signature in the current implementation.

> **Note:** `openpyxl` is not explicitly imported in `medicine_processor.py` but is a required runtime dependency for `pd.read_excel()` to handle `.xlsx` files. It must be present in the environment: `pip install openpyxl`.

---

## 5. Input Arguments

### 5.1 `FoodProcessor.__init__()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `csv_path` | `str` | ❌ Optional | `"food_recommendation_dataset.csv"` | Filename (not full path) of the raw food CSV, resolved relative to the module's directory. | File must exist at `<module_dir>/<csv_path>` for `process_data()` to succeed. |

### 5.2 `MedicineProcessor.__init__()`

| Parameter | Type | Required | Default | Description | Constraints |
|---|---|---|---|---|---|
| `excel_path` | `str` | ❌ Optional | `"Ayurvedic_Medicine_Dataset.xlsx"` | Filename of the raw Excel file, resolved relative to the module's directory. | File must exist and contain a sheet named `"Sheet1"`. |

### 5.3 `get_food_by_name()` / `get_medicine_by_name()`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `food_name` / `medicine_name` | `str` | ✅ Required | — | Case-insensitive name lookup. Compared using `.lower()` on both sides. |

### 5.4 `get_foods_by_category()` / `get_medicines_by_formulation()` / `get_medicines_by_body_system()`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `category` / `formulation_type` / `body_system` | `str` | ✅ Required | — | Case-insensitive string match against the corresponding field in each record. |

### 5.5 `get_suitable_foods()` / `get_medicines_by_dosha()`

| Parameter | Type | Required | Default | Description | Notes |
|---|---|---|---|---|---|
| `dosha` | `str` | ✅ Required | — | Dosha name for filtering. | `FoodProcessor` checks against specific field keys (`vaat_suitable`, `pit_suitable`, `kapha_suitable`). `MedicineProcessor` checks via substring match on `dosha_label`. |

---

## 6. Output Format

### 6.1 `load_foods()` → `List[Dict]`

Each dictionary represents one unique food item with the following fields:

**Base fields (food-level):**

| Field | Type | Description |
|---|---|---|
| `food_name` | `str` | Unique food identifier / display name |
| `food_type` | `str` | Dietary classification: `"veg"`, `"vegan"`, or non-vegetarian type |
| `category` | `str` | Food group (e.g., `"Legumes"`, `"Dairy"`, `"Vegetables"`) |
| `protein_g` | `float` | Average protein content in grams |
| `carbs_g` | `float` | Average carbohydrate content in grams |
| `fat_g` | `float` | Average fat content in grams |
| `fiber_g` | `float` | Average dietary fiber in grams |
| `calories_kcal` | `float` | Average caloric value in kcal |
| `vitamins` | `str` | Vitamin profile (from first row of the food's entries) |
| `minerals` | `str` | Mineral profile (from first row) |
| `health_tags` | `str` | Comma-separated health benefit tags (from first row) |

**Dosha-specific fields (pivoted):**

| Field | Type | Description |
|---|---|---|
| `vaat_suitable` | `str` or `None` | Suitability for Vata dosha (e.g., `"Yes"`, `"No"`) |
| `vaat_rating` | `int` or `None` | Suitability rating for Vata (1–5) |
| `suitability_reason_vaat` | `str` or `None` | Explanation of Vata suitability |
| `pit_suitable` | `str` or `None` | Suitability for Pitta dosha |
| `pit_rating` | `int` or `None` | Suitability rating for Pitta (1–5) |
| `suitability_reason_pit` | `str` or `None` | Explanation of Pitta suitability |
| `kapha_suitable` | `str` or `None` | Suitability for Kapha dosha |
| `kapha_rating` | `int` or `None` | Suitability rating for Kapha (1–5) |
| `suitability_reason_kapha` | `str` or `None` | Explanation of Kapha suitability |

> Fields are `None` if the source CSV did not contain a row for that dosha for the given food.

---

### 6.2 `load_medicines()` → `List[Dict]`

Each dictionary represents one unique medicine record:

| Field | Type | Description |
|---|---|---|
| `medicine_id` | `int` | Sequential identifier (1-based); generated if not present in source |
| `medicine_name` | `str` | Unique medicine display name |
| `formulation_type` | `str` | Physical form (e.g., `"Churna"`, `"Kwath"`, `"Vati"`, `"Ghrita"`) |
| `rasa_taste` | `str` | Ayurvedic taste (e.g., `"Bitter"`, `"Sweet"`, `"Pungent"`) |
| `virya_potency` | `str` | Thermal potency (`"Hot"` or `"Cold"`) |
| `vipaka` | `str` | Post-digestive effect |
| `guna_quality` | `str` | Physical/energetic qualities (e.g., `"Light, Dry"`) |
| `vata_score` | `float` or `NaN` | Numeric dosha affinity score for Vata; `NaN` if source value was non-numeric |
| `pitta_score` | `float` or `NaN` | Numeric dosha affinity score for Pitta |
| `kapha_score` | `float` or `NaN` | Numeric dosha affinity score for Kapha |
| `body_system` | `str` | Target body system (e.g., `"Digestive"`, `"Nervous"`, `"Respiratory"`) |
| `therapeutic_uses` | `str` | Free-text description of health conditions treated |
| `dosage` | `str` | Recommended dosage instructions |
| `anupana` | `str` | Vehicle/co-administration substance |
| `dosha_label` | `str` | Classification label (e.g., `"Vata"`, `"Pitta"`, `"Tridosha"`) |

---

### 6.3 Other Query Method Return Types

| Method | Return Type | Description |
|---|---|---|
| `get_food_by_name()` | `Dict` or `None` | Single food dict or `None` if not found |
| `get_medicine_by_name()` | `Dict` or `None` | Single medicine dict or `None` if not found |
| `get_foods_dataframe()` | `pd.DataFrame` | All foods as a pandas DataFrame |
| `get_medicines_dataframe()` | `pd.DataFrame` | All medicines as a pandas DataFrame |
| `get_foods_by_category()` | `List[Dict]` | Filtered food list; empty list if no matches |
| `get_medicines_by_formulation()` | `List[Dict]` | Filtered medicine list; empty list if no matches |
| `get_medicines_by_body_system()` | `List[Dict]` | Filtered medicine list; empty list if no matches |
| `get_suitable_foods()` | `List[Dict]` | Foods marked suitable for given dosha |
| `get_medicines_by_dosha()` | `List[Dict]` | Medicines whose `dosha_label` contains the given dosha string |
| `get_formulation_types()` | `List[str]` | Sorted list of unique formulation type strings |
| `get_body_systems()` | `List[str]` | Sorted list of unique body system strings |

---

## 7. Example

### 7.1 First-Run Processing and Cache Creation

```python
from food_processor import FoodProcessor

processor = FoodProcessor()

# Process raw CSV and save JSON cache
processor.process_data()
processor.save_cleaned_data()
# Output:
# Loaded 540 rows from /project/data/food_recommendation_dataset.csv
# Processed 90 unique food items
# Saved cleaned data to /project/data/cleaned_foods.json
```

---

### 7.2 Loading Foods (Cache Hit)

```python
processor = FoodProcessor()
foods = processor.load_foods()
# Output: Loaded 90 food items from /project/data/cleaned_foods.json

print(f"Total foods: {len(foods)}")
print(f"First food: {foods[0]['food_name']}")
print(f"Protein: {foods[0]['protein_g']}g")
print(f"Vata suitable: {foods[0]['vaat_suitable']}")
print(f"Vata reason: {foods[0]['suitability_reason_vaat']}")
```

**Expected Output:**

```
Total foods: 90
First food: Moong Dal
Protein: 24.0g
Vata suitable: Yes
Vata reason: Easy to digest and warming when cooked well, suitable for Vata
```

---

### 7.3 Loading Medicines and Querying by Dosha

```python
from medicine_processor import MedicineProcessor

processor = MedicineProcessor()
medicines = processor.load_medicines()
# Output: Loaded 120 medicine items from /project/data/cleaned_medicines.json

# Get all Vata medicines
vata_medicines = processor.get_medicines_by_dosha("vata")
print(f"Vata medicines: {len(vata_medicines)}")

# Get details for a specific medicine
ashwagandha = processor.get_medicine_by_name("Ashwagandha Churna")
if ashwagandha:
    print(f"Name       : {ashwagandha['medicine_name']}")
    print(f"Formulation: {ashwagandha['formulation_type']}")
    print(f"Vata Score : {ashwagandha['vata_score']}")
    print(f"Dosage     : {ashwagandha['dosage']}")
    print(f"Anupana    : {ashwagandha['anupana']}")
```

**Expected Output:**

```
Vata medicines: 38
Name       : Ashwagandha Churna
Formulation: Churna
Vata Score : 9.0
Dosage     : 3-6g with warm milk before bed
Anupana    : Warm milk or ghee
```

---

### 7.4 Custom CSV Path (FoodProcessor)

```python
# Load from a non-default CSV file
processor = FoodProcessor(csv_path="extended_food_dataset_v2.csv")
processor.process_data()
processor.save_cleaned_data()
```

---

### 7.5 Using Convenience Functions

```python
from food_processor import load_foods
from medicine_processor import load_medicines

foods = load_foods()         # Triggers full pipeline if JSON missing
medicines = load_medicines() # Same behaviour

print(f"Foods: {len(foods)}, Medicines: {len(medicines)}")
```

---

### 7.6 DataFrame Access for Analysis

```python
processor = MedicineProcessor()
df = processor.get_medicines_dataframe()

# Use pandas for advanced filtering
high_vata = df[df['vata_score'] >= 8]
print(f"High Vata-affinity medicines: {len(high_vata)}")
print(high_vata[['medicine_name', 'vata_score', 'formulation_type']].head())
```

---

## 8. Pros / Advantages

**Write-Once JSON Cache Architecture**
Both processors implement a transparent lazy caching strategy: raw files are processed only once, and all subsequent reads serve from the fast JSON cache. This eliminates repeated CSV/Excel parsing overhead in every application request, with no changes required in consuming code.

**Automatic Cache Bootstrap**
The `load_foods()` / `load_medicines()` methods handle cache miss scenarios entirely internally through a self-invoking fallback chain. Consuming code — such as `AyurRecommender.__init__()` — requires no awareness of whether the cache exists, making integration seamless.

**Long-to-Wide Pivot Without External Dependencies**
`FoodProcessor` implements the dosha pivot transformation using plain Python iteration over a grouped DataFrame, avoiding the complexity of `pandas.pivot_table()` while maintaining full control over field naming and the handling of missing body types per food.

**Resilient Column Name Normalisation (`MedicineProcessor`)**
The 42-entry column mapping dictionary in `MedicineProcessor.process_data()` handles three common naming conventions per field (space-separated, underscore-separated, lowercase), making the processor robust against minor variations in the source Excel file's column headers without requiring data file changes.

**Schema Enforcement with Safe Defaults**
Both processors explicitly verify the presence of all required output columns after transformation and fill any missing columns with `None`. This guarantees a consistent schema for consuming code regardless of gaps in the source data.

**Immutable Source Data**
Both processors use `df.copy()` / `medicine.copy()` at key points to avoid `SettingWithCopyWarning` and prevent unintended mutation of source DataFrames.

**UTF-8 / Unicode Safe JSON Serialisation**
JSON files are written with `ensure_ascii=False`, correctly preserving non-ASCII characters in food names, Ayurvedic terminology, and multilingual content.

**Dual Access Modes**
Both processors offer `List[Dict]` output (for the recommendation engines) and `pd.DataFrame` output (for data analysis, exploration, and debugging), serving different consumer needs without data reloading.

**Module-Relative Path Resolution**
Using `Path(__file__).parent / filename` ensures the processors locate data files correctly regardless of the working directory from which the application is launched, avoiding brittle relative path assumptions.

---

## 9. Edge Cases & Limitations

### Edge Cases Handled

| Scenario | Module | Behaviour |
|---|---|---|
| JSON cache missing on first run | Both | Transparently triggers `process_data()` → `save_cleaned_data()` → re-loads from JSON |
| Food with only one or two dosha rows in CSV | `FoodProcessor` | Missing dosha fields are left as `None` after schema enforcement |
| Duplicate food/medicine names in source data | Both | `drop_duplicates(..., keep='first')` retains the first occurrence and silently discards the rest |
| Non-numeric values in dosha score columns | `MedicineProcessor` | `pd.to_numeric(..., errors='coerce')` converts them to `NaN` silently |
| `'nan'` string from pandas `astype(str)` conversion | `MedicineProcessor` | Explicitly replaced with `None` in `_clean_medicine_data()` |
| Source CSV file not found | `FoodProcessor` | Raises `FileNotFoundError` with the resolved absolute path |
| Source Excel file not found | `MedicineProcessor` | Raises `FileNotFoundError` with the resolved absolute path |
| Other Excel read errors | `MedicineProcessor` | Raises a generic `Exception` with the error message |
| Case-insensitive name lookup | Both | `.lower()` comparison in `get_food_by_name()` / `get_medicine_by_name()` |

### Known Limitations

**No Incremental Update Support**
There is no mechanism to add, update, or remove individual records from the JSON cache without reprocessing the entire source file. Any change to the raw data requires a full re-run of `process_data()` and `save_cleaned_data()`.

**Categorical Field Takes First-Row Value**
In `FoodProcessor`, categorical fields (`food_type`, `category`, `vitamins`, `minerals`, `health_tags`) are populated from the first row of each food's entries using `.iloc[0]`. If these fields differ across dosha rows for the same food, the discrepancy is silently ignored.

**Silent Duplicate Discarding**
Both processors drop duplicate records without logging which entries were removed. In a dataset with legitimate near-duplicate entries (e.g., same food in different preparations), this can result in silent data loss.

**Hardcoded Sheet Name in `MedicineProcessor`**
The Excel sheet name is hardcoded as `"Sheet1"` in `load_raw_data()`. Excel files with differently named sheets will fail without a code change. There is no parameter to override this.

**No Partial Column Mapping Warnings**
If the source Excel file contains a column that does not match any of the 42 entries in the mapping dictionary, it is silently passed through with its original name rather than triggering a warning. This may introduce unexpected columns or leave expected columns unmapped.

**`Union` Import is Unused**
`Union` is imported from `typing` in both modules but is not used in any function signature. This is a minor code hygiene issue.

**Every Query Method Reloads from Disk**
Methods such as `get_food_by_name()`, `get_foods_by_category()`, and all equivalent medicine methods call `load_foods()` / `load_medicines()` on every invocation. While JSON loading is fast, this means each query re-reads the entire file rather than operating on an in-memory cache.

**`get_suitable_foods()` Uses Exact String Match**
The method checks `food.get('vaat_suitable') == 'Yes'` with exact case. Any variation in the source data (`'yes'`, `'YES'`, `'Y'`) would silently exclude matching foods.

**No Data Validation Post-Processing**
Beyond schema enforcement (column presence) and type coercion, neither processor validates semantic correctness — for example, that ratings fall within `[1, 5]`, that dosha scores are non-negative, or that required string fields are non-empty.

---

## 10. Future Improvements

**In-Memory Caching for Query Methods**
Cache the loaded data as an instance attribute after the first load, so repeated calls to `get_food_by_name()`, `get_foods_by_category()`, and equivalent medicine methods serve results from memory rather than re-reading the JSON file on each call:

```python
def load_foods(self) -> List[Dict]:
    if self._cache is not None:
        return self._cache
    # ... existing load logic ...
    self._cache = data
    return self._cache
```

**Configurable Excel Sheet Name**
Expose the Excel sheet name as a constructor parameter to support non-standard source files:

```python
def __init__(self, excel_path: str = "...", sheet_name: str = "Sheet1"):
    self.sheet_name = sheet_name
```

**Incremental Cache Update**
Add an `update_record()` method that can modify individual records in the JSON cache without requiring a full reprocessing cycle, using the unique key (`food_name` / `medicine_name`) as the identifier.

**Case-Insensitive Suitability Matching**
Normalise the suitability field check in `get_suitable_foods()` to handle mixed casing:

```python
if food.get('vaat_suitable', '').lower() == 'yes':
```

**Logging Instead of `print()`**
Replace all `print()` statements with Python's `logging` module to allow consuming applications to control log verbosity, redirect output, and integrate with centralised logging infrastructure:

```python
import logging
logger = logging.getLogger(__name__)
logger.info(f"Loaded {len(data)} food items from {self.json_path}")
```

**Semantic Data Validation Layer**
Add a post-processing validation step that checks:
- Rating values are in `[1, 5]`
- Dosha scores are non-negative
- Required string fields (`medicine_name`, `food_name`) are non-empty
- Numeric fields are not `NaN` in critical positions

**Duplicate Logging**
Log the names of duplicate records that are dropped during `drop_duplicates()`, enabling data quality auditing:

```python
duplicates = processed_df[processed_df.duplicated(subset=['food_name'], keep='first')]
if not duplicates.empty:
    logger.warning(f"Dropped {len(duplicates)} duplicates: {duplicates['food_name'].tolist()}")
```

**Remove Unused `Union` Import**
Clean up both modules:

```python
from typing import List, Dict  # Remove Union
```

**Configurable JSON Cache Path**
Allow the JSON cache path to be overridden via constructor parameter, enabling multi-environment deployments (development, staging, production) to use separate cache files:

```python
def __init__(self, csv_path: str = "...", json_cache_path: str = None):
    self.json_path = Path(json_cache_path) if json_cache_path else Path(__file__).parent / "cleaned_foods.json"
```

**Schema Version Tagging in JSON**
Embed a schema version number in the JSON cache file to detect stale caches when the processing logic changes, triggering automatic re-processing:

```json
{
  "schema_version": "1.2",
  "generated_at": "2026-04-01T10:00:00Z",
  "records": [...]
}
```

---

*Documentation generated for `food_processor.py` and `medicine_processor.py` — Ayurvedic Recommendation System Data Processing Layer.*
