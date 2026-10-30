package com.pos.gateway.filter;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/** Matches request paths against the configured public (no JWT needed) path patterns. */
public final class PublicPaths {

    private final List<PathPattern> patterns;

    public PublicPaths(String commaSeparated) {
        PathPatternParser parser = new PathPatternParser();
        this.patterns = Arrays.stream(commaSeparated == null ? new String[0] : commaSeparated.split(","))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .map(parser::parse)
                .toList();
    }

    public boolean matches(String path) {
        org.springframework.http.server.PathContainer container =
                org.springframework.http.server.PathContainer.parsePath(path);
        return patterns.stream().anyMatch(p -> p.matches(container));
    }
}
