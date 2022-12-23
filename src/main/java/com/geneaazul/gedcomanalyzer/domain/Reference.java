package com.geneaazul.gedcomanalyzer.domain;

import com.geneaazul.gedcomanalyzer.model.ReferenceType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.PersonFamilyCommonContainer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class Reference {

    private final PersonFamilyCommonContainer source;

    private final ReferenceType sourceType;

    private final PersonFamilyCommonContainer target;

    private final ReferenceType targetType;

    @Override
    public String toString() {
        return "["
                + sourceType + "(" + getId(source) + getDisplayValue(source, "-") + ") -> "
                + targetType + "(" + getId(target) + getDisplayValue(target, "-") + ")]";
    }

    private static String getId(PersonFamilyCommonContainer personOrFamily) {
        if (personOrFamily instanceof Person) {
            return ((Person) personOrFamily).getId();
        }
        if (personOrFamily instanceof Family) {
            return ((Family) personOrFamily).getId();
        }
        return "";
    }

    private static String getDisplayValue(PersonFamilyCommonContainer personOrFamily, String prefix) {
        if (personOrFamily instanceof Person) {
            String displayValue = PersonUtils.getDisplayName((Person) personOrFamily);
            return prefix + displayValue;
        }
        return "";
    }

}
