package com.moodtune.recommendation;

import com.moodtune.model.Catalogue;
import com.moodtune.model.Morceau;

import java.util.List;

public final class MockRecommandeur implements Recommandeur {
    @Override
    public List<Morceau> recommander(String humeur, Catalogue catalogue) {
        return HumeurMatcher.candidats(humeur, catalogue);
    }

    @Override
    public List<Morceau> recommanderParmi(String humeur, List<Morceau> candidats) {
        return candidats.stream().limit(5).toList();
    }
}
