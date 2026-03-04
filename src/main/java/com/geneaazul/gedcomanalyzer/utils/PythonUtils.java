package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.Strings;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PythonUtils {

    public static String getPython3Command() {
        String os = System.getProperty("os.name");
        if (os != null && Strings.CI.startsWith(os, "Windows")) {
            return "python";
        }
        return "python3";
    }

}
