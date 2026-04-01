# Ayurvedic Food & Medicine Recommender API

A FastAPI-based recommendation system that provides personalized food and medicine suggestions based on Ayurvedic body types (Vata, Pitta, Kapha).

## 🚀 Features

- **Personalized Food Recommendations**: Get food suggestions based on your Ayurvedic dosha
- **Intelligent Medicine Recommendations**: Get medicine suggestions based on body type and specific diseases
- **Dietary Filters**: Support for vegetarian, high-protein preferences
- **Disease Matching**: Advanced keyword matching for various health conditions
- **Fast Performance**: Optimized for <100ms response times
- **Comprehensive Data**: 105+ food items and medicine database with detailed information
- **RESTful API**: Clean, well-documented endpoints with Swagger UI
- **CORS Enabled**: Ready for React frontend integration

## 📋 Requirements

- Python 3.11+
- Dependencies listed in `requirements.txt`

## 🛠️ Installation

1. **Clone/Download the project** to your local machine

2. **Navigate to the project directory**:
   ```bash
   cd "d:\DATA SCIENCE AND ML\ayur-food-recommender"
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Process the data** (first time only):
   ```bash
   cd data
   python food_processor.py
   cd ..
   ```

## 🏃‍♂️ Running the API

### Method 1: Direct Python
```bash
python main.py
```

### Method 2: Uvicorn (Recommended)
```bash
uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

### Method 3: Uvicorn Module
```bash
python -m uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

The API will be available at:
- **Main API**: http://127.0.0.1:8000
- **Interactive Docs**: http://127.0.0.1:8000/docs
- **ReDoc**: http://127.0.0.1:8000/redoc

## 📚 API Endpoints

### 1. Get Recommendations
**GET** `/api/recommend`

Get personalized food recommendations based on body type and preferences.

**Query Parameters**:
- `body_type` (required): "vata", "pitta", or "kapha"
- `veg_only` (optional, default: true): Filter vegetarian foods
- `high_protein` (optional, default: false): Filter high-protein foods (≥8g)
- `min_rating` (optional, default: 4): Minimum suitability rating (1-5)
- `limit` (optional, default: 12): Maximum number of recommendations

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/recommend?body_type=vata&veg_only=true&min_rating=4&limit=5"
```

**Example Response**:
```json
{
  "body_type": "vata",
  "recommended_foods": [
    {
      "food_name": "Basmati Rice",
      "food_type": "Veg",
      "category": "Grains",
      "protein_g": 2.7,
      "carbs_g": 45.0,
      "fat_g": 0.3,
      "fiber_g": 0.4,
      "calories_kcal": 200.0,
      "vitamins": "B1,B3",
      "minerals": "Mg,P",
      "health_tags": "Light,Easily Digestible",
      "dosha_rating": 5,
      "dosha_reason": "Warm, oily, and grounding qualities pacify Vaat dosha.",
      "recommendation_type": "highly_suitable"
    }
  ],
  "total_found": 1,
  "message": "Highly suitable foods for Vata type",
  "filters_applied": {
    "veg_only": true,
    "high_protein": false,
    "min_rating": 4,
    "limit": 5
  }
}
```

### 2. Get All Foods
**GET** `/api/foods`

Get all available foods for debugging and exploration.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/foods"
```

### 3. Get Categories
**GET** `/api/categories`

Get all available food categories.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/categories"
```

### 4. Get Food Details
**GET** `/api/food/{food_name}`

Get detailed information about a specific food.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/food/Basmati%20Rice"
```

### 5. Health Check
**GET** `/health`

Check API health status.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/health"
```

## 🩺 Medicine Recommendations

### Get Medicine Recommendations
**GET** `/api/recommend/medicine`

Get personalized medicine recommendations based on body type and disease.

**Query Parameters**:
- `body_type` (required): "vata", "pitta", or "kapha"
- `disease` (required): Any disease/symptom like "insomnia", "acidity", "joint pain", "cough", "diabetes", "anxiety"
- `limit` (optional, default: 8): Maximum number of recommendations

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/recommend/medicine?body_type=vata&disease=insomnia"
```

**Example Response**:
```json
{
  "body_type": "vata",
  "disease": "insomnia",
  "recommended_medicines": [
    {
      "medicine_name": "Ashwagandha",
      "formulation_type": "Churna",
      "rasa_taste": "Sweet, Bitter",
      "virya_potency": "Hot",
      "dosage": "1-2 tsp twice daily",
      "anupana": "Warm water",
      "dosha_match_score": 8,
      "disease_relevance": 4,
      "therapeutic_uses": "Nervine tonic, stress relief, sleep aid",
      "reason": "Excellent for Vata and helps with insomnia (Hot potency)."
    }
  ],
  "total_found": 1,
  "message": "Targeted medicines for Vata with insomnia",
  "filters_applied": {
    "body_type": "vata",
    "disease": "insomnia",
    "limit": 8
  }
}
```

### 6. Get All Medicines
**GET** `/api/medicines`

Get all available medicines for debugging and exploration.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/medicines"
```

### 7. Get Medicine Formulation Types
**GET** `/api/medicine/formulation-types`

Get all available medicine formulation types.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/medicine/formulation-types"
```

### 8. Get Medicine Body Systems
**GET** `/api/medicine/body-systems`

Get all available body systems.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/medicine/body-systems"
```

### 9. Get Medicine Details
**GET** `/api/medicine/{medicine_name}`

Get detailed information about a specific medicine.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/medicine/Ashwagandha"
```

### 10. Get Common Conditions
**GET** `/api/medicine/common-conditions`

Get list of common conditions that can be searched.

**Example Request**:
```bash
curl "http://127.0.0.1:8000/api/medicine/common-conditions"
```

## 🎯 Usage Examples

### Food Recommendations

#### Basic Vata Recommendations
```bash
curl "http://127.0.0.1:8000/api/recommend?body_type=vata"
```

#### High-Protein Pitta Foods
```bash
curl "http://127.0.0.1:8000/api/recommend?body_type=pitta&high_protein=true"
```

#### Non-Vegetarian Kapha Foods
```bash
curl "http://127.0.0.1:8000/api/recommend?body_type=kapha&veg_only=false"
```

#### Low Rating Threshold
```bash
curl "http://127.0.0.1:8000/api/recommend?body_type=vata&min_rating=3&limit=20"
```

### Medicine Recommendations

#### Vata with Insomnia
```bash
curl "http://127.0.0.1:8000/api/recommend/medicine?body_type=vata&disease=insomnia"
```

#### Pitta with Acidity
```bash
curl "http://127.0.0.1:8000/api/recommend/medicine?body_type=pitta&disease=acidity"
```

#### Kapha with Joint Pain
```bash
curl "http://127.0.0.1:8000/api/recommend/medicine?body_type=kapha&disease=joint%20pain"
```

#### Vata with Anxiety (Limited Results)
```bash
curl "http://127.0.0.1:8000/api/recommend/medicine?body_type=vata&disease=anxiety&limit=3"
```

## 📊 Response Format

All recommendations include:
- **Basic Nutrition**: Protein, carbs, fat, fiber, calories
- **Micronutrients**: Vitamins and minerals
- **Health Tags**: Functional properties
- **Dosha-Specific Info**: Rating and recommendation reason
- **Fallback Messages**: When using alternative recommendations

## 🚨 Error Handling

The API returns appropriate HTTP status codes:
- `400`: Invalid body_type or parameters
- `404`: Food not found
- `500`: Internal server error

**Example Error Response**:
```json
{
  "detail": "body_type must be one of: vata, pitta, kapha"
}
```

## ⚡ Performance

- **Response Time**: <100ms for most requests
- **Data Loading**: JSON cache for fast startup
- **Memory Efficient**: Optimized data structures

## 🏗️ Project Structure

```
ayur-food-recommender/
├── main.py                 # FastAPI application and endpoints
├── models/
│   ├── recommender.py      # Food recommendation engine
│   └── medicine_recommender.py  # Medicine recommendation engine
├── data/
│   ├── food_processor.py   # Food data processing utilities
│   ├── medicine_processor.py     # Medicine data processing utilities
│   ├── food_recommendation_dataset.csv  # Raw food dataset
│   ├── Ayurvedic_Medicine_Dataset.xlsx   # Raw medicine dataset
│   ├── cleaned_foods.json  # Processed food data cache
│   └── cleaned_medicines.json  # Processed medicine data cache
├── requirements.txt        # Python dependencies
├── .env                    # Environment variables
└── README.md              # This file
```

## 🔧 Configuration

Environment variables (optional, set in `.env`):
- `DEBUG=True` - Enable debug mode
- `API_HOST=127.0.0.1` - API host
- `API_PORT=8000` - API port

## 🌐 CORS Configuration

The API is configured to work with React frontend:
- **Allowed Origins**: `http://localhost:3000`, `http://127.0.0.1:3000`
- **All Methods**: GET, POST, PUT, DELETE, OPTIONS
- **All Headers**: Supported

## 📈 Data Sources

The recommendation system uses comprehensive datasets with:

**Food Database:**
- **105+ Food Items**: Complete nutritional and Ayurvedic information
- **Nutritional Data**: Macros, micros, calories, fiber
- **Ayurvedic Properties**: Dosha suitability and ratings
- **Health Benefits**: Functional properties and tags
- **Dietary Information**: Vegetarian/Non-vegetarian classification

**Medicine Database:**
- **Classical Formulations**: Traditional Ayurvedic medicines
- **Therapeutic Properties**: Rasa, Virya, Vipaka, Guna
- **Dosha Scores**: Quantified suitability for each dosha
- **Clinical Applications**: Body systems and therapeutic uses
- **Dosage Information**: Recommended doses and anupana (vehicle)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test the API endpoints
5. Submit a pull request

## 📄 License

This project is for educational and demonstration purposes.

## 🆘 Troubleshooting

**Common Issues**:

1. **ModuleNotFoundError**: Ensure all dependencies are installed
2. **FileNotFoundError**: Run `python food_processor.py` first
3. **CORS Issues**: Check frontend URL configuration
4. **Slow Response**: Restart the server or check data cache

**Debug Commands**:
```bash
# Check data processing
cd data && python food_processor.py

# Test API health
curl http://127.0.0.1:8000/health

# View all foods
curl http://127.0.0.1:8000/api/foods | head -20
```

## 📞 Support

For issues and questions, refer to the API documentation at http://127.0.0.1:8000/docs
