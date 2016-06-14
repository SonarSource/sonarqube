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
import static com.google.common.collect.ImmutableSet.copyOf;

import java.util.HashSet;
import java.util.Set;
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

    private final Set<String> includePatterns;
    private final Set<String> excludePatterns;

    private UrlPattern(Builder builder) {
      this.includePatterns = builder.includePatterns;
      this.excludePatterns = builder.excludePatterns;
    }

    public boolean matches(String path) {
      return !excludePatterns.stream().anyMatch(pattern -> matches(path, pattern))
        && includePatterns.stream().anyMatch(pattern -> matches(path, pattern));
    }

    private static boolean matches(String path, String pattern) {
      String urlToMatch = getUrlToMatch(pattern);
      if ("/*".equals(pattern)) {
        return true;
      } else if (pattern.startsWith("*")) {
        return path.endsWith(urlToMatch);
      } else if (pattern.endsWith("*")) {
        return path.startsWith(urlToMatch);
      } else {
        return path.equals(urlToMatch);
      }
    }

    private static String getUrlToMatch(String url) {
      return url.replaceAll("/?\\*", "");
    }

    public Set<String> getIncludePatterns() {
      return includePatterns;
    }

    public Set<String> getExcludePatterns() {
      return excludePatterns;
    }

    @Override
    public String toString() {
      return "UrlPattern{" +
        "includePatterns=" + includePatterns +
        ", excludePatterns=" + excludePatterns +
        '}';
    }

    public static UrlPattern create(String pattern) {
      return new UrlPattern.Builder().setIncludePatterns(pattern).build();
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private Set<String> includePatterns = new HashSet<>();
      private Set<String> excludePatterns = new HashSet<>();

      private Builder() {
        this.includePatterns = new HashSet<>();
        this.includePatterns.add("/*");
        this.excludePatterns = new HashSet<>();
      }

      public Builder setIncludePatterns(String... includePatterns) {
        this.includePatterns = copyOf(includePatterns);
        return this;
      }

      public Builder setExcludePatterns(String... excludePatterns) {
        this.excludePatterns = copyOf(excludePatterns);
        return this;
      }

      public UrlPattern build() {
        checkArgument(!includePatterns.isEmpty() || !excludePatterns.isEmpty(), "Empty urls");
        checkNoEmptyValue(includePatterns);
        checkNoEmptyValue(excludePatterns);
        return new UrlPattern(this);
      }

      private static void checkNoEmptyValue(Set<String> list) {
        checkArgument(!list.stream().anyMatch(String::isEmpty), "Empty url");
      }
    }

  }
}
