/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarqube.ws.MediaTypes;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.WsUtils.checkArgument;
import static org.sonarqube.ws.WsUtils.isNullOrEmpty;

abstract class BaseRequest<SELF extends BaseRequest> implements WsRequest {

  private final String path;

  private String mediaType = MediaTypes.JSON;

  private final DefaultParameters parameters = new DefaultParameters();
  private final DefaultHeaders headers = new DefaultHeaders();
  private OptionalInt timeOutInMs = OptionalInt.empty();

  BaseRequest(String path) {
    this.path = path;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getMediaType() {
    return mediaType;
  }

  @Override
  public OptionalInt getTimeOutInMs() {
    return timeOutInMs;
  }

  public SELF setTimeOutInMs(int timeOutInMs) {
    this.timeOutInMs = OptionalInt.of(timeOutInMs);
    return (SELF) this;
  }

  /**
   * Expected media type of response. Default is {@link MediaTypes#JSON}.
   */
  @SuppressWarnings("unchecked")
  public SELF setMediaType(String s) {
    requireNonNull(s, "media type of response cannot be null");
    this.mediaType = s;
    return (SELF) this;
  }

  public SELF setParam(String key, @Nullable String value) {
    return setSingleValueParam(key, value);
  }

  public SELF setParam(String key, @Nullable Integer value) {
    return setSingleValueParam(key, value);
  }

  public SELF setParam(String key, @Nullable Long value) {
    return setSingleValueParam(key, value);
  }

  public SELF setParam(String key, @Nullable Float value) {
    return setSingleValueParam(key, value);
  }

  public SELF setParam(String key, @Nullable Boolean value) {
    return setSingleValueParam(key, value);
  }

  @SuppressWarnings("unchecked")
  private SELF setSingleValueParam(String key, @Nullable Object value) {
    checkArgument(!isNullOrEmpty(key), "a WS parameter key cannot be null");
    if (value == null) {
      return (SELF) this;
    }
    parameters.setValue(key, value.toString());

    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  public SELF setParam(String key, @Nullable Collection<? extends Object> values) {
    checkArgument(!isNullOrEmpty(key), "a WS parameter key cannot be null");
    if (values == null || values.isEmpty()) {
      return (SELF) this;
    }

    parameters.setValues(key, values.stream()
      .filter(Objects::nonNull)
      .map(Object::toString)
      .collect(Collectors.toList()));

    return (SELF) this;
  }

  @Override
  public Map<String, String> getParams() {
    return parameters.keyValues.keySet().stream()
      .collect(Collectors.toMap(
        Function.identity(),
        key -> parameters.keyValues.get(key).get(0),
        (v1, v2) -> {
          throw new IllegalStateException(String.format("Duplicate key '%s' in request", v1));
        },
        LinkedHashMap::new));
  }

  @Override
  public Parameters getParameters() {
    return parameters;
  }

  @Override
  public Headers getHeaders() {
    return headers;
  }

  @SuppressWarnings("unchecked")
  public SELF setHeader(String name, @Nullable String value) {
    requireNonNull(name, "Header name can't be null");
    headers.setValue(name, value);
    return (SELF) this;
  }

  private static class DefaultParameters implements Parameters {
    // preserve insertion order
    private final Map<String, List<String>> keyValues = new LinkedHashMap<>();

    @Override
    @CheckForNull
    public String getValue(String key) {
      return keyValues.containsKey(key) ? keyValues.get(key).get(0) : null;
    }

    @Override
    public List<String> getValues(String key) {
      return keyValues.containsKey(key) ? keyValues.get(key) : emptyList();
    }

    @Override
    public Set<String> getKeys() {
      return keyValues.keySet();
    }

    private DefaultParameters setValue(String key, String value) {
      checkArgument(!isNullOrEmpty(key));
      checkArgument(value != null);

      keyValues.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
      return this;
    }

    private DefaultParameters setValues(String key, Collection<String> values) {
      checkArgument(!isNullOrEmpty(key));
      checkArgument(values != null && !values.isEmpty());

      keyValues.computeIfAbsent(key, k -> new ArrayList<>()).addAll(values.stream().map(Object::toString).filter(Objects::nonNull).collect(Collectors.toList()));
      return this;
    }
  }

  private static class DefaultHeaders implements Headers {
    private final Map<String, String> keyValues = new HashMap<>();

    @Override
    public Optional<String> getValue(String name) {
      return Optional.ofNullable(keyValues.get(name));
    }

    private DefaultHeaders setValue(String name, @Nullable String value) {
      checkArgument(!isNullOrEmpty(name));

      if (value == null) {
        keyValues.remove(name);
      } else {
        keyValues.put(name, value);
      }
      return this;
    }

    @Override
    public Set<String> getNames() {
      return unmodifiableSet(keyValues.keySet());
    }
  }
}
