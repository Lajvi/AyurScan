# Run with: pytest tests/ -v

import pytest
import sys
from pathlib import Path

# Add project root to path for imports
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))
sys.path.insert(0, str(project_root / 'models'))
sys.path.insert(0, str(project_root / 'data'))

@pytest.fixture
def food_recommender():
    """Fixture for AyurRecommender instance"""
    from recommender import AyurRecommender
    return AyurRecommender()

@pytest.fixture
def medicine_recommender():
    """Fixture for AyurMedicineRecommender instance"""
    from medicine_recommender import AyurMedicineRecommender
    return AyurMedicineRecommender()

@pytest.fixture
def sample_foods():
    """Sample known foods for testing"""
    return {
        'moong_dal': {
            'food_name': 'Moong Dal',
            'food_type': 'Veg',
            'category': 'Legumes',
            'vaat_suitable': 'Yes',
            'pit_suitable': 'Yes', 
            'cough_suitable': 'Yes',
            'vaat_rating': 5,
            'pit_rating': 5,
            'cough_rating': 5,
            'protein_g': 8.0
        },
        'bajra_roti': {
            'food_name': 'Bajra Roti',
            'food_type': 'Veg',
            'category': 'Grains',
            'vaat_suitable': 'Yes',
            'pit_suitable': 'No',
            'cough_suitable': 'Yes',
            'vaat_rating': 4,
            'pit_rating': 2,
            'cough_rating': 5,
            'protein_g': 1.6
        },
        'basmati_rice': {
            'food_name': 'Basmati Rice',
            'food_type': 'Veg',
            'category': 'Grains',
            'vaat_suitable': 'Yes',
            'pit_suitable': 'Yes',
            'cough_suitable': 'No',
            'vaat_rating': 5,
            'pit_rating': 4,
            'cough_rating': 2,
            'protein_g': 2.7
        },
        'ghee': {
            'food_name': 'Ghee',
            'food_type': 'Veg',
            'category': 'Dairy',
            'vaat_suitable': 'Yes',
            'pit_suitable': 'Yes',
            'cough_suitable': 'No',
            'vaat_rating': 5,
            'pit_rating': 4,
            'cough_rating': 1,
            'protein_g': 0.0
        }
    }

@pytest.fixture
def sample_medicines():
    """Sample known medicines for testing"""
    return {
        'ashwagandha': {
            'medicine_name': 'Ashwagandha',
            'formulation_type': 'Churna',
            'rasa_taste': 'Sweet, Bitter',
            'virya_potency': 'Hot',
            'vata_score': 8,
            'pitta_score': 3,
            'kapha_score': 2,
            'therapeutic_uses': 'Nervine tonic, stress relief, sleep aid'
        },
        'triphala': {
            'medicine_name': 'Triphala',
            'formulation_type': 'Churna',
            'rasa_taste': 'All five tastes except salty',
            'virya_potency': 'Cool',
            'vata_score': 5,
            'pitta_score': 5,
            'kapha_score': 5,
            'therapeutic_uses': 'Detoxification, digestion, eye health'
        },
        'tulsi': {
            'medicine_name': 'Tulsi',
            'formulation_type': 'Leaf',
            'rasa_taste': 'Pungent, Bitter',
            'virya_potency': 'Hot',
            'vata_score': 4,
            'pitta_score': 6,
            'kapha_score': 3,
            'therapeutic_uses': 'Respiratory health, cough, cold, fever'
        },
        'shatavari': {
            'medicine_name': 'Shatavari',
            'formulation_type': 'Root',
            'rasa_taste': 'Sweet',
            'virya_potency': 'Cool',
            'vata_score': 6,
            'pitta_score': 8,
            'kapha_score': 3,
            'therapeutic_uses': 'Acidity, ulcers, reproductive health'
        }
    }
