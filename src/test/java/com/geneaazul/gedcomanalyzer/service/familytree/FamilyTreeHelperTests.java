package com.geneaazul.gedcomanalyzer.service.familytree;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.GivenName;
import com.geneaazul.gedcomanalyzer.model.Surname;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyTreeHelperTests {

    @Mock
    private com.geneaazul.gedcomanalyzer.service.PersonService personService;

    @InjectMocks
    private FamilyTreeHelper familyTreeHelper;

    @Test
    void getFamilyTreeFileId_withGivenNameAndSurname_returnsConcatenatedSimplified() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.of(GivenName.of("Juan", "juan", "juan")));
        when(person.getSurname()).thenReturn(Optional.of(Surname.of("Pérez", "perez", "perez", "perez")));
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("juan_perez");
    }

    @Test
    void getFamilyTreeFileId_withSpacesInNames_replacesWithUnderscores() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.of(GivenName.of("Mary Jane", "mary jane", "mary jane")));
        when(person.getSurname()).thenReturn(Optional.of(Surname.of("Van Der Berg", "van der berg", "berg", "berg_")));
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("mary_jane_van_der_berg");
    }

    @Test
    void getFamilyTreeFileId_withOnlyGivenName_returnsSimplifiedGivenName() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.of(GivenName.of("Anonymous", "anonymous", "anonymous")));
        when(person.getSurname()).thenReturn(Optional.empty());
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("anonymous");
    }

    @Test
    void getFamilyTreeFileId_withNoName_returnsDefault() {
        EnrichedPerson person = org.mockito.Mockito.mock(EnrichedPerson.class);
        when(person.getGivenName()).thenReturn(Optional.empty());
        when(person.getSurname()).thenReturn(Optional.empty());
        assertThat(familyTreeHelper.getFamilyTreeFileId(person)).isEqualTo("genea-azul");
    }
}
