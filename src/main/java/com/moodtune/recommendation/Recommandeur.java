package com.moodtune.recommendation;

import com.moodtune.model.Catalogue;
import com.moodtune.model.Morceau;

import java.util.List;

public interface Recommandeur {
    List<Morceau> recommander(String humeur, Catalogue catalogue);
}
