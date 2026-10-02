package tn.esprit.backend.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.dto.EntrepriseRequest;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.service.IEntrepriseService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseControllerTest {

    @Mock
    IEntrepriseService entrepriseService;

    @InjectMocks
    EntrepriseController controller;

    @Test
    void addEntrepriseConvertitLeDtoEtDelegueAuService() {
        Entreprise entite = new Entreprise();
        when(entrepriseService.addEntreprise(any(Entreprise.class))).thenReturn(entite);

        assertSame(entite, controller.addEntreprise(new EntrepriseRequest(1L, "Esprit", "Ariana")));
        verify(entrepriseService).addEntreprise(any(Entreprise.class));
    }

    @Test
    void updateEntrepriseConvertitLeDtoEtDelegueAuService() {
        Entreprise entite = new Entreprise();
        when(entrepriseService.updateEntreprise(any(Entreprise.class))).thenReturn(entite);

        assertSame(entite, controller.updateEntreprise(new EntrepriseRequest(1L, "Esprit", "Ariana")));
        verify(entrepriseService).updateEntreprise(any(Entreprise.class));
    }

    @Test
    void deleteEntrepriseDelegueAuService() {
        controller.deleteEntreprise(4L);

        verify(entrepriseService).deleteEntreprise(4L);
    }

    @Test
    void getEntrepriseByIdDelegueAuService() {
        Entreprise entite = new Entreprise();
        when(entrepriseService.getEntrepriseById(4L)).thenReturn(entite);

        assertSame(entite, controller.getEntrepriseById(4L));
    }

    @Test
    void getAllEntreprisesDelegueAuService() {
        List<Entreprise> liste = List.of(new Entreprise());
        when(entrepriseService.getAllEntreprises()).thenReturn(liste);

        assertSame(liste, controller.getAllEntreprises());
    }
}
