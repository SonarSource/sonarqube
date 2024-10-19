/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.ws;

import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.protobuf.GeneratedMessageV3;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.sonar.api.impl.ws.PartImpl;
import org.sonar.api.impl.ws.ValidatingRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class TestRequest extends ValidatingRequest {

  private final ListMultimap<String, String> multiParams = ArrayListMultimap.create();
  private final Map<String, String> params = new HashMap<>();
  private final Map<String, String> headers = new HashMap<>();
  private final Map<String, Part> parts = new HashMap<>();
  private String payload = "";
  private boolean payloadConsumed = false;
  private String method = "GET";
  private String mimeType = "application/octet-stream";
  private String path;

  @Override
  public BufferedReader getReader() {
    checkState(!payloadConsumed, "Payload already consumed");
    if (payload == null) {
      return super.getReader();
    }

    BufferedReader res = new BufferedReader(new StringReader(payload));
    payloadConsumed = true;
    return res;
  }

  public TestRequest setPayload(String payload) {
    checkState(!payloadConsumed, "Payload already consumed");

    this.payload = payload;
    return this;
  }

  @Override
  public String readParam(String key) {
    return params.get(key);
  }

  @Override
  public List<String> readMultiParam(String key) {
    return multiParams.get(key);
  }

  @Override
  public InputStream readInputStreamParam(String key) {
    String value = readParam(key);
    if (value == null) {
      return null;
    }
    return IOUtils.toInputStream(value);
  }

  @Override
  public Part readPart(String key) {
    return parts.get(key);
  }

  public TestRequest setPart(String key, InputStream input, String fileName) {
    parts.put(key, new PartImpl(input, fileName));
    return this;
  }

  @Override
  public String method() {
    return method;
  }

  @Override
  public boolean hasParam(String key) {
    return params.containsKey(key) || multiParams.containsKey(key);
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public Map<String, String[]> getParams() {
    ArrayListMultimap<String, String> result = ArrayListMultimap.create(multiParams);
    params.forEach(result::put);
    return result.asMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(new String[0])));
  }

  public TestRequest setPath(String path) {
    this.path = path;
    return this;
  }

  public TestRequest setMethod(String method) {
    checkNotNull(method);
    this.method = method;
    return this;
  }

  @Override
  public String getMediaType() {
    return mimeType;
  }

  public TestRequest setMediaType(String type) {
    checkNotNull(type);
    this.mimeType = type;
    return this;
  }

  public TestRequest setParam(String key, String value) {
    checkNotNull(key);
    checkNotNull(value);
    this.params.put(key, value);
    return this;
  }

  public TestRequest setMultiParam(String key, List<String> values) {
    requireNonNull(key);
    requireNonNull(values);

    multiParams.putAll(key, values);

    return this;
  }

  @Override
  public Map<String, String> getHeaders() {
    return ImmutableMap.copyOf(headers);
  }

  @Override
  public Optional<String> header(String name) {
    return Optional.ofNullable(headers.get(name));
  }

  public TestRequest setHeader(String name, String value) {
    headers.put(requireNonNull(name), requireNonNull(value));
    return this;
  }

  public TestResponse execute() {
    try {
      DumbResponse response = new DumbResponse();
      action().handler().handle(this, response);
      return new TestResponse(response);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public <T extends GeneratedMessageV3> T executeProtobuf(Class<T> protobufClass) {
    return setMediaType(PROTOBUF).execute().getInputObject(protobufClass);
  }

  @Override
  public String toString() {
    return path;
  }
}
