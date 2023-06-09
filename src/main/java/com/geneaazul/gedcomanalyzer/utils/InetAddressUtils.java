package com.geneaazul.gedcomanalyzer.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InetAddressUtils {

    public static final Set<String> LOCALHOST_ADDRESSES = Set.of(
            "0:0:0:0:0:0:0:1",
            "::1",
            "127.0.0.1",
            "localhost");

    public static Optional<String> getRemoteAddress(HttpServletRequest request) {
        return Optional.ofNullable(StringUtils.trimToNull(request.getHeader("X-REAL-IP")))
                .or(() -> Optional.ofNullable(StringUtils.trimToNull(request.getHeader("X-CLIENT-IP"))))
                .or(() -> Optional.ofNullable(StringUtils.trimToNull(request.getHeader("X-FORWARDED-FOR"))))
                .or(() -> Optional.ofNullable(StringUtils.trimToNull(request.getRemoteAddr())))
                .filter(remoteAddress -> !LOCALHOST_ADDRESSES.contains(remoteAddress));
    }

}
