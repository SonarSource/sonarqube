/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.server.ws.internal;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.defaultString;

/**
 * @since 4.2
 */
public abstract class ValidatingRequest extends Request {

  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private WebService.Action action;
  private LocalConnector localConnector;

  public void setAction(WebService.Action action) {
    this.action = action;
  }

  public WebService.Action action() {
    return action;
  }

  @Override
  public LocalConnector localConnector() {
    requireNonNull(localConnector, "Local connector has not been set");
    return localConnector;
  }

  public void setLocalConnector(LocalConnector lc) {
    this.localConnector = lc;
  }

  @Override
  @CheckForNull
  public String param(String key) {
    return param(key, true);
  }

  @Override
  public List<String> multiParam(String key) {
    WebService.Param definition = action.param(key);
    List<String> values = readMultiParamOrDefaultValue(key, definition);
    return validateValues(values, definition);
  }

  @Override
  @CheckForNull
  public InputStream paramAsInputStream(String key) {
    return readInputStreamParam(key);
  }

  @Override
  @CheckForNull
  public Part paramAsPart(String key) {
    return readPart(key);
  }

  @CheckForNull
  private String param(String key, boolean validateValue) {
    WebService.Param definition = action.param(key);
    String value = readParamOrDefaultValue(key, definition);
    String trimmedValue = value == null ? null : CharMatcher.WHITESPACE.trimFrom(value);
    if (trimmedValue != null && validateValue) {
      validateValue(trimmedValue, definition);
    }
    return trimmedValue;
  }

  @CheckForNull
  @Override
  public List<String> paramAsStrings(String key) {
    WebService.Param definition = action.param(key);
    String value = readParamOrDefaultValue(key, definition);
    if (value == null) {
      return null;
    }
    List<String> values = COMMA_SPLITTER.splitToList(value);
    return validateValues(values, definition);
  }

  @CheckForNull
  @Override
  public <E extends Enum<E>> List<E> paramAsEnums(String key, Class<E> enumClass) {
    List<String> values = paramAsStrings(key);
    if (values == null) {
      return null;
    }
    return values.stream()
      .map(value -> Enum.valueOf(enumClass, value))
      .collect(Collectors.toList());
  }

  @CheckForNull
  private String readParamOrDefaultValue(String key, @Nullable WebService.Param definition) {
    checkArgument(definition != null, "BUG - parameter '%s' is undefined for action '%s'", key, action.key());

    String deprecatedKey = definition.deprecatedKey();
    String value = deprecatedKey != null ? defaultString(readParam(deprecatedKey), readParam(key)) : readParam(key);
    return defaultString(value, definition.defaultValue());
  }

  private List<String> readMultiParamOrDefaultValue(String key, @Nullable WebService.Param definition) {
    checkArgument(definition != null, "BUG - parameter '%s' is undefined for action '%s'", key, action.key());

    List<String> keyValues = readMultiParam(key);
    if (!keyValues.isEmpty()) {
      return keyValues;
    }

    String deprecatedKey = definition.deprecatedKey();
    List<String> deprecatedKeyValues = deprecatedKey == null ? emptyList() : readMultiParam(deprecatedKey);
    if (!deprecatedKeyValues.isEmpty()) {
      return deprecatedKeyValues;
    }

    String defaultValue = definition.defaultValue();
    return defaultValue == null ? emptyList() : singletonList(defaultValue);
  }

  @CheckForNull
  protected abstract String readParam(String key);

  protected abstract List<String> readMultiParam(String key);

  @CheckForNull
  protected abstract InputStream readInputStreamParam(String key);

  @CheckForNull
  protected abstract Part readPart(String key);

  private static List<String> validateValues(List<String> values, WebService.Param definition) {
    Integer maximumValues = definition.maxValuesAllowed();
    checkArgument(maximumValues == null || values.size() <= maximumValues, "'%s' can contains only %s values, got %s", definition.key(), maximumValues, values.size());
    values.forEach(value -> validateValue(value, definition));
    return values;
  }

  private static void validateValue(String value, WebService.Param definition) {
    Set<String> possibleValues = definition.possibleValues();
    checkArgument(possibleValues == null || possibleValues.contains(value), "Value of parameter '%s' (%s) must be one of: %s", definition.key(), value, possibleValues);
  }
}
