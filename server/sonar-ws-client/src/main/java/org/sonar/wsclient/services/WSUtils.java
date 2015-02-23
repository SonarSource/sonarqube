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

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.Set;

/**
 * Compatibility layer between GWT and plain Java.
 * Well, this is bad, because code is not type-safe, so all unmarshallers also,
 * but this allows to remove duplications between sonar-gwt-api and sonar-ws-client.
 */
public abstract class WSUtils {

  private static volatile WSUtils instance = null;

  public static void setInstance(WSUtils utils) {
    instance = utils;
  }

  public static WSUtils getINSTANCE() {
    return instance;
  }

  public abstract String format(Date date, String format);

  public abstract String encodeUrl(String url);

  /**
   * @return value of specified field from specified JSON object,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract Object getField(Object json, String field);

  /**
   * @return value of a string field from specified JSON object,
   *         or string representation of a numeric field,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract String getString(Object json, String field);

  /**
   * @return Boolean value of specified field from specified JSON object,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract Boolean getBoolean(Object json, String field);

  /**
   * @return Integer value of specified field from specified JSON object,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract Integer getInteger(Object json, String field);

  /**
   * @return Double value of specified field from specified JSON object,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract Double getDouble(Object json, String field);

  /**
   * @return Long value of specified field from specified JSON object,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract Long getLong(Object json, String field);

  /**
   * @return Date value of specified field from specified JSON object,
   *         or <code>null</code> if field does not exist
   */
  @CheckForNull
  public abstract Date getDateTime(Object json, String field);

  /**
   * @return size of specified JSON array
   */
  public abstract int getArraySize(Object array);

  /**
   * @return element from specified JSON array
   */
  public abstract Object getArrayElement(Object array, int i);

  /**
   * @return JSON object
   */
  public abstract Object parse(String jsonStr);

  /**
   * @return field names in specified JSON object
   */
  public abstract Set<String> getFields(Object json);

}
