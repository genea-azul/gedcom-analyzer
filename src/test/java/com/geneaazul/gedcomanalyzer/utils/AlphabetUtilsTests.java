package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class AlphabetUtilsTests {

    private static final Pattern LATIN_CHARS_PATTERN = Pattern.compile("^[\\p{IsLatin} /ʺʹ’‘]+$");

    @Test
    public void testConvertAnyToLatinWhenEmpty() {
        assertThat(AlphabetUtils.convertAnyToLatin(null)).isNull();
        assertThat(AlphabetUtils.convertAnyToLatin("")).isEmpty();
    }

    @Test
    public void testConvertRussianToLatinUppercase() {
        String russianScript = "А Б В Г Д Е Ё Ж З И Й К Л М Н О П Р С Т У Ф Х Ц Ч Ш Щ Ъ Ы Ь Э Ю Я";
        String russianToLatin = AlphabetUtils.convertAnyToLatin(russianScript);

        assertThat(russianToLatin).isEqualTo("A B V G D Ye Ë ZH Z I Y K L M N O P R S T U F KH TS CH SH SHCH ʺ Y ʹ E YU YA");
        assertThat(LATIN_CHARS_PATTERN.matcher(russianToLatin).matches()).isTrue();
    }

    @Test
    public void testConvertRussianToLatinLowercase() {
        String russianScript = "а б в г д е ё ж з и й к л м н о п р с т у ф х ц ч ш щ ъ ы ь э ю я";
        String russianToLatin = AlphabetUtils.convertAnyToLatin(russianScript);

        assertThat(russianToLatin).isEqualTo("a b v g d ye yë zh z i y k l m n o p r s t u f kh ts ch sh shch ʺ y ʹ e yu ya");
        assertThat(LATIN_CHARS_PATTERN.matcher(russianToLatin).matches()).isTrue();
    }

    @Test
    public void testConvertGreekToLatinUppercase() {
        String greekScript = "Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω";
        String greekToLatin = AlphabetUtils.convertAnyToLatin(greekScript);

        assertThat(greekToLatin).isEqualTo("A V G DH E Z I TH I K L M N X O P R S T I F KH PS O");
        assertThat(LATIN_CHARS_PATTERN.matcher(greekToLatin).matches()).isTrue();
    }

    @Test
    public void testConvertGreekToLatinLowercase() {
        String greekScript = "α β γ δ ε ζ η θ ι κ λ μ ν ξ ο π ρ σ/ς τ υ φ χ ψ ω";
        String greekToLatin = AlphabetUtils.convertAnyToLatin(greekScript);

        assertThat(greekToLatin).isEqualTo("a v g dh e z i th i k l m n x o p r s/s t i f kh ps o");
        assertThat(LATIN_CHARS_PATTERN.matcher(greekToLatin).matches()).isTrue();
    }

    @Test
    public void testConvertHebrewToLatin() {
        String hebrewScript = "א ב ג ד ה ו ז ח ט י ך/כ ל ם/מ ן/נ ס ע ף/פ ץ/צ ק ר שׁ/שׂ ת";
        String hebrewToLatin = AlphabetUtils.convertAnyToLatin(hebrewScript);
        String hebrewToLatinStripAccents = StringUtils.stripAccents(hebrewToLatin);

        assertThat(hebrewToLatin).isEqualTo("’ v g d h w z ẖ t y kh/kh l m/m n/n s ‘ f/f ẕ/ẕ q r sh/s t");
        assertThat(hebrewToLatinStripAccents).isEqualTo("’ v g d h w z h t y kh/kh l m/m n/n s ‘ f/f z/z q r sh/s t");
        assertThat(LATIN_CHARS_PATTERN.matcher(hebrewToLatinStripAccents).matches()).isTrue();
    }

}
