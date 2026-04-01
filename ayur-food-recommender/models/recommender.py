from typing import List, Dict, Optional
import sys
from pathlib import Path

# Add data directory to path to import food_processor
sys.path.append(str(Path(__file__).parent.parent / 'data'))
from food_processor import FoodProcessor

class AyurRecommender:
    def __init__(self):
        self.processor = FoodProcessor()
        self.foods = self.processor.load_foods()
    
    def recommend(
        self,
        body_type: str,           # "vata", "pitta", "kapha"
        veg_only: bool = True,
        high_protein: bool = False,
        min_rating: int = 4,
        limit: int = 12
    ) -> List[Dict]:
        """
        Recommend foods based on Ayurvedic body type and preferences
        
        Args:
            body_type: Ayurvedic body type ("vata", "pitta", "kapha")
            veg_only: Filter for vegetarian/vegan foods only
            high_protein: Filter for high protein foods (>=8g)
            min_rating: Minimum suitability rating (1-5)
            limit: Maximum number of recommendations to return
            
        Returns:
            List of recommended food dictionaries with dosha-specific reasons
        """
        
        # Normalize body type input
        body_type = body_type.lower()
        if body_type not in ["vata", "pitta", "kapha"]:
            raise ValueError("body_type must be one of: 'vata', 'pitta', 'kapha'")
        
        # Map body type to data columns
        body_type_mapping = {
            "vata": {
                "suitable_col": "vaat_suitable",
                "rating_col": "vaat_rating", 
                "reason_col": "suitability_reason_vaat"
            },
            "pitta": {
                "suitable_col": "pit_suitable",
                "rating_col": "pit_rating",
                "reason_col": "suitability_reason_pit"
            },
            "kapha": {
                "suitable_col": "kapha_suitable", 
                "rating_col": "kapha_rating",
                "reason_col": "suitability_reason_kapha"
            }
        }
        
        cols = body_type_mapping[body_type]
        
        # Start with all foods
        filtered_foods = self.foods.copy()
        
        # Filter by body type suitability
        suitable_foods = []
        moderately_suitable_foods = []
        
        for food in filtered_foods:
            suitability = food.get(cols["suitable_col"], "").lower()
            rating = food.get(cols["rating_col"], 0)
            
            # Check if food meets minimum rating
            if rating >= min_rating:
                if suitability == "yes" or suitability == "suitable":
                    suitable_foods.append(food)
                elif suitability == "moderately_suitable":
                    moderately_suitable_foods.append(food)
        
        # Apply dietary preference filters
        def apply_dietary_filters(foods_list):
            filtered = []
            for food in foods_list:
                # Vegetarian filter
                if veg_only:
                    food_type = food.get("food_type", "").lower()
                    if food_type not in ["veg", "vegan"]:
                        continue
                
                # High protein filter
                if high_protein:
                    protein = food.get("protein_g", 0)
                    if protein < 8:
                        continue
                
                filtered.append(food)
            return filtered
        
        # Apply filters to both suitable and moderately suitable lists
        suitable_foods = apply_dietary_filters(suitable_foods)
        moderately_suitable_foods = apply_dietary_filters(moderately_suitable_foods)
        
        # Sort by rating (descending)
        suitable_foods.sort(key=lambda x: x.get(cols["rating_col"], 0), reverse=True)
        moderately_suitable_foods.sort(key=lambda x: x.get(cols["rating_col"], 0), reverse=True)
        
        # Prepare results with dosha-specific information
        results = []
        
        for food in suitable_foods[:limit]:
            food_copy = food.copy()
            food_copy["dosha_rating"] = food.get(cols["rating_col"], 0)
            food_copy["dosha_reason"] = food.get(cols["reason_col"], "")
            food_copy["recommendation_type"] = "highly_suitable"
            results.append(food_copy)
        
        # Fallback: if no suitable foods found, use moderately suitable
        if not results and moderately_suitable_foods:
            fallback_limit = min(5, len(moderately_suitable_foods))
            for food in moderately_suitable_foods[:fallback_limit]:
                food_copy = food.copy()
                food_copy["dosha_rating"] = food.get(cols["rating_col"], 0)
                food_copy["dosha_reason"] = food.get(cols["reason_col"], "")
                food_copy["recommendation_type"] = "moderately_suitable"
                food_copy["fallback_message"] = f"Limited options found for {body_type} type. Showing moderately suitable alternatives."
                results.append(food_copy)
        
        # Final fallback: if still no results, return top rated foods regardless of suitability
        if not results:
            all_foods_filtered = apply_dietary_filters(self.foods)
            all_foods_filtered.sort(key=lambda x: x.get(cols["rating_col"], 0), reverse=True)
            
            fallback_limit = min(5, len(all_foods_filtered))
            for food in all_foods_filtered[:fallback_limit]:
                food_copy = food.copy()
                food_copy["dosha_rating"] = food.get(cols["rating_col"], 0)
                food_copy["dosha_reason"] = food.get(cols["reason_col"], "No specific recommendation available")
                food_copy["recommendation_type"] = "general_fallback"
                food_copy["fallback_message"] = f"No specific recommendations found for {body_type} type. Showing general options."
                results.append(food_copy)
        
        return results[:limit]
    
    def get_food_details(self, food_name: str) -> Optional[Dict]:
        """Get detailed information about a specific food"""
        return self.processor.get_food_by_name(food_name)
    
    def get_categories(self) -> List[str]:
        """Get all available food categories"""
        categories = set()
        for food in self.foods:
            if food.get("category"):
                categories.add(food["category"])
        return sorted(list(categories))
    
    def get_nutrition_stats(self, foods_list: List[Dict]) -> Dict:
        """Calculate nutrition statistics for a list of foods"""
        if not foods_list:
            return {}
        
        total_protein = sum(food.get("protein_g", 0) for food in foods_list)
        total_carbs = sum(food.get("carbs_g", 0) for food in foods_list)
        total_fat = sum(food.get("fat_g", 0) for food in foods_list)
        total_fiber = sum(food.get("fiber_g", 0) for food in foods_list)
        total_calories = sum(food.get("calories_kcal", 0) for food in foods_list)
        
        return {
            "total_foods": len(foods_list),
            "avg_protein_g": round(total_protein / len(foods_list), 1),
            "avg_carbs_g": round(total_carbs / len(foods_list), 1),
            "avg_fat_g": round(total_fat / len(foods_list), 1),
            "avg_fiber_g": round(total_fiber / len(foods_list), 1),
            "avg_calories_kcal": round(total_calories / len(foods_list), 1),
            "total_protein_g": round(total_protein, 1),
            "total_carbs_g": round(total_carbs, 1),
            "total_fat_g": round(total_fat, 1),
            "total_fiber_g": round(total_fiber, 1),
            "total_calories_kcal": round(total_calories, 1)
        }

# Convenience function for quick recommendations
def get_recommendations(
    body_type: str,
    veg_only: bool = True,
    high_protein: bool = False,
    min_rating: int = 4,
    limit: int = 12
) -> List[Dict]:
    """Convenience function for getting recommendations"""
    recommender = AyurRecommender()
    return recommender.recommend(
        body_type=body_type,
        veg_only=veg_only,
        high_protein=high_protein,
        min_rating=min_rating,
        limit=limit
    )

# Test the recommender
if __name__ == "__main__":
    recommender = AyurRecommender()
    
    print("Testing Ayurvedic Food Recommender")
    print("=" * 50)
    
    # Test for each body type
    for body_type in ["vata", "pitta", "kapha"]:
        print(f"\nRecommendations for {body_type.upper()} type:")
        recommendations = recommender.recommend(
            body_type=body_type,
            veg_only=True,
            min_rating=4,
            limit=3
        )
        
        for i, food in enumerate(recommendations, 1):
            print(f"{i}. {food['food_name']} (Rating: {food.get('dosha_rating', 'N/A')})")
            print(f"   {food.get('dosha_reason', 'No reason available')}")
            print(f"   Category: {food.get('category', 'N/A')}, Protein: {food.get('protein_g', 'N/A')}g")
    
    print(f"\nTotal foods in database: {len(recommender.foods)}")
    print(f"Available categories: {', '.join(recommender.get_categories())}")
