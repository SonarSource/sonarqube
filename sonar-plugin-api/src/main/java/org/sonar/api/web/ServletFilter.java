/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @since 3.1
 */
@ServerSide
@ExtensionPoint
public abstract class ServletFilter implements Filter {

  /**
   * Override to change URL. Default is /*
   */
  public UrlPattern doGetPattern() {
    return UrlPattern.builder().build();
  }

  public static final class UrlPattern {

    private static final String MATCH_ALL = "/*";

    private final List<String> inclusions;
    private final List<String> exclusions;
    private final Predicate<String>[] inclusionPredicates;
    private final Predicate<String>[] exclusionPredicates;

    private UrlPattern(Builder builder) {
      this.inclusions = unmodifiableList(new ArrayList<>(builder.inclusions));
      this.exclusions = unmodifiableList(new ArrayList<>(builder.exclusions));
      if (builder.inclusionPredicates.isEmpty()) {
        // because Stream#anyMatch() returns false if stream is empty
        this.inclusionPredicates = new Predicate[] {s -> true};
      } else {
        this.inclusionPredicates = builder.inclusionPredicates.stream().toArray(Predicate[]::new);
      }
      this.exclusionPredicates = builder.exclusionPredicates.stream().toArray(Predicate[]::new);
    }

    public boolean matches(String path) {
      return !Arrays.stream(exclusionPredicates).anyMatch(pattern -> pattern.test(path)) &&
        Arrays.stream(inclusionPredicates).anyMatch(pattern -> pattern.test(path));
    }

    /**
     * @since 6.0
     */
    public Collection<String> getInclusions() {
      return inclusions;
    }

    /**
     * @since 6.0
     */
    public Collection<String> getExclusions() {
      return exclusions;
    }

    /**
     * @deprecated replaced in version 6.0 by {@link #getInclusions()} and {@link #getExclusions()}
     * @throws IllegalStateException if at least one exclusion or more than one inclusions are defined
     */
    @Deprecated
    public String getUrl() {
      // Before 6.0, it was only possible to include one url
      if (exclusions.isEmpty() && inclusions.size() == 1) {
        return inclusions.get(0);
      }
      throw new IllegalStateException("this method is deprecated and should not be used anymore");
    }

    public String label() {
      return "UrlPattern{" +
        "inclusions=[" + convertPatternsToString(inclusions) + "]" +
        ", exclusions=[" + convertPatternsToString(exclusions) + "]" +
        '}';
    }

    private static String convertPatternsToString(List<String> input) {
      StringBuilder output = new StringBuilder();
      if (input.isEmpty()) {
        return "";
      }
      if (input.size() == 1) {
        return output.append(input.get(0)).toString();
      }
      return output.append(input.get(0)).append(", ...").toString();
    }

    /**
     * Defines only a single inclusion pattern. This is a shortcut for {@code builder().includes(inclusionPattern).build()}.
     */
    public static UrlPattern create(String inclusionPattern) {
      return builder().includes(inclusionPattern).build();
    }

    /**
     * @since 6.0
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * @since 6.0
     */
    public static class Builder {
      private static final String WILDCARD_CHAR = "*";
      private static final Collection<String> STATIC_RESOURCES = unmodifiableList(asList("/css/*", "/fonts/*", "/images/*", "/js/*", "/static/*",
        "/robots.txt", "/favicon.ico", "/apple-touch-icon*", "/mstile*"));

      private final Set<String> inclusions = new LinkedHashSet<>();
      private final Set<String> exclusions = new LinkedHashSet<>();
      private final Set<Predicate<String>> inclusionPredicates = new HashSet<>();
      private final Set<Predicate<String>> exclusionPredicates = new HashSet<>();

      private Builder() {
      }

      public static Collection<String> staticResourcePatterns() {
        return STATIC_RESOURCES;
      }

      /**
       * Add inclusion patterns. Supported formats are:
       * <ul>
       *   <li>path prefixed by / and ended by *, for example "/api/foo/*", to match all paths "/api/foo" and "api/api/foo/something/else"</li>
       *   <li>path prefixed by *, for example "*\/foo", to match all paths "/api/foo" and "something/else/foo"</li>
       *   <li>path with leading slash and no wildcard, for example "/api/foo", to match exact path "/api/foo"</li>
       * </ul>
       */
      public Builder includes(String... includePatterns) {
        return includes(asList(includePatterns));
      }

      /**
       * Add exclusion patterns. See format described in {@link #includes(String...)}
       */
      public Builder includes(Collection<String> includePatterns) {
        this.inclusions.addAll(includePatterns);
        this.inclusionPredicates.addAll(includePatterns.stream()
          .filter(pattern -> !MATCH_ALL.equals(pattern))
          .map(Builder::compile)
          .collect(Collectors.toList()));
        return this;
      }

      public Builder excludes(String... excludePatterns) {
        return excludes(asList(excludePatterns));
      }

      public Builder excludes(Collection<String> excludePatterns) {
        this.exclusions.addAll(excludePatterns);
        this.exclusionPredicates.addAll(excludePatterns.stream()
          .map(Builder::compile)
          .collect(Collectors.toList()));
        return this;
      }

      public UrlPattern build() {
        return new UrlPattern(this);
      }

      private static Predicate<String> compile(String pattern) {
        int countStars = pattern.length() - pattern.replace(WILDCARD_CHAR, "").length();
        if (countStars == 0) {
          checkArgument(pattern.startsWith("/"), "URL pattern must start with slash '/': %s", pattern);
          return url -> url.equals(pattern);
        }
        checkArgument(countStars == 1, "URL pattern accepts only zero or one wildcard character '*': %s", pattern);
        if (pattern.charAt(0) == '/') {
          checkArgument(pattern.endsWith(WILDCARD_CHAR), "URL pattern must end with wildcard character '*': %s", pattern);
          // remove the ending /* or *
          String path = pattern.replaceAll("/?\\*", "");
          return url -> url.startsWith(path);
        }
        checkArgument(pattern.startsWith(WILDCARD_CHAR), "URL pattern must start with wildcard character '*': %s", pattern);
        // remove the leading *
        String path = pattern.substring(1);
        return url -> url.endsWith(path);
      }
    }
  }
}
