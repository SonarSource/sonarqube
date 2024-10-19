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

import com.google.protobuf.GeneratedMessageV3;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResponse {

  private final TestableResponse testableResponse;

  public TestResponse(TestableResponse dumbResponse) {
    this.testableResponse = dumbResponse;
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(testableResponse.getFlushedOutput());
  }

  public <T extends GeneratedMessageV3> T getInputObject(Class<T> protobufClass) {
    try (InputStream input = getInputStream()) {
      Method parseFromMethod = protobufClass.getMethod("parseFrom", InputStream.class);
      @SuppressWarnings("unchecked")
      T result = (T) parseFromMethod.invoke(null, input);
      return result;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String getInput() {
    return new String(testableResponse.getFlushedOutput(), StandardCharsets.UTF_8);
  }

  public String getMediaType() {
    return testableResponse.mediaType();
  }

  public int getStatus() {
    return testableResponse.status();
  }

  @CheckForNull
  public String getHeader(String headerKey) {
    return testableResponse.getHeader(headerKey);
  }

  public void assertJson(String expectedJson) {
    JsonAssert.assertJson(getInput()).isSimilarTo(expectedJson);
  }

  /**
   * Compares JSON response with JSON file available in classpath. For example if class
   * is org.foo.BarTest and filename is index.json, then file must be located
   * at src/test/resources/org/foo/BarTest/index.json.
   *
   * @param clazz                the test class
   * @param expectedJsonFilename name of the file containing the expected JSON
   */
  public void assertJson(Class clazz, String expectedJsonFilename) {
    String path = clazz.getSimpleName() + "/" + expectedJsonFilename;
    URL url = clazz.getResource(path);
    if (url == null) {
      throw new IllegalStateException("Cannot find " + path);
    }
    JsonAssert.assertJson(getInput()).isSimilarTo(url);
  }

  public void assertNoContent() {
    assertThat(getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
  }
}
