package tn.esprit.backend.dto;

import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.entity.Equipe;

public record EquipeRequest(Long id, String nom, String specialite, EntrepriseRef entreprise) {

    public record EntrepriseRef(Long id) { }

    public Equipe toEntity() {
        Equipe e = new Equipe();
        e.setId(id);
        e.setNom(nom);
        e.setSpecialite(specialite);
        if (entreprise != null && entreprise.id() != null) {
            Entreprise ent = new Entreprise();
            ent.setId(entreprise.id());
            e.setEntreprise(ent);
        }
        return e;
    }
}
