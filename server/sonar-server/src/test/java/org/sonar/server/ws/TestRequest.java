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
package org.sonar.server.ws;

import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessageV3;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.internal.PartImpl;
import org.sonar.api.server.ws.internal.ValidatingRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class TestRequest extends ValidatingRequest {

  private final ListMultimap<String, String> multiParams = ArrayListMultimap.create();
  private final Map<String, String> params = new HashMap<>();
  private final Map<String, String> headers = new HashMap<>();
  private final Map<String, Part> parts = Maps.newHashMap();
  private String method = "GET";
  private String mimeType = "application/octet-stream";
  private String path;

  @Override
  protected String readParam(String key) {
    return params.get(key);
  }

  @Override
  protected List<String> readMultiParam(String key) {
    return multiParams.get(key);
  }

  @Override
  protected InputStream readInputStreamParam(String key) {
    String value = readParam(key);
    if (value == null) {
      return null;
    }
    return IOUtils.toInputStream(value);
  }

  @Override
  protected Part readPart(String key) {
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
  public Optional<String> header(String name) {
    return Optional.ofNullable(headers.get(name));
  }

  public TestRequest setHeader(String name, String value) {
    this.headers.put(requireNonNull(name), requireNonNull(value));
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
