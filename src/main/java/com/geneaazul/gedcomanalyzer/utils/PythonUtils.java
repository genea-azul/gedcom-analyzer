package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PythonUtils {

    public static String getPython3Command() {
        String os = System.getProperty("os.name");
        if (StringUtils.startsWithIgnoreCase(os, "Windows")) {
            return "python";
        }
        return "python3";
    }

}
