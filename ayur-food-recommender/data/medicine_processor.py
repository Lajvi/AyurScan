import pandas as pd
import json
from pathlib import Path
from typing import List, Dict, Union

class MedicineProcessor:
    def __init__(self, excel_path: str = "Ayurvedic_Medicine_Dataset.xlsx"):
        self.excel_path = Path(__file__).parent / excel_path
        self.json_path = Path(__file__).parent / "cleaned_medicines.json"
        self.cleaned_data = None
    
    def load_raw_data(self) -> pd.DataFrame:
        """Load the raw Excel data from Sheet1"""
        try:
            df = pd.read_excel(self.excel_path, sheet_name="Sheet1")
            print(f"Loaded {len(df)} rows from {self.excel_path}")
            return df
        except FileNotFoundError:
            raise FileNotFoundError(f"Excel file not found at {self.excel_path}")
        except Exception as e:
            raise Exception(f"Error reading Excel file: {str(e)}")
    
    def process_data(self) -> pd.DataFrame:
        """Process the raw data to create clean medicine records"""
        df = self.load_raw_data()
        
        # Create a copy to avoid SettingWithCopyWarning
        processed_df = df.copy()
        
        # Generate medicine_id if not present
        if 'medicine_id' not in processed_df.columns:
            processed_df['medicine_id'] = range(1, len(processed_df) + 1)
        
        # Standardize column names (handle various possible column names)
        column_mapping = {
            # Medicine identification
            'Medicine Name': 'medicine_name',
            'Medicine_Name': 'medicine_name',
            'medicine_name': 'medicine_name',
            'Formulation Type': 'formulation_type',
            'Formulation_Type': 'formulation_type',
            'formulation_type': 'formulation_type',
            
            # Ayurvedic properties
            'Rasa (Taste)': 'rasa_taste',
            'Rasa_Taste': 'rasa_taste',
            'rasa_taste': 'rasa_taste',
            'Virya (Potency)': 'virya_potency',
            'Virya_Potency': 'virya_potency',
            'virya_potency': 'virya_potency',
            'Vipaka': 'vipaka',
            'vipaka': 'vipaka',
            'Guna (Quality)': 'guna_quality',
            'Guna_Quality': 'guna_quality',
            'guna_quality': 'guna_quality',
            
            # Dosha scores
            'Vata Score': 'vata_score',
            'Vata_Score': 'vata_score',
            'vata_score': 'vata_score',
            'Pitta Score': 'pitta_score',
            'Pitta_Score': 'pitta_score',
            'pitta_score': 'pitta_score',
            'Kapha Score': 'kapha_score',
            'Kapha_Score': 'kapha_score',
            'kapha_score': 'kapha_score',
            
            # Therapeutic information
            'Body System': 'body_system',
            'Body_System': 'body_system',
            'body_system': 'body_system',
            'Therapeutic Uses': 'therapeutic_uses',
            'Therapeutic_Uses': 'therapeutic_uses',
            'therapeutic_uses': 'therapeutic_uses',
            'Dosage': 'dosage',
            'dosage': 'dosage',
            'Anupana': 'anupana',
            'anupana': 'anupana',
            
            # Classification
            'Dosha Label': 'dosha_label',
            'Dosha_Label': 'dosha_label',
            'dosha_label': 'dosha_label'
        }
        
        # Rename columns based on mapping
        processed_df = processed_df.rename(columns=column_mapping)
        
        # Ensure all required columns exist
        required_columns = [
            'medicine_id', 'medicine_name', 'formulation_type', 'rasa_taste', 
            'virya_potency', 'vipaka', 'guna_quality', 'vata_score', 
            'pitta_score', 'kapha_score', 'body_system', 'therapeutic_uses', 
            'dosage', 'anupana', 'dosha_label'
        ]
        
        for col in required_columns:
            if col not in processed_df.columns:
                processed_df[col] = None
        
        # Clean and process data
        processed_df = self._clean_medicine_data(processed_df)
        
        # Remove duplicates based on medicine_name
        processed_df = processed_df.drop_duplicates(subset=['medicine_name'], keep='first')
        
        # Reset index after removing duplicates
        processed_df = processed_df.reset_index(drop=True)
        
        self.cleaned_data = processed_df
        print(f"Processed {len(processed_df)} unique medicine items")
        return processed_df
    
    def _clean_medicine_data(self, df: pd.DataFrame) -> pd.DataFrame:
        """Clean and standardize medicine data"""
        # Convert numeric columns to appropriate types
        numeric_columns = ['vata_score', 'pitta_score', 'kapha_score']
        for col in numeric_columns:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')
        
        # Clean text columns
        text_columns = [
            'medicine_name', 'formulation_type', 'rasa_taste', 'virya_potency',
            'vipaka', 'guna_quality', 'body_system', 'therapeutic_uses',
            'dosage', 'anupana', 'dosha_label'
        ]
        
        for col in text_columns:
            if col in df.columns:
                df[col] = df[col].astype(str).str.strip()
                # Replace 'nan' strings with None
                df[col] = df[col].replace('nan', None)
        
        return df
    
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
    
    def load_medicines(self) -> List[Dict]:
        """Load medicines from JSON file (fast loading)"""
        try:
            with open(self.json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            print(f"Loaded {len(data)} medicine items from {self.json_path}")
            return data
        except FileNotFoundError:
            # If JSON doesn't exist, process and save first
            print("JSON file not found. Processing data first...")
            self.process_data()
            self.save_cleaned_data()
            return self.load_medicines()
    
    def get_medicines_dataframe(self) -> pd.DataFrame:
        """Load medicines as DataFrame"""
        medicines_list = self.load_medicines()
        return pd.DataFrame(medicines_list)
    
    def get_medicine_by_name(self, medicine_name: str) -> Dict:
        """Get a specific medicine by name"""
        medicines = self.load_medicines()
        for medicine in medicines:
            if medicine['medicine_name'].lower() == medicine_name.lower():
                return medicine
        return None
    
    def get_medicines_by_formulation(self, formulation_type: str) -> List[Dict]:
        """Get medicines by formulation type"""
        medicines = self.load_medicines()
        return [medicine for medicine in medicines 
                if medicine.get('formulation_type', '').lower() == formulation_type.lower()]
    
    def get_medicines_by_body_system(self, body_system: str) -> List[Dict]:
        """Get medicines by body system"""
        medicines = self.load_medicines()
        return [medicine for medicine in medicines 
                if medicine.get('body_system', '').lower() == body_system.lower()]
    
    def get_medicines_by_dosha(self, dosha: str) -> List[Dict]:
        """Get medicines suitable for specific dosha"""
        medicines = self.load_medicines()
        suitable_medicines = []
        
        for medicine in medicines:
            dosha_label = medicine.get('dosha_label', '').lower()
            if dosha.lower() in dosha_label:
                suitable_medicines.append(medicine)
        
        return suitable_medicines
    
    def get_formulation_types(self) -> List[str]:
        """Get all available formulation types"""
        medicines = self.load_medicines()
        formulation_types = set()
        for medicine in medicines:
            if medicine.get('formulation_type'):
                formulation_types.add(medicine['formulation_type'])
        return sorted(list(formulation_types))
    
    def get_body_systems(self) -> List[str]:
        """Get all available body systems"""
        medicines = self.load_medicines()
        body_systems = set()
        for medicine in medicines:
            if medicine.get('body_system'):
                body_systems.add(medicine['body_system'])
        return sorted(list(body_systems))

# Convenience function for direct loading
def load_medicines() -> List[Dict]:
    """Convenience function to load all medicines"""
    processor = MedicineProcessor()
    return processor.load_medicines()

# Main execution for testing
if __name__ == "__main__":
    processor = MedicineProcessor()
    
    # Process and save data
    print("Processing medicine data...")
    processor.process_data()
    processor.save_cleaned_data()
    
    # Test loading
    print("\nTesting data loading...")
    medicines = processor.load_medicines()
    print(f"Successfully loaded {len(medicines)} medicine items")
    
    # Show sample
    if medicines:
        print("\nSample medicine item:")
        sample_medicine = medicines[0]
        for key, value in sample_medicine.items():
            print(f"  {key}: {value}")
    
    # Show available categories
    print(f"\nAvailable formulation types: {', '.join(processor.get_formulation_types())}")
    print(f"Available body systems: {', '.join(processor.get_body_systems())}")
