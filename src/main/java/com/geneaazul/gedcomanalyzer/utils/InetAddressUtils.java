package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InetAddressUtils {

    public static final Set<String> LOCALHOST_ADDRESSES = Set.of();

    public static String getRemoteAddress(HttpServletRequest request) {
        String remoteAddr = StringUtils.trimToNull(request.getHeader("X-FORWARDED-FOR"));
        if (remoteAddr == null) {
            remoteAddr = StringUtils.trimToNull(request.getRemoteAddr());
        }
        return remoteAddr == null || LOCALHOST_ADDRESSES.contains(remoteAddr) ? null : remoteAddr;
    }

}
