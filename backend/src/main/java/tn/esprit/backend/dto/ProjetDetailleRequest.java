package tn.esprit.backend.dto;

import java.time.LocalDate;

import tn.esprit.backend.entity.ProjetDetaille;

public record ProjetDetailleRequest(Long id, String description, String technologie,
                                    Double coutProvisoire, LocalDate dateDebut) {

    public ProjetDetaille toEntity() {
        ProjetDetaille d = new ProjetDetaille();
        d.setId(id);
        d.setDescription(description);
        d.setTechnologie(technologie);
        d.setCoutProvisoire(coutProvisoire);
        d.setDateDebut(dateDebut);
        return d;
    }
}
