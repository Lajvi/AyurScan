from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import sys
from pathlib import Path

# Add models directory to path
sys.path.append(str(Path(__file__).parent / 'models'))
from recommender import AyurRecommender
from medicine_recommender import AyurMedicineRecommender

app = FastAPI(title="Ayurvedic Food Recommender API", version="1.0.0")

# Initialize recommenders
food_recommender = AyurRecommender()
medicine_recommender = AyurMedicineRecommender()

# Configure CORS middleware to allow React frontend on localhost:3000
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://127.0.0.1:3000"],  # React frontend URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic Models
class FoodItem(BaseModel):
    food_name: str
    food_type: str
    category: str
    protein_g: float
    carbs_g: float
    fat_g: float
    fiber_g: float
    calories_kcal: float
    vitamins: str
    minerals: str
    health_tags: str
    vaat_suitable: Optional[str] = None
    pit_suitable: Optional[str] = None
    kapha_suitable: Optional[str] = None
    vaat_rating: Optional[int] = None
    pit_rating: Optional[int] = None
    kapha_rating: Optional[int] = None
    suitability_reason_vaat: Optional[str] = None
    suitability_reason_pit: Optional[str] = None
    suitability_reason_kapha: Optional[str] = None
    dosha_rating: Optional[int] = None
    dosha_reason: Optional[str] = None
    recommendation_type: Optional[str] = None
    fallback_message: Optional[str] = None

class RecommendationResponse(BaseModel):
    body_type: str
    recommended_foods: List[FoodItem]
    total_found: int
    message: str
    filters_applied: Dict[str, Any]

class FoodsListResponse(BaseModel):
    foods: List[FoodItem]
    total_count: int
    categories: List[str]

# Medicine Pydantic Models
class MedicineItem(BaseModel):
    medicine_name: str
    formulation_type: str
    rasa_taste: str
    virya_potency: str
    dosage: str
    anupana: str
    dosha_match_score: int
    disease_relevance: int
    therapeutic_uses: str
    reason: str
    fallback_note: Optional[str] = None

class MedicineRecommendationResponse(BaseModel):
    body_type: str
    disease: str
    recommended_medicines: List[MedicineItem]
    total_found: int
    message: str
    filters_applied: Dict[str, Any]

@app.get("/")
async def root():
    return {"message": "Food Recommender API is running"}

@app.get("/api/recommend", response_model=RecommendationResponse)
async def get_recommendations(
    body_type: str = Query(..., description="Body type: vata, pitta, or kapha"),
    veg_only: bool = Query(True, description="Filter for vegetarian foods only"),
    high_protein: bool = Query(False, description="Filter for high protein foods (>=8g)"),
    min_rating: int = Query(4, ge=1, le=5, description="Minimum suitability rating (1-5)"),
    limit: int = Query(12, ge=1, le=50, description="Maximum number of recommendations")
):
    """
    Get Ayurvedic food recommendations based on body type and preferences
    """
    try:
        # Validate body_type
        if body_type.lower() not in ["vata", "pitta", "kapha"]:
            raise HTTPException(
                status_code=400, 
                detail="body_type must be one of: vata, pitta, kapha"
            )
        
        # Get recommendations
        recommendations = food_recommender.recommend(
            body_type=body_type.lower(),
            veg_only=veg_only,
            high_protein=high_protein,
            min_rating=min_rating,
            limit=limit
        )
        
        # Determine message based on recommendations
        if recommendations:
            first_rec_type = recommendations[0].get("recommendation_type", "unknown")
            if first_rec_type == "highly_suitable":
                message = f"Highly suitable foods for {body_type.capitalize()} type"
            elif first_rec_type == "moderately_suitable":
                message = f"Moderately suitable foods for {body_type.capitalize()} type"
            else:
                message = f"General food recommendations for {body_type.capitalize()} type"
        else:
            message = f"No recommendations found for {body_type.capitalize()} type"
        
        # Prepare response
        response_data = {
            "body_type": body_type.lower(),
            "recommended_foods": recommendations,
            "total_found": len(recommendations),
            "message": message,
            "filters_applied": {
                "veg_only": veg_only,
                "high_protein": high_protein,
                "min_rating": min_rating,
                "limit": limit
            }
        }
        
        return response_data
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/recommend/medicine", response_model=MedicineRecommendationResponse)
async def get_medicine_recommendations(
    body_type: str = Query(..., description="Body type: vata, pitta, or kapha"),
    disease: str = Query(..., description="Health condition or symptom"),
    limit: int = Query(8, ge=1, le=20, description="Maximum number of recommendations")
):
    """
    Get Ayurvedic medicine recommendations based on body type and disease
    """
    try:
        # Validate body_type
        if body_type.lower() not in ["vata", "pitta", "kapha"]:
            raise HTTPException(
                status_code=400, 
                detail="body_type must be one of: vata, pitta, kapha"
            )
        
        # Get medicine recommendations
        recommendations = medicine_recommender.recommend_medicines(
            body_type=body_type.lower(),
            disease=disease.lower(),
            limit=limit
        )
        
        # Determine message based on recommendations
        if recommendations:
            has_fallback = any('fallback_note' in rec for rec in recommendations)
            if has_fallback:
                message = f"General medicine recommendations for {body_type.capitalize()} with {disease}"
            else:
                message = f"Targeted medicines for {body_type.capitalize()} with {disease}"
        else:
            message = f"No medicine recommendations found for {body_type.capitalize()} with {disease}"
        
        # Prepare response
        response_data = {
            "body_type": body_type.lower(),
            "disease": disease.lower(),
            "recommended_medicines": recommendations,
            "total_found": len(recommendations),
            "message": message,
            "filters_applied": {
                "body_type": body_type.lower(),
                "disease": disease.lower(),
                "limit": limit
            }
        }
        
        return response_data
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/foods", response_model=FoodsListResponse)
async def get_all_foods():
    """
    Get all available foods (for debugging and exploration)
    """
    try:
        all_foods = food_recommender.foods
        categories = food_recommender.get_categories()
        
        return {
            "foods": all_foods,
            "total_count": len(all_foods),
            "categories": categories
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/categories")
async def get_categories():
    """
    Get all available food categories
    """
    try:
        categories = food_recommender.get_categories()
        return {"categories": categories, "total_count": len(categories)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/food/{food_name}")
async def get_food_details(food_name: str):
    """
    Get detailed information about a specific food
    """
    try:
        food_details = food_recommender.get_food_details(food_name)
        if not food_details:
            raise HTTPException(status_code=404, detail=f"Food '{food_name}' not found")
        return food_details
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "Ayurvedic Food Recommender"}

@app.get("/api/medicines")
async def get_all_medicines():
    """
    Get all available medicines (for debugging and exploration)
    """
    try:
        all_medicines = medicine_recommender.medicines
        formulation_types = medicine_recommender.get_formulation_types()
        body_systems = medicine_recommender.get_body_systems()
        
        return {
            "medicines": all_medicines,
            "total_count": len(all_medicines),
            "formulation_types": formulation_types,
            "body_systems": body_systems
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/medicine/formulation-types")
async def get_medicine_formulation_types():
    """
    Get all available medicine formulation types
    """
    try:
        formulation_types = medicine_recommender.get_formulation_types()
        return {"formulation_types": formulation_types, "total_count": len(formulation_types)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/medicine/body-systems")
async def get_medicine_body_systems():
    """
    Get all available body systems
    """
    try:
        body_systems = medicine_recommender.get_body_systems()
        return {"body_systems": body_systems, "total_count": len(body_systems)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/medicine/{medicine_name}")
async def get_medicine_details(medicine_name: str):
    """
    Get detailed information about a specific medicine
    """
    try:
        medicine_details = medicine_recommender.get_medicine_details(medicine_name)
        if not medicine_details:
            raise HTTPException(status_code=404, detail=f"Medicine '{medicine_name}' not found")
        return medicine_details
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/medicine/common-conditions")
async def get_common_conditions():
    """
    Get list of common conditions that can be searched
    """
    try:
        conditions = medicine_recommender.get_common_conditions()
        return {"conditions": conditions, "total_count": len(conditions)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
