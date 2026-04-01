import pandas as pd
import json
from pathlib import Path
from typing import List, Dict, Union

class FoodProcessor:
    def __init__(self, csv_path: str = "food_recommendation_dataset.csv"):
        self.csv_path = Path(__file__).parent / csv_path
        self.json_path = Path(__file__).parent / "cleaned_foods.json"
        self.cleaned_data = None
    
    def load_raw_data(self) -> pd.DataFrame:
        """Load the raw CSV data"""
        try:
            df = pd.read_csv(self.csv_path)
            print(f"Loaded {len(df)} rows from {self.csv_path}")
            return df
        except FileNotFoundError:
            raise FileNotFoundError(f"CSV file not found at {self.csv_path}")
    
    def process_data(self) -> pd.DataFrame:
        """Process the raw data to create one row per food item"""
        df = self.load_raw_data()
        
        # Get unique food items
        unique_foods = df['food_name'].unique()
        processed_foods = []
        
        for food_name in unique_foods:
            food_data = df[df['food_name'] == food_name].copy()
            
            # Get base food info (use average for nutritional data if multiple entries)
            base_info = {
                'food_name': food_name,
                'food_type': food_data['food_type'].iloc[0],
                'category': food_data['category'].iloc[0],
                'protein_g': food_data['protein_g'].mean(),
                'carbs_g': food_data['carbs_g'].mean(),
                'fat_g': food_data['fat_g'].mean(),
                'fiber_g': food_data['fiber_g'].mean(),
                'calories_kcal': food_data['calories_kcal'].mean(),
                'vitamins': food_data['vitamins'].iloc[0],
                'minerals': food_data['minerals'].iloc[0],
                'health_tags': food_data['health_tags'].iloc[0]
            }
            
            # Process body type specific information
            for _, row in food_data.iterrows():
                body_type = row['body_type']
                
                # Map body types to standardized names
                if body_type == "Vaat":
                    base_info['vaat_suitable'] = row['vaat_suitable']
                    base_info['vaat_rating'] = row['vaat_rating']
                    base_info['suitability_reason_vaat'] = row['suitability_reason']
                elif body_type == "Pit":
                    base_info['pit_suitable'] = row['pit_suitable']
                    base_info['pit_rating'] = row['pit_rating']
                    base_info['suitability_reason_pit'] = row['suitability_reason']
                elif body_type == "Cough (Kapha)":
                    base_info['kapha_suitable'] = row['cough_suitable']
                    base_info['kapha_rating'] = row['cough_rating']
                    base_info['suitability_reason_kapha'] = row['suitability_reason']
            
            processed_foods.append(base_info)
        
        # Create processed DataFrame
        processed_df = pd.DataFrame(processed_foods)
        
        # Remove duplicates if any
        processed_df = processed_df.drop_duplicates(subset=['food_name'], keep='first')
        
        # Ensure all required columns exist
        required_columns = [
            'food_name', 'food_type', 'category', 'protein_g', 'carbs_g', 'fat_g', 
            'fiber_g', 'calories_kcal', 'vitamins', 'minerals', 'health_tags',
            'vaat_suitable', 'pit_suitable', 'kapha_suitable',
            'vaat_rating', 'pit_rating', 'kapha_rating',
            'suitability_reason_vaat', 'suitability_reason_pit', 'suitability_reason_kapha'
        ]
        
        for col in required_columns:
            if col not in processed_df.columns:
                processed_df[col] = None
        
        self.cleaned_data = processed_df
        print(f"Processed {len(processed_df)} unique food items")
        return processed_df
    
    def save_cleaned_data(self) -> None:
        """Save cleaned data as JSON for fast loading"""
        if self.cleaned_data is None:
            self.process_data()
        
        # Convert DataFrame to list of dictionaries
        data_dict = self.cleaned_data.to_dict('records')
        
        # Save as JSON
        with open(self.json_path, 'w', encoding='utf-8') as f:
            json.dump(data_dict, f, indent=2, ensure_ascii=False)
        
        print(f"Saved cleaned data to {self.json_path}")
    
    def load_foods(self) -> List[Dict]:
        """Load foods from JSON file (fast loading)"""
        try:
            with open(self.json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            print(f"Loaded {len(data)} food items from {self.json_path}")
            return data
        except FileNotFoundError:
            # If JSON doesn't exist, process and save first
            print("JSON file not found. Processing data first...")
            self.process_data()
            self.save_cleaned_data()
            return self.load_foods()
    
    def get_foods_dataframe(self) -> pd.DataFrame:
        """Load foods as DataFrame"""
        foods_list = self.load_foods()
        return pd.DataFrame(foods_list)
    
    def get_food_by_name(self, food_name: str) -> Dict:
        """Get a specific food by name"""
        foods = self.load_foods()
        for food in foods:
            if food['food_name'].lower() == food_name.lower():
                return food
        return None
    
    def get_foods_by_category(self, category: str) -> List[Dict]:
        """Get foods by category"""
        foods = self.load_foods()
        return [food for food in foods if food['category'].lower() == category.lower()]
    
    def get_suitable_foods(self, dosha: str) -> List[Dict]:
        """Get foods suitable for specific dosha"""
        foods = self.load_foods()
        suitable_foods = []
        
        for food in foods:
            if dosha.lower() == 'vaat' and food.get('vaat_suitable') == 'Yes':
                suitable_foods.append(food)
            elif dosha.lower() == 'pit' and food.get('pit_suitable') == 'Yes':
                suitable_foods.append(food)
            elif dosha.lower() == 'kapha' and food.get('kapha_suitable') == 'Yes':
                suitable_foods.append(food)
        
        return suitable_foods

# Convenience function for direct loading
def load_foods() -> List[Dict]:
    """Convenience function to load all foods"""
    processor = FoodProcessor()
    return processor.load_foods()

# Main execution for testing
if __name__ == "__main__":
    processor = FoodProcessor()
    
    # Process and save data
    print("Processing food data...")
    processor.process_data()
    processor.save_cleaned_data()
    
    # Test loading
    print("\nTesting data loading...")
    foods = processor.load_foods()
    print(f"Successfully loaded {len(foods)} food items")
    
    # Show sample
    if foods:
        print("\nSample food item:")
        sample_food = foods[0]
        for key, value in sample_food.items():
            print(f"  {key}: {value}")
