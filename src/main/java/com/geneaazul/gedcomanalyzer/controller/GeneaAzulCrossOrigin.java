package com.geneaazul.gedcomanalyzer.controller;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CORS policy for endpoints called by the public website: the production domains plus the
 * Cloudflare Pages project ({@code geneaazul-web.pages.dev}) and its preview deployments
 * ({@code <hash|branch>.geneaazul-web.pages.dev}).
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CrossOrigin(originPatterns = {
        "http://geneaazul.com.ar:[*]",
        "https://geneaazul.com.ar:[*]",
        "http://*.geneaazul.com.ar:[*]",
        "https://*.geneaazul.com.ar:[*]",
        "https://geneaazul-web.pages.dev",
        "https://*.geneaazul-web.pages.dev",
})
public @interface GeneaAzulCrossOrigin {

    @AliasFor(annotation = CrossOrigin.class)
    String[] exposedHeaders() default {};

}
