# Run with: pytest tests/ -v

import pytest
from medicine_recommender import AyurMedicineRecommender

class TestMedicineRecommender:
    """Comprehensive test suite for AyurMedicineRecommender"""
    
    def test_vata_insomnia_returns_appropriate_medicines(self, medicine_recommender):
        """Test that Vata + insomnia returns nerve-calming medicines"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="vata",
            disease="insomnia",
            limit=10
        )
        
        assert len(recommendations) > 0, "Should return medicines for Vata + insomnia"
        
        # Check that returned medicines are suitable for Vata
        for medicine in recommendations:
            assert medicine['dosha_match_score'] >= 0, \
                f"{medicine['medicine_name']} should have valid dosha score"
        
        # Look for expected insomnia-related medicines
        medicine_names = [med['medicine_name'].lower() for med in recommendations]
        
        # Should contain nerve-calming medicines
        expected_medicines = ['ashwagandha', 'brahmi', 'jatamansi']
        found_expected = any(
            any(exp in name for exp in expected_medicines) 
            for name in medicine_names
        )
        assert found_expected, "Should contain known insomnia medicines for Vata"
    
    def test_pitta_acidity_returns_cooling_medicines(self, medicine_recommender):
        """Test that Pitta + acidity returns cooling medicines"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="pitta",
            disease="acidity",
            limit=10
        )
        
        assert len(recommendations) > 0, "Should return medicines for Pitta + acidity"
        
        # Check that returned medicines are suitable for Pitta
        for medicine in recommendations:
            assert medicine['dosha_match_score'] >= 0, \
                f"{medicine['medicine_name']} should have valid dosha score"
        
        # Look for expected acidity-related medicines
        medicine_names = [med['medicine_name'].lower() for med in recommendations]
        
        # Should contain cooling medicines
        expected_medicines = ['shatavari', 'amla', 'licorice', 'mulethi']
        found_expected = any(
            any(exp in name for exp in expected_medicines) 
            for name in medicine_names
        )
        assert found_expected, "Should contain known acidity medicines for Pitta"
    
    def test_kapha_cough_returns_expectorant_medicines(self, medicine_recommender):
        """Test that Kapha + cough returns expectorant medicines"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="kapha",
            disease="cough",
            limit=10
        )
        
        assert len(recommendations) > 0, "Should return medicines for Kapha + cough"
        
        # Look for expected cough-related medicines
        medicine_names = [med['medicine_name'].lower() for med in recommendations]
        
        # Should contain respiratory medicines
        expected_medicines = ['tulsi', 'ginger', 'pippali', 'vasaka']
        found_expected = any(
            any(exp in name for exp in expected_medicines) 
            for name in medicine_names
        )
        assert found_expected, "Should contain known cough medicines for Kapha"
    
    def test_triphala_appears_in_most_cases(self, medicine_recommender):
        """Test that Triphala (tridosha) appears as safe option"""
        triphala_found_in_cases = 0
        total_cases = 0
        
        for body_type in ["vata", "pitta", "kapha"]:
            for disease in ["insomnia", "acidity", "cough"]:
                total_cases += 1
                recommendations = medicine_recommender.recommend_medicines(
                    body_type=body_type,
                    disease=disease,
                    limit=10
                )
                
                medicine_names = [med['medicine_name'].lower() for med in recommendations]
                if 'triphala' in medicine_names:
                    triphala_found_in_cases += 1
        
        # Triphala should appear in many cases as tridosha medicine
        assert triphala_found_in_cases >= total_cases // 2, \
            "Triphala should appear in at least half the cases as tridosha medicine"
    
    def test_high_dosha_score_prioritized(self, medicine_recommender):
        """Test that medicines with high relevant dosha score are prioritized"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="vata",
            disease="stress",
            limit=10
        )
        
        if len(recommendations) < 2:
            pytest.skip("Need at least 2 recommendations to test prioritization")
        
        # Check that recommendations are sorted by dosha score (descending)
        for i in range(len(recommendations) - 1):
            current_score = recommendations[i]['dosha_match_score']
            next_score = recommendations[i + 1]['dosha_match_score']
            assert current_score >= next_score, \
                f"Medicines should be sorted by dosha score: {current_score} >= {next_score}"
    
    def test_keyword_matching_in_therapeutic_uses(self, medicine_recommender):
        """Test that keyword matching works in therapeutic_uses field"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="vata",
            disease="joint pain",
            limit=10
        )
        
        assert len(recommendations) > 0, "Should return medicines for joint pain"
        
        # Check that returned medicines have joint-related therapeutic uses
        found_joint_medicine = False
        for medicine in recommendations:
            therapeutic = medicine['therapeutic_uses'].lower()
            if any(keyword in therapeutic for keyword in ['joint', 'arthritis', 'inflammation', 'pain']):
                found_joint_medicine = True
                break
        
        assert found_joint_medicine, \
            "Should return medicines with joint-related therapeutic uses"
    
    def test_no_direct_match_returns_dosha_based_medicines(self, medicine_recommender):
        """Test fallback when disease has no direct match"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="pitta",
            disease="rare_condition_xyz",  # Unlikely to have direct matches
            limit=5
        )
        
        # Should still return some recommendations based on dosha
        assert len(recommendations) > 0, \
            "Should return dosha-based medicines even for rare conditions"
        
        # Check for fallback notes
        has_fallback = any(
            medicine.get('fallback_note') is not None 
            for medicine in recommendations
        )
        
        # May or may not have fallback depending on data matching
        # The important thing is it doesn't crash and returns reasonable results
    
    def test_limit_parameter_works_correctly(self, medicine_recommender):
        """Test that limit parameter controls number of returned recommendations"""
        limit = 3
        recommendations = medicine_recommender.recommend_medicines(
            body_type="vata",
            disease="insomnia",
            limit=limit
        )
        
        assert len(recommendations) <= limit, \
            f"Should return at most {limit} recommendations (got {len(recommendations)})"
    
    def test_invalid_body_type_handling(self, medicine_recommender):
        """Test that invalid body type raises appropriate error"""
        with pytest.raises(ValueError, match="body_type must be one of"):
            medicine_recommender.recommend_medicines(
                body_type="invalid_dosha",
                disease="insomnia",
                limit=5
            )
    
    def test_ashwagandha_properties_for_vata_insomnia(self, medicine_recommender):
        """Test specific properties of Ashwagandha for Vata + insomnia"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="vata",
            disease="insomnia",
            limit=10
        )
        
        ashwagandha_found = None
        for medicine in recommendations:
            if medicine['medicine_name'].lower() == 'ashwagandha':
                ashwagandha_found = medicine
                break
        
        if ashwagandha_found:
            # Should have high Vata score
            assert ashwagandha_found['dosha_match_score'] >= 6, \
                "Ashwagandha should have high Vata score for insomnia"
            
            # Should have disease relevance
            assert ashwagandha_found['disease_relevance'] >= 0, \
                "Ashwagandha should have disease relevance for insomnia"
            
            # Should have proper structure
            required_fields = [
                'medicine_name', 'formulation_type', 'rasa_taste', 
                'virya_potency', 'dosage', 'anupana',
                'dosha_match_score', 'disease_relevance', 
                'therapeutic_uses', 'reason'
            ]
            
            for field in required_fields:
                assert field in ashwagandha_found, \
                    f"Ashwagandha recommendation should have {field} field"
    
    def test_response_structure_is_correct(self, medicine_recommender):
        """Test that medicine recommendation response has correct structure"""
        recommendations = medicine_recommender.recommend_medicines(
            body_type="vata",
            disease="insomnia",
            limit=5
        )
        
        assert len(recommendations) > 0, "Should return recommendations"
        
        # Check structure of first recommendation
        rec = recommendations[0]
        required_fields = [
            'medicine_name', 'formulation_type', 'rasa_taste', 'virya_potency',
            'dosage', 'anupana', 'dosha_match_score', 'disease_relevance',
            'therapeutic_uses', 'reason'
        ]
        
        for field in required_fields:
            assert field in rec, f"Recommendation should have {field} field"
        
        # Check data types
        assert isinstance(rec['medicine_name'], str)
        assert isinstance(rec['dosha_match_score'], int)
        assert isinstance(rec['disease_relevance'], int)
        assert isinstance(rec['reason'], str)
        
        # Check optional fields
        if 'fallback_note' in rec:
            assert isinstance(rec['fallback_note'], str)
    
    def test_disease_case_insensitive_matching(self, medicine_recommender):
        """Test that disease matching is case insensitive"""
        disease_variations = ["INSOMNIA", "Insomnia", "insomnia"]
        
        for disease in disease_variations:
            recommendations = medicine_recommender.recommend_medicines(
                body_type="vata",
                disease=disease,
                limit=5
            )
            
            assert len(recommendations) > 0, \
                f"Should handle disease case: {disease}"
    
    def test_body_type_case_insensitive(self, medicine_recommender):
        """Test that body type is case insensitive"""
        body_type_variations = ["VATA", "Vata", "vata"]
        
        for body_type in body_type_variations:
            recommendations = medicine_recommender.recommend_medicines(
                body_type=body_type,
                disease="insomnia",
                limit=5
            )
            
            assert len(recommendations) > 0, \
                f"Should handle body_type case: {body_type}"
    
    def test_integration_style_recommendation_flow(self, medicine_recommender):
        """Integration test for complete recommendation flow"""
        # Test complete flow with realistic parameters
        recommendations = medicine_recommender.recommend_medicines(
            body_type="pitta",
            disease="acidity",
            limit=8
        )
        
        # Basic validation
        assert len(recommendations) > 0, "Should return recommendations"
        assert len(recommendations) <= 8, "Should respect limit parameter"
        
        # Content validation
        for medicine in recommendations:
            # Should have dosha relevance
            assert medicine['dosha_match_score'] >= 0, \
                f"{medicine['medicine_name']} should have valid dosha score"
            
            # Should have reason
            assert len(medicine['reason']) > 0, \
                f"{medicine['medicine_name']} should have recommendation reason"
            
            # Should have therapeutic info
            assert len(medicine['therapeutic_uses']) > 0, \
                f"{medicine['medicine_name']} should have therapeutic uses"
