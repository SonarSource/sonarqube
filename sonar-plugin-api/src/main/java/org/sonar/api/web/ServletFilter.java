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
import static org.sonar.api.web.ServletFilter.UrlPattern.Pattern.Type.END_WITH_START;
import static org.sonar.api.web.ServletFilter.UrlPattern.Pattern.Type.MATCH_ALL;
import static org.sonar.api.web.ServletFilter.UrlPattern.Pattern.Type.START_WITH_STAR;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final String MATCH_ALL_URL = "/*";

    private final boolean matchAllUrls;
    private final List<Pattern> includePatterns = new ArrayList<>();
    private final List<Pattern> excludePatterns = new ArrayList<>();

    private UrlPattern(Builder builder) {
      this.includePatterns.addAll(builder.includePatterns.stream().map(Pattern::new).collect(Collectors.toList()));
      this.excludePatterns.addAll(builder.excludePatterns.stream().map(Pattern::new).collect(Collectors.toList()));
      this.matchAllUrls = excludePatterns.isEmpty() && includePatterns.size() == 1 && includePatterns.get(0).getUrl().equals(MATCH_ALL_URL);
    }

    public boolean matches(String path) {
      // Optimization for filter that match all urls
      return matchAllUrls ||
      // Otherwise we first verify if the url is not excluded, then we verify if the url is included
        (!excludePatterns.stream().anyMatch(pattern -> pattern.matches(path)) && includePatterns.stream().anyMatch(pattern -> pattern.matches(path)));
    }

    public Set<String> getIncludePatterns() {
      return includePatterns.stream().map(Pattern::getUrl).collect(Collectors.toSet());
    }

    public Set<String> getExcludePatterns() {
      return excludePatterns.stream().map(Pattern::getUrl).collect(Collectors.toSet());
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
        // By default, every urls is accepted
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

    static class Pattern {

      enum Type {
        MATCH_ALL, START_WITH_STAR, END_WITH_START, ANY
      }

      private String url;
      private String urlToMatch;
      private Type urlType;

      Pattern(String url) {
        this.url = url;
        this.urlToMatch = url.replaceAll("/?\\*", "");
        if (MATCH_ALL_URL.equals(url)) {
          this.urlType = MATCH_ALL;
        } else if (url.startsWith("*")) {
          this.urlType = START_WITH_STAR;
        } else if (url.endsWith("*")) {
          this.urlType = END_WITH_START;
        } else {
          this.urlType = Type.ANY;
        }
      }

      boolean matches(String path) {
        switch (urlType) {
          case MATCH_ALL:
            return true;
          case START_WITH_STAR:
            return path.endsWith(urlToMatch);
          case END_WITH_START:
            return path.startsWith(urlToMatch);
          default:
            return path.equals(urlToMatch);
        }
      }

      String getUrl() {
        return url;
      }
    }

  }
}
