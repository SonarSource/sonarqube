/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

import java.util.Date;

/**
 * @since 2.2
 */
public abstract class AbstractQuery<MODEL extends Model> {

  /**
   * Must start with a slash, for example: /api/metrics
   */
  public abstract String getUrl();

  protected static void appendUrlParameter(StringBuilder url, String paramKey, Object paramValue) {
    if (paramValue != null) {
      url.append(paramKey)
          .append('=')
          .append(paramValue)
          .append('&');
    }
  }

  protected static void appendUrlParameter(StringBuilder url, String paramKey, Object[] paramValues) {
    if (paramValues != null) {
      url.append(paramKey).append('=');
      for (int index = 0; index < paramValues.length; index++) {
        if (index > 0) {
          url.append(',');
        }
        if (paramValues[index] != null) {
          url.append(paramValues[index]);
        }
      }
      url.append('&');
    }
  }

  protected static void appendUrlParameter(StringBuilder url, String paramKey, Date paramValue, boolean includeTime) {
    if (paramValue != null) {
      String format = (includeTime ? "yyyy-MM-dd'T'HH:mm:ssZ" : "yyyy-MM-dd");
      url.append(paramKey)
          .append('=')
          .append(WSUtils.getINSTANCE().encodeUrl(WSUtils.getINSTANCE().format(paramValue, format)))
          .append('&');
    }
  }
}