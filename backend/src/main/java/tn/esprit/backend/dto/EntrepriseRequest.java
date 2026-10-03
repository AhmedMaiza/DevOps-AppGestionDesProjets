package tn.esprit.backend.dto;

import tn.esprit.backend.entity.Entreprise;

public record EntrepriseRequest(Long id, String nom, String adresse) {

    public Entreprise toEntity() {
        Entreprise e = new Entreprise();
        e.setId(id);
        e.setNom(nom);
        e.setAdresse(adresse);
        return e;
    }
}
