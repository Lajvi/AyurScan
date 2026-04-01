from typing import List, Dict, Optional, Tuple
import sys
from pathlib import Path
import re

# Add data directory to path to import medicine_processor
sys.path.append(str(Path(__file__).parent.parent / 'data'))
from medicine_processor import MedicineProcessor

class AyurMedicineRecommender:
    def __init__(self):
        self.processor = MedicineProcessor()
        self.medicines = self.processor.load_medicines()
    
    def recommend_medicines(
        self,
        body_type: str,           # "vata", "pitta", "kapha"
        disease: str,             # e.g. "insomnia", "acidity", "joint pain", "cough"
        limit: int = 8
    ) -> List[Dict]:
        """
        Recommend medicines based on body type and disease
        
        Args:
            body_type: Ayurvedic body type ("vata", "pitta", "kapha")
            disease: Health condition or symptom
            limit: Maximum number of recommendations
            
        Returns:
            List of recommended medicine dictionaries with scores and reasons
        """
        
        # Normalize inputs
        body_type = body_type.lower()
        disease = disease.lower().strip()
        
        if body_type not in ["vata", "pitta", "kapha"]:
            raise ValueError("body_type must be one of: 'vata', 'pitta', 'kapha'")
        
        # Step 1: Filter medicines based on body type
        body_type_medicines = self._filter_by_body_type(body_type)
        
        # Step 2: Score medicines based on disease relevance
        scored_medicines = self._score_by_disease_relevance(
            body_type_medicines, disease, body_type
        )
        
        # Step 3: Sort by dosha score and disease relevance
        sorted_medicines = self._sort_medicines(scored_medicines, body_type)
        
        # Step 4: Prepare results with reasons
        results = self._prepare_results(sorted_medicines[:limit], body_type, disease)
        
        # Step 5: Fallback if no strong matches
        if not results:
            results = self._get_fallback_recommendations(body_type, disease, limit)
        
        return results
    
    def _filter_by_body_type(self, body_type: str) -> List[Dict]:
        """Filter medicines based on body type with score threshold"""
        filtered_medicines = []
        score_threshold = 6  # High score threshold
        
        for medicine in self.medicines:
            # Get relevant dosha score
            dosha_scores = {
                'vata': medicine.get('vata_score', 0),
                'pitta': medicine.get('pitta_score', 0),
                'kapha': medicine.get('kapha_score', 0)
            }
            
            relevant_score = dosha_scores.get(body_type, 0)
            dosha_label = medicine.get('dosha_label', '').lower()
            
            # Include if:
            # 1. High dosha score
            # 2. Dosha label contains the body type
            # 3. Tridosha (safe fallback)
            if (relevant_score >= score_threshold or 
                body_type in dosha_label or 
                'tridosha' in dosha_label):
                
                medicine_copy = medicine.copy()
                medicine_copy['dosha_match_score'] = relevant_score
                filtered_medicines.append(medicine_copy)
        
        return filtered_medicines
    
    def _score_by_disease_relevance(
        self, medicines: List[Dict], disease: str, body_type: str
    ) -> List[Dict]:
        """Score medicines based on disease keyword matching"""
        
        # Split disease into keywords
        disease_keywords = self._extract_keywords(disease)
        
        for medicine in medicines:
            therapeutic_uses = medicine.get('therapeutic_uses', '').lower()
            body_system = medicine.get('body_system', '').lower()
            
            # Calculate keyword match score
            keyword_score = 0
            matched_keywords = []
            
            for keyword in disease_keywords:
                # Check in therapeutic uses
                if keyword in therapeutic_uses:
                    keyword_score += 2
                    matched_keywords.append(keyword)
                
                # Check in body system (lower weight)
                if keyword in body_system:
                    keyword_score += 1
                    matched_keywords.append(keyword)
            
            medicine['disease_relevance_score'] = keyword_score
            medicine['matched_keywords'] = matched_keywords
        
        return medicines
    
    def _extract_keywords(self, disease: str) -> List[str]:
        """Extract meaningful keywords from disease string"""
        # Common medical keywords and their variations
        keyword_mapping = {
            'insomnia': ['sleep', 'insomnia', 'sleeplessness'],
            'acidity': ['acid', 'acidity', 'heartburn', 'reflux', 'gerd'],
            'joint pain': ['joint', 'pain', 'arthritis', 'inflammation'],
            'cough': ['cough', 'cold', 'respiratory', 'congestion'],
            'headache': ['headache', 'migraine', 'head', 'pain'],
            'digestion': ['digestion', 'digestive', 'stomach', 'gut'],
            'stress': ['stress', 'anxiety', 'mental', 'nervous'],
            'skin': ['skin', 'dermatology', 'rash', 'eczema'],
            'respiratory': ['respiratory', 'breathing', 'lungs', 'asthma'],
            'immune': ['immune', 'immunity', 'resistance', 'defense']
        }
        
        # Normalize and split input
        words = re.findall(r'\b\w+\b', disease.lower())
        keywords = []
        
        # Map words to medical keywords
        for word in words:
            for key, variations in keyword_mapping.items():
                if word in variations or key in word:
                    keywords.extend(variations)
        
        # Add original words if not mapped
        for word in words:
            if word not in keywords and len(word) > 2:
                keywords.append(word)
        
        return list(set(keywords))  # Remove duplicates
    
    def _sort_medicines(self, medicines: List[Dict], body_type: str) -> List[Dict]:
        """Sort medicines by dosha score and disease relevance"""
        def sort_key(medicine):
            return (
                medicine.get('dosha_match_score', 0),  # Primary: dosha score
                medicine.get('disease_relevance_score', 0),  # Secondary: disease relevance
                medicine.get('medicine_name', '')  # Tertiary: alphabetical
            )
        
        return sorted(medicines, key=sort_key, reverse=True)
    
    def _prepare_results(
        self, medicines: List[Dict], body_type: str, disease: str
    ) -> List[Dict]:
        """Prepare final results with selected fields and generated reasons"""
        results = []
        
        for medicine in medicines:
            result = {
                'medicine_name': medicine.get('medicine_name', ''),
                'formulation_type': medicine.get('formulation_type', ''),
                'rasa_taste': medicine.get('rasa_taste', ''),
                'virya_potency': medicine.get('virya_potency', ''),
                'dosage': medicine.get('dosage', ''),
                'anupana': medicine.get('anupana', ''),
                'dosha_match_score': medicine.get('dosha_match_score', 0),
                'disease_relevance': medicine.get('disease_relevance_score', 0),
                'therapeutic_uses': medicine.get('therapeutic_uses', ''),
                'reason': self._generate_reason(medicine, body_type, disease)
            }
            results.append(result)
        
        return results
    
    def _generate_reason(self, medicine: Dict, body_type: str, disease: str) -> str:
        """Generate a short, informative reason for recommendation"""
        medicine_name = medicine.get('medicine_name', 'This medicine')
        dosha_score = medicine.get('dosha_match_score', 0)
        relevance_score = medicine.get('disease_relevance_score', 0)
        matched_keywords = medicine.get('matched_keywords', [])
        
        # Base reason components
        dosha_reason = f"Good for {body_type.capitalize()}"
        
        if dosha_score >= 7:
            dosha_reason = f"Excellent for {body_type.capitalize()}"
        elif dosha_score >= 5:
            dosha_reason = f"Suitable for {body_type.capitalize()}"
        
        # Disease relevance component
        disease_reason = ""
        if relevance_score > 0 and matched_keywords:
            primary_keyword = matched_keywords[0]
            disease_reason = f" and helps with {primary_keyword}"
        elif relevance_score > 0:
            disease_reason = " and relevant to your condition"
        
        # Additional properties
        properties = []
        if medicine.get('virya_potency'):
            properties.append(f"{medicine['virya_potency']} potency")
        if medicine.get('rasa_taste'):
            properties.append(f"{medicine['rasa_taste']} taste")
        
        property_text = ""
        if properties:
            property_text = f" ({', '.join(properties)})"
        
        return f"{dosha_reason}{disease_reason}{property_text}."
    
    def _get_fallback_recommendations(
        self, body_type: str, disease: str, limit: int
    ) -> List[Dict]:
        """Fallback to Tridosha medicines when no strong matches found"""
        # Get Tridosha medicines
        tridosha_medicines = []
        for medicine in self.medicines:
            dosha_label = medicine.get('dosha_label', '').lower()
            if 'tridosha' in dosha_label:
                medicine_copy = medicine.copy()
                medicine_copy['dosha_match_score'] = 5  # Moderate score for fallback
                tridosha_medicines.append(medicine_copy)
        
        # Score by disease relevance
        scored_tridosha = self._score_by_disease_relevance(
            tridosha_medicines, disease, body_type
        )
        
        # Sort and prepare results
        sorted_tridosha = self._sort_medicines(scored_tridosha, body_type)
        results = self._prepare_results(sorted_tridosha[:5], body_type, disease)
        
        # Add fallback note
        for result in results:
            result['fallback_note'] = "Tridosha medicine suitable for all body types"
        
        return results
    
    def get_medicine_details(self, medicine_name: str) -> Optional[Dict]:
        """Get detailed information about a specific medicine"""
        return self.processor.get_medicine_by_name(medicine_name)
    
    def get_formulation_types(self) -> List[str]:
        """Get all available formulation types"""
        return self.processor.get_formulation_types()
    
    def get_body_systems(self) -> List[str]:
        """Get all available body systems"""
        return self.processor.get_body_systems()
    
    def get_common_conditions(self) -> List[str]:
        """Get list of common conditions that can be searched"""
        return [
            "insomnia", "acidity", "joint pain", "cough", "headache",
            "digestion", "stress", "skin problems", "respiratory issues",
            "immune support", "fever", "fatigue", "anxiety", "depression"
        ]

# Convenience function for quick recommendations
def get_medicine_recommendations(
    body_type: str,
    disease: str,
    limit: int = 8
) -> List[Dict]:
    """Convenience function for getting medicine recommendations"""
    recommender = AyurMedicineRecommender()
    return recommender.recommend_medicines(
        body_type=body_type,
        disease=disease,
        limit=limit
    )

# Test the recommender
if __name__ == "__main__":
    recommender = AyurMedicineRecommender()
    
    print("Testing Ayurvedic Medicine Recommender")
    print("=" * 50)
    
    # Test cases
    test_cases = [
        ("vata", "insomnia"),
        ("pitta", "acidity"),
        ("kapha", "cough"),
        ("vata", "joint pain")
    ]
    
    for body_type, disease in test_cases:
        print(f"\nRecommendations for {body_type.upper()} with {disease}:")
        recommendations = recommender.recommend_medicines(
            body_type=body_type,
            disease=disease,
            limit=3
        )
        
        for i, medicine in enumerate(recommendations, 1):
            print(f"{i}. {medicine['medicine_name']} ({medicine['formulation_type']})")
            print(f"   Score: {medicine['dosha_match_score']}, Relevance: {medicine['disease_relevance']}")
            print(f"   Reason: {medicine['reason']}")
            if 'fallback_note' in medicine:
                print(f"   Note: {medicine['fallback_note']}")
    
    print(f"\nTotal medicines in database: {len(recommender.medicines)}")
    print(f"Available formulation types: {', '.join(recommender.get_formulation_types())}")
    print(f"Available body systems: {', '.join(recommender.get_body_systems())}")
