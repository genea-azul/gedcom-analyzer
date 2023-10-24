package com.geneaazul.gedcomanalyzer.mapper;

import com.geneaazul.gedcomanalyzer.model.EnrichedPerson;
import com.geneaazul.gedcomanalyzer.model.EnrichedSpouseWithChildren;
import com.geneaazul.gedcomanalyzer.model.FormattedRelationship;
import com.geneaazul.gedcomanalyzer.model.Place;
import com.geneaazul.gedcomanalyzer.model.Relationship;
import com.geneaazul.gedcomanalyzer.model.dto.AdoptionType;
import com.geneaazul.gedcomanalyzer.model.dto.ReferenceType;
import com.geneaazul.gedcomanalyzer.model.dto.RelationshipDto;
import com.geneaazul.gedcomanalyzer.model.dto.SexType;
import com.geneaazul.gedcomanalyzer.model.dto.TreeSideType;
import com.geneaazul.gedcomanalyzer.utils.PersonUtils;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

@Component
public class RelationshipMapper {

    public RelationshipDto toRelationshipDto(
            @Nullable Relationship relationship,
            boolean obfuscateCondition) {

        if (relationship == null) {
            return null;
        }

        ReferenceType referenceType;
        int generation = relationship.getGeneration();
        int grade;

        if (generation > 0) {
            grade = relationship.distanceToAncestorThisPerson();
            referenceType = grade == 0 ? ReferenceType.PARENT : ReferenceType.PIBLING;
        } else if (generation == 0) {
            grade = relationship.distanceToAncestorRootPerson() - 1;
            referenceType = grade < 0
                    ? (relationship.isInLaw() ? ReferenceType.SPOUSE : ReferenceType.SELF)
                    : (grade == 0 ? ReferenceType.SIBLING : ReferenceType.COUSIN);
        } else {
            generation = -generation;
            grade = relationship.distanceToAncestorRootPerson();
            referenceType = grade == 0 ? ReferenceType.CHILD : ReferenceType.NIBLING;
        }

        SexType spouseSex = null;
        boolean isSeparated = false;
        if (relationship.isInLaw()) {
            Integer relatedPersons = Optional
                    .ofNullable(relationship.relatedPersonIds())
                    .map(List::size)
                    .orElse(0);
            if (relatedPersons == 1) {
                String spouseId = relationship.relatedPersonIds().get(0);
                EnrichedPerson spouse = relationship
                        .person()
                        .getGedcom()
                        .getPersonById(spouseId);
                //noinspection DataFlowIssue
                spouseSex = spouse.getSex();
                isSeparated = relationship
                    .person()
                        .getSpousesWithChildren()
                        .stream()
                        .filter(spouseWithChildren -> spouseWithChildren
                                .getSpouse()
                                .map(EnrichedPerson::getId)
                                .map(spouseId::equals)
                                .orElse(false))
                        .findFirst()
                        .map(EnrichedSpouseWithChildren::isSeparated)
                        .orElse(false);
            }
        }

        return RelationshipDto.builder()
                .personSex(relationship.person().getSex())
                .personIsAlive(relationship.person().isAlive())
                .personName(PersonUtils.obfuscateName(relationship.person(), obfuscateCondition))
                .personYearOfBirth(relationship.person().getDateOfBirth()
                        .map(date -> obfuscateCondition ? -1 : date.getYear().getValue())
                        .orElse(null))
                .personYearOfBirthIsAbout(relationship.person().getDateOfBirth()
                        .map(dateOfBirth -> dateOfBirth.getOperator() != null)
                        .orElse(false))
                .personCountryOfBirth(relationship
                        .person()
                        .getPlaceOfBirth()
                        .map(Place::country)
                        .orElse(null))
                .referenceType(referenceType)
                .generation(generation)
                .grade(grade)
                .isInLaw(relationship.isInLaw())
                .isHalf(relationship.isHalf())
                .adoptionType(relationship.adoptionType())
                .spouseSex(spouseSex)
                .isSeparated(isSeparated)
                .treeSides(relationship.treeSides())
                .isObfuscated(obfuscateCondition)
                .build();
    }

    public FormattedRelationship formatInSpanish(RelationshipDto relationship, int index, boolean onlySecondaryDescription) {
        String personSex = displaySex(relationship.getPersonSex());
        String treeSide = displayTreeSide(relationship.getTreeSides());
        String personIsAlive = relationship.getPersonIsAlive() ? " " : "✝";
        String personName = displayNameInSpanish(relationship.getPersonName());
        String personYearOfBirth = Optional.ofNullable(relationship.getPersonYearOfBirth())
                .map(yearOfBirth -> displayYear(yearOfBirth, relationship.getPersonYearOfBirthIsAbout()))
                .orElse(null);
        String personCountryOfBirth = relationship.getPersonCountryOfBirth();
        String adoption = Optional.ofNullable(relationship.getAdoptionType())
                .map(AdoptionType::name)
                .orElse(null);
        String relationshipDesc = displayRelationshipInSpanish(relationship, onlySecondaryDescription);
        return new FormattedRelationship(
                String.valueOf(index),
                personName,
                personSex,
                personIsAlive,
                personYearOfBirth,
                personCountryOfBirth,
                adoption,
                treeSide,
                relationshipDesc,
                relationship.getIsObfuscated());
    }

    public String displayNameInSpanish(String name) {
        // Only the name is private, surname is not obfuscated
        name = name.replace("<private>", "<nombre privado>");
        name = name.replace("<no name>", "<nombre desconocido>");
        return name.replace("<no spouse>", "<sin pareja>");
    }

    public String displaySex(SexType sex) {
        return switch (sex) {
            case F -> "♀";
            case M -> "♂";
            default -> " ";
        };
    }

    public String displayTreeSide(@Nullable Set<TreeSideType> treeSides) {
        if (treeSides == null) {
            return " ";
        }
        if (treeSides.size() == 1) {
            TreeSideType treeSide = treeSides.stream().findFirst().orElseThrow();
            return switch (treeSide) {
                case FATHER -> "←";
                case MOTHER -> "→";
                case DESCENDANT -> "↓";
                case SPOUSE -> "◇";
            };
        }
        if (treeSides.size() == 2) {
            if (treeSides.contains(TreeSideType.DESCENDANT)) {
                if (treeSides.contains(TreeSideType.FATHER)) {
                    return "↙";
                }
                if (treeSides.contains(TreeSideType.MOTHER)) {
                    return "↘";
                }
            } else if (treeSides.contains(TreeSideType.SPOUSE)) {
                if (treeSides.contains(TreeSideType.FATHER)) {
                    return "◁";
                }
                if (treeSides.contains(TreeSideType.MOTHER)) {
                    return "▷";
                }
            } else {
                return "↔";
            }
        } else if (treeSides.size() == 3) {
            if (treeSides.contains(TreeSideType.SPOUSE)) {
                return "▽";
            }
            if (treeSides.contains(TreeSideType.DESCENDANT)) {
                return "⇊";
            }
        }
        return " ";
    }

    private String displayYear(Integer year, @Nullable Boolean yearIsAbout) {
        if (year == -1) {
            return "----"; // obfuscated
        }
        return Boolean.TRUE.equals(yearIsAbout)
                ? "~" + year
                : year.toString();
    }

    public String displayRelationshipInSpanish(RelationshipDto relationship, boolean onlySecondaryDescription) {
        if (relationship.getReferenceType() == ReferenceType.SELF) {
            return "persona principal";
        }

        String separated = (relationship.getIsSeparated() ? "ex-" : "");

        if (relationship.getReferenceType() == ReferenceType.SPOUSE) {
            return separated + "pareja";
        }

        String spousePrefix = (relationship.getIsInLaw() ? separated + "pareja de " : "");

        if (relationship.getReferenceType() == ReferenceType.PARENT) {
            if (relationship.getGeneration() == 1) {
                String sexSuffix = getSexSuffixInSpanish(relationship);
                String relationshipName = sexSuffix.equals("o") ? "padre" : "madre";
                String adoptionSuffix = getAdoptionSuffixInSpanish(relationship.getAdoptionType(), sexSuffix);
                return spousePrefix + relationshipName + adoptionSuffix;
            }

            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGeneration() - 4, sexSuffix);

            String relationshipName;
            if (relationship.getGeneration() == 2) {
                relationshipName = "abuel" + sexSuffix;
            } else if (relationship.getGeneration() == 3) {
                relationshipName = "bisabuel" + sexSuffix;
            } else if (relationship.getGeneration() == 4) {
                relationshipName = "tatarabuel" + sexSuffix;
            } else {
                relationshipName = "trastatarabuel" + sexSuffix;
            }

            String or = "";
            if (relationship.getGeneration() >= 6) {
                or = spousePrefix + "ancestro directo de " + relationship.getGeneration() + " generaciones";
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            return spousePrefix + relationshipName + gradeSuffix + or;
        }

        if (relationship.getReferenceType() == ReferenceType.CHILD) {
            if (relationship.getGeneration() == 1) {
                if (relationship.getIsInLaw() && relationship.getAdoptionType() == null) {
                    return relationship.getPersonSex() == SexType.M ? separated + "yerno" : separated + "nuera";
                }
                String sexSuffix = getSexSuffixInSpanish(relationship);
                String adoptionSuffix = getAdoptionSuffixInSpanish(relationship.getAdoptionType(), sexSuffix);
                return spousePrefix + "hij" + sexSuffix + adoptionSuffix;
            }

            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGeneration() - 4, sexSuffix);

            String relationshipName;
            if (relationship.getGeneration() == 2) {
                relationshipName = "niet" + sexSuffix;
            } else if (relationship.getGeneration() == 3) {
                relationshipName = "bisniet" + sexSuffix;
            } else if (relationship.getGeneration() == 4) {
                relationshipName = "tataraniet" + sexSuffix;
            } else {
                relationshipName = "trastataraniet" + sexSuffix;
            }

            String or = "";
            if (relationship.getGeneration() >= 6) {
                or = spousePrefix + "descendiente directo de " + relationship.getGeneration() + " generaciones";
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            return spousePrefix + relationshipName + gradeSuffix + or;
        }

        if (relationship.getReferenceType() == ReferenceType.SIBLING) {
            if (relationship.getIsInLaw() && !relationship.getIsHalf()) {
                return relationship.getPersonSex() == SexType.M ? separated + "cuñado" : separated + "cuñada";
            }

            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);

            String relationshipName = "herman" + sexSuffix;
            return spousePrefix + halfPrefix + relationshipName;
        }

        if (relationship.getReferenceType() == ReferenceType.COUSIN) {
            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGrade(), sexSuffix);

            String relationshipName = "prim" + sexSuffix;
            return spousePrefix + halfPrefix + relationshipName + gradeSuffix;
        }

        if (relationship.getReferenceType() == ReferenceType.PIBLING) {
            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGrade(), sexSuffix);

            String relationshipName1 = "tí" + sexSuffix + (relationship.getGeneration() > 1 ? "-" : "");
            String relationshipName2;

            if (relationship.getGeneration() == 1) {
                relationshipName2 = "";
            } else if (relationship.getGeneration() == 2) {
                relationshipName2 = "abuel" + sexSuffix;
            } else if (relationship.getGeneration() == 3) {
                relationshipName2 = "bisabuel" + sexSuffix;
            } else if (relationship.getGeneration() == 4) {
                relationshipName2 = "tatarabuel" + sexSuffix;
            } else {
                relationshipName2 = "trastatarabuel" + sexSuffix;
            }

            String or = "";
            if (relationship.getGeneration() > 2 || relationship.getGrade() >= 2) {
                String relationshipNameOr1 = (relationshipName2.isEmpty())
                        ? getTreeSideInSpanish(relationship.getTreeSides(), "padre/madre")
                        : relationshipName2.substring(0, relationshipName2.length() - 1) + "o/a";
                String relationshipNameOr2;
                if (relationship.getGrade() == 1) {
                    relationshipNameOr2 = "herman" + sexSuffix;
                } else {
                    relationshipNameOr2 = "prim" + sexSuffix;
                }
                String gradeSuffixOr = getGradeSuffixInSpanish(relationship.getGrade() - 1, sexSuffix);
                or = spousePrefix + halfPrefix + relationshipNameOr2 + gradeSuffixOr + " de " + relationshipNameOr1;
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            return spousePrefix + halfPrefix + relationshipName1 + relationshipName2 + gradeSuffix + or;
        }

        if (relationship.getReferenceType() == ReferenceType.NIBLING) {
            String halfPrefix = relationship.getIsHalf() ? "medio-" : "";
            String sexSuffix = getSexSuffixInSpanish(relationship);
            String gradeSuffix = getGradeSuffixInSpanish(relationship.getGrade(), sexSuffix);

            String relationshipName1 = "sobrin" + sexSuffix + (relationship.getGeneration() > 1 ? "-" : "");
            String relationshipName2;

            if (relationship.getGeneration() == 1) {
                relationshipName2 = "";
            } else if (relationship.getGeneration() == 2) {
                relationshipName2 = "niet" + sexSuffix;
            } else if (relationship.getGeneration() == 3) {
                relationshipName2 = "bisniet" + sexSuffix;
            } else if (relationship.getGeneration() == 4) {
                relationshipName2 = "tataraniet" + sexSuffix;
            } else {
                relationshipName2 = "trastataraniet" + sexSuffix;
            }

            String or = "";
            if (relationship.getGeneration() > 2 || relationship.getGrade() >= 2) {
                String relationshipNameOr1 = (relationshipName2.isEmpty()) ? "hij" + sexSuffix : relationshipName2;
                String relationshipNameOr2;
                if (relationship.getGrade() == 1) {
                    relationshipNameOr2 = "hermano/a";
                } else {
                    relationshipNameOr2 = "primo/a";
                }
                String gradeSuffixOr = getGradeSuffixInSpanish(relationship.getGrade() - 1, "o/a");
                or = spousePrefix + relationshipNameOr1 + " de " + halfPrefix + relationshipNameOr2 + gradeSuffixOr;
                if (onlySecondaryDescription) {
                    return or;
                }
                or = "  (" + or + ")";
            }

            return spousePrefix + halfPrefix + relationshipName1 + relationshipName2 + gradeSuffix + or;
        }

        return "familiar";
    }

    private String getTreeSideInSpanish(
            @Nullable Set<TreeSideType> treeSides,
            @SuppressWarnings("SameParameterValue") String defaultValue) {
        if (treeSides == null) {
            return defaultValue;
        }
        if (treeSides.containsAll(List.of(TreeSideType.FATHER, TreeSideType.MOTHER))) {
            return "padre/madre";
        }
        if (treeSides.contains(TreeSideType.FATHER)) {
            return "padre";
        }
        if (treeSides.contains(TreeSideType.MOTHER)) {
            return "madre";
        }
        return defaultValue;
    }

    private String getSexSuffixInSpanish(RelationshipDto relationship) {
        if (relationship.getIsInLaw()) {
            return (relationship.getSpouseSex() == SexType.M ? "o" : "a");
        } else {
            return (relationship.getPersonSex() == SexType.M ? "o" : "a");
        }
    }

    private String getGradeSuffixInSpanish(int grade, String sexSuffix) {
        if (grade <= 1) {
            return "";
        }
        if (grade == 2) {
            return " segund" + sexSuffix;
        }
        if (grade == 3) {
            return " tercer" + sexSuffix;
        }
        if (grade == 4) {
            return " cuart" + sexSuffix;
        }
        if (grade == 5) {
            return " quint" + sexSuffix;
        }
        if (grade == 6) {
            return " sext" + sexSuffix;
        }
        if (grade == 7) {
            return " séptim" + sexSuffix;
        }
        if (grade == 8) {
            return " octav" + sexSuffix;
        }
        if (grade == 9) {
            return " noven" + sexSuffix;
        }
        return " de " + grade + "° grado";
    }

    private String getAdoptionSuffixInSpanish(@Nullable AdoptionType adoptionType, String sexSuffix) {
        if (adoptionType == null) {
            return "";
        }
        return switch (adoptionType) {
            case ADOPTIVE -> " adoptiv" + sexSuffix;
            case FOSTER -> " de crianza";
        };
    }

}
