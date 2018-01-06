/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import static java.lang.String.format;
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
    WebService.Param definition = action.param(key);

    String rawValue = readParam(key, definition);
    String rawValueOrDefault = defaultString(rawValue, definition.defaultValue());
    String value = rawValueOrDefault == null ? null : CharMatcher.WHITESPACE.trimFrom(rawValueOrDefault);
    validateRequiredValue(key, definition, rawValue);
    if (value == null) {
      return null;
    }
    validatePossibleValues(key, value, definition);
    validateMaximumLength(key, definition, rawValueOrDefault);
    validateMinimumLength(key, definition, rawValueOrDefault);
    validateMaximumValue(key, definition, value);
    return value;
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
  @Override
  public List<String> paramAsStrings(String key) {
    WebService.Param definition = action.param(key);
    String value = defaultString(readParam(key, definition), definition.defaultValue());
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
  private String readParam(String key, @Nullable WebService.Param definition) {
    checkArgument(definition != null, "BUG - parameter '%s' is undefined for action '%s'", key, action.key());
    String deprecatedKey = definition.deprecatedKey();
    return deprecatedKey != null ? defaultString(readParam(deprecatedKey), readParam(key)) : readParam(key);
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
    values.forEach(value -> validatePossibleValues(definition.key(), value, definition));
    return values;
  }

  private static void validatePossibleValues(String key, String value, WebService.Param definition) {
    Set<String> possibleValues = definition.possibleValues();
    if (possibleValues == null) {
      return;
    }
    checkArgument(possibleValues.contains(value), "Value of parameter '%s' (%s) must be one of: %s", key, value, possibleValues);
  }

  private static void validateMaximumLength(String key, WebService.Param definition, String valueOrDefault) {
    Integer maximumLength = definition.maximumLength();
    if (maximumLength == null) {
      return;
    }
    int valueLength = valueOrDefault.length();
    checkArgument(valueLength <= maximumLength, "'%s' length (%s) is longer than the maximum authorized (%s)", key, valueLength, maximumLength);
  }

  private static void validateMinimumLength(String key, WebService.Param definition, String valueOrDefault) {
    Integer minimumLength = definition.minimumLength();
    if (minimumLength == null) {
      return;
    }
    int valueLength = valueOrDefault.length();
    checkArgument(valueLength >= minimumLength, "'%s' length (%s) is shorter than the minimum authorized (%s)", key, valueLength, minimumLength);
  }

  private static void validateMaximumValue(String key, WebService.Param definition, String value) {
    Integer maximumValue = definition.maximumValue();
    if (maximumValue == null) {
      return;
    }
    int valueAsInt = validateAsNumeric(key, value);
    checkArgument(valueAsInt <= maximumValue, "'%s' value (%s) must be less than %s", key, valueAsInt, maximumValue);
  }

  private static void validateRequiredValue(String key, WebService.Param definition, String value) {
    boolean required = definition.isRequired();
    if (required) {
      checkArgument(value != null, format(MSG_PARAMETER_MISSING, key));
    }
  }

  private static int validateAsNumeric(String key, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      throw new IllegalStateException(format("'%s' value '%s' cannot be parsed as an integer", key, value), exception);
    }
  }

}
