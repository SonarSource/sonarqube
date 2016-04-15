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
package org.sonar.api.server.ws;

import com.google.common.annotations.Beta;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;

/**
 * @since 4.2
 */
public abstract class Request {

  /**
   * Returns the name of the HTTP method with which this request was made. Possible
   * values are GET and POST. Others are not supported.
   */
  public abstract String method();

  /**
   * Returns the requested MIME type, or {@code "application/octet-stream"} if not specified.
   */
  public abstract String getMediaType();

  /**
   * Return true of the parameter is set.
   */
  public abstract boolean hasParam(String key);

  /**
   * Returns a non-null value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public String mandatoryParam(String key) {
    String value = param(key);
    if (value == null) {
      throw new IllegalArgumentException(String.format("The '%s' parameter is missing", key));
    }
    return value;
  }

  /**
   * Returns a boolean value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public boolean mandatoryParamAsBoolean(String key) {
    String s = mandatoryParam(key);
    return parseBoolean(key, s);
  }

  /**
   * Returns an int value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public int mandatoryParamAsInt(String key) {
    String s = mandatoryParam(key);
    return Integer.parseInt(s);
  }

  /**
   * Returns a long value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public long mandatoryParamAsLong(String key) {
    String s = mandatoryParam(key);
    return Long.parseLong(s);
  }

  public <E extends Enum<E>> E mandatoryParamAsEnum(String key, Class<E> enumClass) {
    return Enum.valueOf(enumClass, mandatoryParam(key));
  }

  public List<String> mandatoryParamAsStrings(String key) {
    List<String> values = paramAsStrings(key);
    if (values == null) {
      throw new IllegalArgumentException(String.format("The '%s' parameter is missing", key));
    }
    return values;
  }

  @CheckForNull
  public List<String> paramAsStrings(String key) {
    String value = param(key);
    if (value == null) {
      return null;
    }
    return Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(value));
  }

  @CheckForNull
  public abstract String param(String key);

  @CheckForNull
  public abstract InputStream paramAsInputStream(String key);

  /**
   * @deprecated to be dropped in 4.4. Default values are declared in ws metadata
   */
  @CheckForNull
  @Deprecated
  public String param(String key, @CheckForNull String defaultValue) {
    return StringUtils.defaultString(param(key), defaultValue);
  }

  /**
   * @deprecated to be dropped in 4.4. Default values must be declared in {@link org.sonar.api.server.ws.WebService} then
   * this method can be replaced by {@link #mandatoryParamAsBoolean(String)}.
   */
  @Deprecated
  public boolean paramAsBoolean(String key, boolean defaultValue) {
    String value = param(key);
    return value == null ? defaultValue : parseBoolean(key, value);
  }

  /**
   * @deprecated to be dropped in 4.4. Default values must be declared in {@link org.sonar.api.server.ws.WebService} then
   * this method can be replaced by {@link #mandatoryParamAsInt(String)}.
   */
  @Deprecated
  public int paramAsInt(String key, int defaultValue) {
    String s = param(key);
    return s == null ? defaultValue : Integer.parseInt(s);
  }

  /**
   * @deprecated to be dropped in 4.4. Default values must be declared in {@link org.sonar.api.server.ws.WebService} then
   * this method can be replaced by {@link #mandatoryParamAsLong(String)}.
   */
  @Deprecated
  public long paramAsLong(String key, long defaultValue) {
    String s = param(key);
    return s == null ? defaultValue : Long.parseLong(s);
  }

  @CheckForNull
  public Boolean paramAsBoolean(String key) {
    String value = param(key);
    return value == null ? null : parseBoolean(key, value);
  }

  @CheckForNull
  public Integer paramAsInt(String key) {
    String s = param(key);
    return s == null ? null : Integer.parseInt(s);
  }

  @CheckForNull
  public Long paramAsLong(String key) {
    String s = param(key);
    return s == null ? null : Long.parseLong(s);
  }

  @CheckForNull
  public <E extends Enum<E>> E paramAsEnum(String key, Class<E> enumClass) {
    String s = param(key);
    return s == null ? null : Enum.valueOf(enumClass, s);
  }

  @CheckForNull
  public <E extends Enum<E>> List<E> paramAsEnums(String key, Class<E> enumClass) {
    String value = param(key);
    if (value == null) {
      return null;
    }
    Iterable<String> values = Splitter.on(',').omitEmptyStrings().trimResults().split(value);
    List<E> result = new ArrayList<>();
    for (String s : values) {
      result.add(Enum.valueOf(enumClass, s));
    }

    return result;
  }

  @CheckForNull
  public Date paramAsDateTime(String key) {
    String s = param(key);
    if (s != null) {
      try {
        return DateUtils.parseDateTime(s);
      } catch (SonarException notDateTime) {
        try {
          return DateUtils.parseDate(s);
        } catch (SonarException notDateEither) {
          throw new SonarException(String.format("'%s' cannot be parsed as either a date or date+time", s));
        }
      }
    }
    return null;
  }

  @CheckForNull
  public Date paramAsDate(String key) {
    String s = param(key);
    if (s != null) {
      return DateUtils.parseDate(s);
    }
    return null;
  }

  private static boolean parseBoolean(String key, String value) {
    if ("true".equals(value) || "yes".equals(value)) {
      return true;
    }
    if ("false".equals(value) || "no".equals(value)) {
      return false;
    }
    throw new IllegalArgumentException(String.format("Property %s is not a boolean value: %s", key, value));
  }

  /**
   * Allows a web service to call another web service.
   * @see LocalConnector
   * @since 5.5
   */
  @Beta
  public abstract LocalConnector localConnector();
}
