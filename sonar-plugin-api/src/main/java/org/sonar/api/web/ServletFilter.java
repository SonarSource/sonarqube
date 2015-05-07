/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.web;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ServerSide;

import javax.servlet.Filter;

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
    return UrlPattern.create("/*");
  }

  public static final class UrlPattern {
    private int code;
    private String url;
    private String urlToMatch;

    public static UrlPattern create(String pattern) {
      return new UrlPattern(pattern);
    }

    private UrlPattern(String url) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "Empty url");
      this.url = url;
      this.urlToMatch = url.replaceAll("/?\\*", "");
      if ("/*".equals(url)) {
        code = 1;
      } else if (url.startsWith("*")) {
        code = 2;
      } else if (url.endsWith("*")) {
        code = 3;
      } else {
        code = 4;
      }
    }

    public boolean matches(String path) {
      switch (code) {
        case 1:
          return true;
        case 2:
          return path.endsWith(urlToMatch);
        case 3:
          return path.startsWith(urlToMatch);
        default:
          return path.equals(urlToMatch);
      }
    }

    public String getUrl() {
      return url;
    }

    @Override
    public String toString() {
      return url;
    }
  }
}
