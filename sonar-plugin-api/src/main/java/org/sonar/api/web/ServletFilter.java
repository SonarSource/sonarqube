/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

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

    private final Predicate<String>[] inclusions;
    private final Predicate<String>[] exclusions;

    private UrlPattern(Builder builder) {
      if (builder.inclusions.isEmpty()) {
        // because Stream#anyMatch() returns false if stream is empty
        this.inclusions = new Predicate[] {s -> true};
      } else {
        this.inclusions = builder.inclusions.stream().toArray(Predicate[]::new);
      }
      this.exclusions = builder.exclusions.stream().toArray(Predicate[]::new);
    }

    public boolean matches(String path) {
      return !Arrays.stream(exclusions).anyMatch(pattern -> pattern.test(path)) &&
        Arrays.stream(inclusions).anyMatch(pattern -> pattern.test(path));
    }

    /**
     * @return always return empty string {@code ""} since version 6.0
     * @deprecated since 6.0 as multiple inclusion and exclusion patterns can be defined
     */
    @Deprecated
    public String getUrl() {
      return "";
    }

    public static UrlPattern create(String inclusionPattern) {
      return builder().includes(inclusionPattern).build();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private static final String WILDCARD_CHAR = "*";
      private static final Collection<String> STATIC_RESOURCES = ImmutableList.of("/css/*", "/fonts/*", "/images/*", "/js/*", "/static/*");

      private final Set<Predicate<String>> inclusions = new HashSet<>();
      private final Set<Predicate<String>> exclusions = new HashSet<>();

      private Builder() {
      }

      public static Collection<String> staticResourcePatterns() {
        return STATIC_RESOURCES;
      }

      public Builder includes(String... includePatterns) {
        return includes(asList(includePatterns));
      }

      public Builder includes(Collection<String> includePatterns) {
        this.inclusions.addAll(includePatterns.stream()
          .filter(pattern -> !MATCH_ALL.equals(pattern))
          .map(Builder::compile)
          .collect(Collectors.toList()));
        return this;
      }

      public Builder excludes(String... excludePatterns) {
        return excludes(asList(excludePatterns));
      }

      public Builder excludes(Collection<String> excludePatterns) {
        this.exclusions.addAll(excludePatterns.stream()
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
          String path = pattern.replaceAll("/?\\*", "");
          return url -> url.startsWith(path);
        }
        checkArgument(pattern.startsWith(WILDCARD_CHAR), "URL pattern must start with wildcard character '*': %s", pattern);
        String path = pattern.substring(1);
        return url -> url.endsWith(path);
      }
    }
  }
}
