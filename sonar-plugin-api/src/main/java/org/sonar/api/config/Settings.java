/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.config;

import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * @deprecated since 6.5 use {@link Configuration}. Implementation moved out of the API in 8.3. Only remains minimal interface to make some outdated plugins happy.
 */
@ServerSide
@ComputeEngineSide
@ScannerSide
@SonarLintSide
@Deprecated
public abstract class Settings {

  /**
   * @return {@code true} if the property has a non-default value, else {@code false}.
   */
  public abstract boolean hasKey(String key);

  /**
   * The effective value of the specified property. Can return
   * {@code null} if the property is not set and has no
   * defined default value.
   * <p>
   * If the property is encrypted with a secret key,
   * then the returned value is decrypted.
   * </p>
   *
   * @throws IllegalStateException if value is encrypted but fails to be decrypted.
   */
  @CheckForNull
  public abstract String getString(String key);

  /**
   * Effective value as boolean. It is {@code false} if {@link #getString(String)}
   * does not return {@code "true"}, even if it's not a boolean representation.
   *
   * @return {@code true} if the effective value is {@code "true"}, else {@code false}.
   */
  public abstract boolean getBoolean(String key);

  /**
   * Effective value as {@code int}.
   *
   * @return the value as {@code int}. If the property does not have value nor default value, then {@code 0} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable integer
   */
  public abstract int getInt(String key);

  /**
   * Effective value as {@code long}.
   *
   * @return the value as {@code long}. If the property does not have value nor default value, then {@code 0L} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable {@code long}
   */
  public abstract long getLong(String key);

  /**
   * Effective value as {@link Date}, without time fields. Format is {@link DateUtils#DATE_FORMAT}.
   *
   * @return the value as a {@link Date}. If the property does not have value nor default value, then {@code null} is returned.
   * @throws RuntimeException if value is not empty and is not in accordance with {@link DateUtils#DATE_FORMAT}.
   */
  @CheckForNull
  public abstract Date getDate(String key);

  /**
   * Effective value as {@link Date}, with time fields. Format is {@link DateUtils#DATETIME_FORMAT}.
   *
   * @return the value as a {@link Date}. If the property does not have value nor default value, then {@code null} is returned.
   * @throws RuntimeException if value is not empty and is not in accordance with {@link DateUtils#DATETIME_FORMAT}.
   */
  @CheckForNull
  public abstract Date getDateTime(String key);

  /**
   * Effective value as {@code Float}.
   *
   * @return the value as {@code Float}. If the property does not have value nor default value, then {@code null} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable number
   */
  @CheckForNull
  public abstract Float getFloat(String key);

  /**
   * Effective value as {@code Double}.
   *
   * @return the value as {@code Double}. If the property does not have value nor default value, then {@code null} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable number
   */
  @CheckForNull
  public abstract Double getDouble(String key);

  /**
   * Value is split by comma and trimmed. Never returns null.
   * <br>
   * Examples :
   * <ul>
   * <li>"one,two,three " -&gt; ["one", "two", "three"]</li>
   * <li>"  one, two, three " -&gt; ["one", "two", "three"]</li>
   * <li>"one, , three" -&gt; ["one", "", "three"]</li>
   * </ul>
   */
  public abstract String[] getStringArray(String key);

  /**
   * Value is split by carriage returns.
   *
   * @return non-null array of lines. The line termination characters are excluded.
   * @since 3.2
   */
  public abstract String[] getStringLines(String key);

  /**
   * Value is split and trimmed.
   */
  public abstract String[] getStringArrayBySeparator(String key, String separator);

  public abstract List<String> getKeysStartingWith(String prefix);

}
