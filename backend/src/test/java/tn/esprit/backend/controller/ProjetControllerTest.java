package tn.esprit.backend.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.dto.ProjetRequest;
import tn.esprit.backend.entity.Projet;
import tn.esprit.backend.service.IProjetService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjetControllerTest {

    @Mock
    IProjetService projetService;

    @InjectMocks
    ProjetController controller;

    @Test
    void addProjetConvertitLeDtoEtDelegueAuService() {
        Projet entite = new Projet();
        when(projetService.addProjet(any(Projet.class))).thenReturn(entite);

        assertSame(entite, controller.addProjet(new ProjetRequest(1L, "Gestion")));
        verify(projetService).addProjet(any(Projet.class));
    }

    @Test
    void updateProjetConvertitLeDtoEtDelegueAuService() {
        Projet entite = new Projet();
        when(projetService.updateProjet(any(Projet.class))).thenReturn(entite);

        assertSame(entite, controller.updateProjet(new ProjetRequest(1L, "Gestion")));
        verify(projetService).updateProjet(any(Projet.class));
    }

    @Test
    void deleteProjetDelegueAuService() {
        controller.deleteProjet(4L);

        verify(projetService).deleteProjet(4L);
    }

    @Test
    void getProjetByIdDelegueAuService() {
        Projet entite = new Projet();
        when(projetService.getProjetById(4L)).thenReturn(entite);

        assertSame(entite, controller.getProjetById(4L));
    }

    @Test
    void getAllProjetsDelegueAuService() {
        List<Projet> liste = List.of(new Projet());
        when(projetService.getAllProjets()).thenReturn(liste);

        assertSame(liste, controller.getAllProjets());
    }
}
