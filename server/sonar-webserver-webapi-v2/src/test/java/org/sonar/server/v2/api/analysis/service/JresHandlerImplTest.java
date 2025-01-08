/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api.analysis.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.v2.api.analysis.response.JreInfoRestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JresHandlerImplTest {

  private static final Map<String, JreInfoRestResponse> JRE_METADATA = Map.of(
    "1", new JreInfoRestResponse("1", "jre1", "checksum1", "java1", "alpine", "aarch64"),
    "2", new JreInfoRestResponse("2", "jre2", "checksum2", "java2", "linux", "aarch64"),
    "3", new JreInfoRestResponse("3", "jre3", "checksum3", "java3", "alpine", "x64")
  );

  private static final JresHandlerImpl jresHandler = new JresHandlerImpl("jres-metadata-tests.json");

  @BeforeAll
  static void setup() {
    jresHandler.initMetadata();
  }

  @Test
  void getJresMetadata_shouldReturnAllMetadata_whenNoFiltering() {
    List<JreInfoRestResponse> result = jresHandler.getJresMetadata(null, null);

    assertThat(result).extracting(JreInfoRestResponse::id).containsExactly("1", "2", "3");
  }

  @ParameterizedTest
  @MethodSource("filteredMetadata")
  void getJresMetadata_shouldReturnFilteredMetadata_whenFiltering(String os, String arch, JreInfoRestResponse expected) {
    List<JreInfoRestResponse> resultList = jresHandler.getJresMetadata(os, arch);

    assertThat(resultList).hasSize(1);
    JreInfoRestResponse result = resultList.get(0);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getJresMetadata_shouldFail_whenFilteredWithUnsupportedOsValue() {
    String anyUnsupportedOS = "not-supported";

    assertThatThrownBy(() -> jresHandler.getJresMetadata(anyUnsupportedOS, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Unsupported OS: '" + anyUnsupportedOS + "'");
  }

  @Test
  void getJresMetadata_shouldFail_whenFilteredWithUnsupportedArchValue() {
    String anyUnsupportedArch = "not-supported";

    assertThatThrownBy(() -> jresHandler.getJresMetadata(null, anyUnsupportedArch))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Unsupported architecture: '" + anyUnsupportedArch + "'");
  }

  private static Stream<Arguments> filteredMetadata() {
    return Stream.of(
      arguments("alpine", "aarch64", JRE_METADATA.get("1")),
      arguments("linux", null, JRE_METADATA.get("2")),
      arguments(null, "x64", JRE_METADATA.get("3"))
    );
  }

  @Test
  void getJresMetadata_shouldReturnEmptyList_whenNoMetadata() {
    List<JreInfoRestResponse> result = jresHandler.getJresMetadata("windows", "x64");
    assertThat(result).isEmpty();
  }

  @Test
  void getJresMetadata_shouldReturnEmptyList_whenNoFilteringAndNoMetadata() {
    JresHandlerImpl noMetadataHandler = new JresHandlerImpl("");
    List<JreInfoRestResponse> result = noMetadataHandler.getJresMetadata(null, null);
    assertThat(result).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "2", "3"})
  void getJreMetadata(String id) {
    JreInfoRestResponse result = jresHandler.getJreMetadata(id);

    assertThat(result).usingRecursiveComparison().isEqualTo(JRE_METADATA.get(id));
  }

  @Test
  void getJreMetadata_shouldFail_whenJreNotFound() {
    assertThatThrownBy(() -> jresHandler.getJreMetadata("4"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("JRE not found for id: 4");
  }

  @Test
  void getJreBinary_shouldFail_whenFileNotFound() {
    assertThatThrownBy(() -> jresHandler.getJreBinary("jre1"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unable to find JRE 'jre1'");
  }
}
