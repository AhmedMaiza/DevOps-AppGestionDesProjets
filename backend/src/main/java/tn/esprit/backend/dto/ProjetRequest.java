package tn.esprit.backend.dto;

import tn.esprit.backend.entity.Projet;

public record ProjetRequest(Long id, String sujet) {

    public Projet toEntity() {
        Projet p = new Projet();
        p.setId(id);
        p.setSujet(sujet);
        return p;
    }
}
