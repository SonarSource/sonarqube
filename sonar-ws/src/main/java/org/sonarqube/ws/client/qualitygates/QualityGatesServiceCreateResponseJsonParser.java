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
package org.sonarqube.ws.client.qualitygates;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.sonarqube.ws.Qualitygates;

public class QualityGatesServiceCreateResponseJsonParser implements Parser<Qualitygates.CreateResponse> {

  @Override
  public Qualitygates.CreateResponse parseFrom(CodedInputStream input) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(CodedInputStream input) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(ByteString data) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(ByteString data) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(byte[] data, int off, int len) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(byte[] data, int off, int len, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(byte[] data) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(byte[] data, int off, int len) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(byte[] data, int off, int len, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(byte[] data) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(InputStream input) throws InvalidProtocolBufferException {
    Qualitygates.CreateResponse.Builder builder = Qualitygates.CreateResponse.newBuilder();
    String json = readInputStream(input);
    JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
    builder.setId(jobj.get("id").getAsString());
    builder.setName(jobj.get("name").getAsString());
    return builder.build();
  }

  private String readInputStream(InputStream input) {
    StringBuilder textBuilder = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader(input, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c = 0;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return textBuilder.toString();
  }

  @Override
  public Qualitygates.CreateResponse parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(InputStream input) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parseDelimitedFrom(InputStream input) throws InvalidProtocolBufferException {
    return parseFrom(input);
  }

  @Override
  public Qualitygates.CreateResponse parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialDelimitedFrom(InputStream input) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }

  @Override
  public Qualitygates.CreateResponse parsePartialDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
    throw new IllegalStateException("not implemented");
  }
}
