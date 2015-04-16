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
package org.sonar.api.server.ws.internal;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @since 4.2
 */
public abstract class ValidatingRequest extends Request {

  private WebService.Action action;

  public void setAction(WebService.Action action) {
    this.action = action;
  }

  public WebService.Action action() {
    return action;
  }

  @Override
  @CheckForNull
  public String param(String key) {
    return param(key, true);
  }

  @Override
  @CheckForNull
  public InputStream paramAsInputStream(String key) {
    return readInputStreamParam(key);
  }

  @CheckForNull
  private String param(String key, boolean validateValue) {
    WebService.Param definition = action.param(key);
    String value = readParamOrDefaultValue(key, definition);
    if (value != null && validateValue) {
      validate(value, definition);
    }
    return value;
  }

  @CheckForNull
  @Override
  public List<String> paramAsStrings(String key) {
    WebService.Param definition = action.param(key);
    String value = readParamOrDefaultValue(key, definition);
    if (value == null) {
      return null;
    }
    List<String> values = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(value));
    for (String s : values) {
      validate(s, definition);
    }
    return values;
  }

  @CheckForNull
  @Override
  public <E extends Enum<E>> List<E> paramAsEnums(String key, Class<E> enumClass) {
    WebService.Param definition = action.param(key);
    String value = readParamOrDefaultValue(key, definition);
    if (value == null) {
      return null;
    }
    Iterable<String> values = Splitter.on(',').omitEmptyStrings().trimResults().split(value);
    List<E> result = new ArrayList<>();
    for (String s : values) {
      validate(s, definition);
      result.add(Enum.valueOf(enumClass, s));
    }
    return result;
  }

  @CheckForNull
  private String readParamOrDefaultValue(String key, @Nullable WebService.Param definition) {
    if (definition == null) {
      String message = String.format("BUG - parameter '%s' is undefined for action '%s'", key, action.key());
      Loggers.get(getClass()).error(message);
      throw new IllegalArgumentException(message);
    }
    String deprecatedKey = definition.deprecatedKey();
    String value = deprecatedKey != null ? StringUtils.defaultString(readParam(deprecatedKey), readParam(key)) : readParam(key);
    value = StringUtils.defaultString(value, definition.defaultValue());
    if (value == null) {
      return null;
    }
    return value;
  }

  @CheckForNull
  protected abstract String readParam(String key);

  @CheckForNull
  protected abstract InputStream readInputStreamParam(String key);

  private void validate(String value, WebService.Param definition) {
    Set<String> possibleValues = definition.possibleValues();
    if (possibleValues != null && !possibleValues.contains(value)) {
      throw new IllegalArgumentException(String.format(
        "Value of parameter '%s' (%s) must be one of: %s", definition.key(), value, possibleValues));
    }
  }
}
