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
package org.sonar.server.v2.api.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.v2.api.analysis.response.JreInfoRestResponse;

import static java.lang.String.join;
import static org.apache.commons.lang.StringUtils.isBlank;

public class JresHandlerImpl implements JresHandler {

  private static final String JRES_METADATA_FILENAME = "jres-metadata.json";

  private final String jresMetadataFilename;
  private final Map<String, JreInfoRestResponse> metadata = new HashMap<>();

  public JresHandlerImpl() {
    this(JRES_METADATA_FILENAME);
  }

  @VisibleForTesting
  public JresHandlerImpl(String jresMetadataFilename) {
    this.jresMetadataFilename = jresMetadataFilename;
  }

  @PostConstruct
  void initMetadata() {
    metadata.clear();
    readJresMetadata().forEach(jre -> metadata.put(jre.id(), jre));
  }

  private List<JreInfoRestResponse> readJresMetadata() {
    ObjectMapper objectMapper = new ObjectMapper();
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(jresMetadataFilename)) {
      return objectMapper.readValue(is, objectMapper.getTypeFactory().constructCollectionType(List.class, JreInfoRestResponse.class));
    } catch (IOException ioException) {
      throw new UncheckedIOException(ioException);
    }
  }

  @Override
  public List<JreInfoRestResponse> getJresMetadata(@Nullable String os, @Nullable String arch) {
    Predicate<JreInfoRestResponse> osFilter = isBlank(os) ? jre -> true : (jre -> OS.from(jre.os()) == OS.from(os));
    Predicate<JreInfoRestResponse> archFilter = isBlank(arch) ? jre -> true : (jre -> Arch.from(jre.arch()) == Arch.from(arch));
    return metadata.values().stream()
      .filter(osFilter)
      .filter(archFilter)
      .toList();
  }

  @Override
  public JreInfoRestResponse getJreMetadata(String id) {
    return Optional.ofNullable(metadata.get(id))
      .orElseThrow(() -> new NotFoundException("JRE not found for id: " + id));
  }

  @Override
  public InputStream getJreBinary(String jreFilename) {
    try {
      return new FileInputStream("jres/" + jreFilename);
    } catch (FileNotFoundException fileNotFoundException) {
      throw new NotFoundException(String.format("Unable to find JRE '%s'", jreFilename));
    }
  }

  enum OS {
    WINDOWS("win", "windows", "win32"),
    LINUX("linux"),
    MACOS("mac", "macos", "darwin"),
    ALPINE("alpine");

    private final List<String> aliases;

    OS(String... aliases) {
      this.aliases = Arrays.stream(aliases).toList();
    }

    private static OS from(String alias) {
      return Arrays.stream(values())
        .filter(os -> os.aliases.contains(alias))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported OS: '%s'. Supported values are '%s'", alias, join(", ", supportedValues()))));
    }

    private static List<String> supportedValues() {
      return Arrays.stream(values())
        .flatMap(os -> os.aliases.stream())
        .toList();
    }
  }

  enum Arch {
    X64("x86_64", "x86-64", "amd64", "x64"),
    AARCH64("arm64", "aarch64");

    private final List<String> aliases;

    Arch(String... aliases) {
      this.aliases = Arrays.stream(aliases).toList();
    }

    private static Arch from(String alias) {
      return Arrays.stream(values())
        .filter(arch -> arch.aliases.contains(alias))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported architecture: '%s'. Supported values are '%s'", alias, join(", ", supportedValues()))));
    }

    private static List<String> supportedValues() {
      return Arrays.stream(values())
        .flatMap(arch -> arch.aliases.stream())
        .toList();
    }
  }
}
