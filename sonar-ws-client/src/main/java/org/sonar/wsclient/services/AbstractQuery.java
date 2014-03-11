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
package org.sonar.wsclient.services;

import javax.annotation.Nullable;

import java.util.Date;

/**
 * @since 2.2
 */
public abstract class AbstractQuery<M extends Model> {

  /**
   * Default timeout for waiting data, in milliseconds.
   *
   * @since 2.10
   */
  public static final int DEFAULT_TIMEOUT_MILLISECONDS = 30 * 1000;

  private int timeoutMilliseconds = DEFAULT_TIMEOUT_MILLISECONDS;

  // accepted-language as defined in http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
  private String locale;

  /**
   * Must start with a slash, for example: /api/metrics
   * <p>
   * IMPORTANT: In implementations of this method we must use helper methods to construct URL.
   * </p>
   *
   * @see #encode(String)
   * @see #appendUrlParameter(StringBuilder, String, Object)
   * @see #appendUrlParameter(StringBuilder, String, Object[])
   * @see #appendUrlParameter(StringBuilder, String, Date, boolean)
   */
  public abstract String getUrl();

  /**
   * Request body. By default it is empty but it can be overridden.
   */
  public String getBody() {
    return null;
  }

  /**
   * Get the timeout for waiting data, in milliseconds. A value of zero is interpreted as an infinite timeout.
   *
   * @since 2.10
   */
  public final int getTimeoutMilliseconds() {
    return timeoutMilliseconds;
  }

  /**
   * Set the timeout for waiting data, in milliseconds. Avalue of zero is interpreted as an infinite timeout.
   *
   * @since 2.10
   */
  public final AbstractQuery<M> setTimeoutMilliseconds(int i) {
    this.timeoutMilliseconds = i<0 ? 0 : i;
    return this;
  }

  /**
   * Accepted-language, as defined in http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
   *
   * @since 2.10
   */
  public final String getLocale() {
    return locale;
  }

  /**
   * Set the Accepted-language HTTP parameter
   *
   * @since 2.10
   */
  public final AbstractQuery<M> setLocale(String locale) {
    this.locale = locale;
    return this;
  }

  /**
   * Encodes single parameter value.
   */
  protected static String encode(String value) {
    return WSUtils.getINSTANCE().encodeUrl(value);
  }

  protected static void appendUrlParameter(StringBuilder url, String paramKey, int paramValue) {
    url.append(paramKey)
      .append('=')
      .append(paramValue)
      .append("&");
  }

  protected static void appendUrlParameter(StringBuilder url, String paramKey, @Nullable Object paramValue) {
    if (paramValue != null) {
      url.append(paramKey)
        .append('=')
        .append(encode(paramValue.toString()))
        .append('&');
    }
  }

  protected static void appendUrlParameter(StringBuilder url, String paramKey, @Nullable Object[] paramValues) {
    if (paramValues != null) {
      url.append(paramKey).append('=');
      for (int index = 0; index < paramValues.length; index++) {
        if (index > 0) {
          url.append(',');
        }
        if (paramValues[index] != null) {
          url.append(encode(paramValues[index].toString()));
        }
      }
      url.append('&');
    }
  }

  protected static void appendUrlParameter(StringBuilder url, String paramKey, @Nullable Date paramValue, boolean includeTime) {
    if (paramValue != null) {
      String format = includeTime ? "yyyy-MM-dd'T'HH:mm:ssZ" : "yyyy-MM-dd";
      url.append(paramKey)
        .append('=')
        .append(encode(WSUtils.getINSTANCE().format(paramValue, format)))
        .append('&');
    }
  }
}
