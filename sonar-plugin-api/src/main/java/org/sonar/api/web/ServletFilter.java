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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private enum PatternType {
      MATCH_ALL, START_WITH_STAR, END_WITH_START, ANY
    }

    private static final String MATCH_ALL_URL = "/*";

    private final boolean matchAllUrls;
    private final Map<String, UrlWithType> includePatterns = new HashMap<>();
    private final Map<String, UrlWithType> excludePatterns = new HashMap<>();

    private UrlPattern(Builder builder) {
      for (String includePattern : builder.includePatterns) {
        includePatterns.put(includePattern, new UrlWithType(includePattern));
      }
      for (String excludePattern : builder.excludePatterns) {
        excludePatterns.put(excludePattern, new UrlWithType(excludePattern));
      }
      this.matchAllUrls = excludePatterns.isEmpty() && includePatterns.size() == 1 && includePatterns.keySet().iterator().next().equals(MATCH_ALL_URL);
    }

    public boolean matches(String path) {
      // Optimization for filter that match all urls
      if (matchAllUrls) {
        return true;
      }
      for (Map.Entry<String, UrlWithType> excludePattern : excludePatterns.entrySet()) {
        if (matches(path, excludePattern.getValue())) {
          return false;
        }
      }
      for (Map.Entry<String, UrlWithType> includePattern : includePatterns.entrySet()) {
        if (matches(path, includePattern.getValue())) {
          return true;
        }
      }
      return false;
    }

    private static boolean matches(String path, UrlWithType urlWithType) {
      switch (urlWithType.getPatternType()) {
        case MATCH_ALL:
          return true;
        case START_WITH_STAR:
          return path.endsWith(urlWithType.getUrlToMatch());
        case END_WITH_START:
          return path.startsWith(urlWithType.getUrlToMatch());
        default:
          return path.equals(urlWithType.getUrlToMatch());
      }
    }

    public Set<String> getIncludePatterns() {
      return includePatterns.keySet();
    }

    public Set<String> getExcludePatterns() {
      return excludePatterns.keySet();
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
        this.includePatterns.add(MATCH_ALL_URL);
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

    private static class UrlWithType {
      private String urlToMatch;
      private PatternType urlType;

      UrlWithType(String url) {
        this.urlToMatch = url.replaceAll("/?\\*", "");
        if ("/*".equals(url)) {
          this.urlType = PatternType.MATCH_ALL;
        } else if (url.startsWith("*")) {
          this.urlType = PatternType.START_WITH_STAR;
        } else if (url.endsWith("*")) {
          this.urlType = PatternType.END_WITH_START;
        } else {
          this.urlType = PatternType.ANY;
        }
      }

      String getUrlToMatch() {
        return urlToMatch;
      }

      PatternType getPatternType() {
        return urlType;
      }
    }

  }
}
