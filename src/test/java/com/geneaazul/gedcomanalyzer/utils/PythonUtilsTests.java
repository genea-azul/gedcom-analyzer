package com.geneaazul.gedcomanalyzer.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PythonUtilsTests {

    @Test
    void getPython3Command_returnsPythonOrPython3() {
        // We cannot mock os.name easily; just assert we get one of the two valid values
        String cmd = PythonUtils.getPython3Command();
        assertThat(cmd).isIn("python", "python3");
    }
}
