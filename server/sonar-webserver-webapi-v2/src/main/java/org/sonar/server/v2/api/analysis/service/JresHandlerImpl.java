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
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.v2.api.analysis.response.JreInfoRestResponse;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import static java.lang.String.join;
import static org.apache.commons.lang.StringUtils.isBlank;

public class JresHandlerImpl implements JresHandler {

  private static final String JRES_METADATA_FILENAME = "jres-metadata.json";
  private static final Logger LOG = LoggerFactory.getLogger(JresHandlerImpl.class);

  private final String jresMetadataFilename;
  private final Map<String, JreInfoRestResponse> metadata = new HashMap<>();
  private final String jresBucketName = System.getenv("CODESCAN_JRE_BUCKET_NAME");
  private final String jresPath = System.getenv("CODESCAN_JRE_PATH");
  private final Region region = Region.of(System.getenv("AWS_DEFAULT_REGION"));
  private final S3Client s3Client = S3Client.builder()
          .region(region)
          .credentialsProvider(DefaultCredentialsProvider.create())
          .build();
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
      if (is == null) {
        return List.of();
      }
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

  /**
   * Fetches the JRE binary from S3 if the bucket is set; otherwise, loads from classpath.
   * @param jreFilename File name or S3 key (e.g. "linux-x64/openjdk-17.tar.gz")
   * @return InputStream to the binary data
   */

  @Override
  public InputStream getJreBinary(String jreFilename) throws Exception {
    LOG.info("Fetching JRE file {} from bucket {}", jreFilename, jresBucketName);
    if (isNotBlank(jresBucketName) && isNotBlank(jresPath)) {
      try {
        LOG.info("Fetching file {} from S3 path {}", jreFilename, jresPath);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(jresBucketName)
                .key(jresPath + jreFilename)
                .build();

        return s3Client.getObject(request);
      } catch (S3Exception e) {
        LOG.debug("Failed to fetch file {} from S3 bucket path {}.", jreFilename, jresPath );
        throw new RuntimeException("Failed to fetch file from S3 bucket");
      }
    }else {
      LOG.info("Fetching file {} from classpath", jreFilename);
      //Only Linux x64 supported JRE is bundled with the server
      InputStream inputStream = getClass().getResourceAsStream("/jres/" + jreFilename);
      if (inputStream == null) {
        LOG.debug("File not found in classpath: /jres/{}", jreFilename);
        throw new Exception("Resource not found in classpath");
      }
      return inputStream;
    }
  }

  private boolean isNotBlank(String str) {
    return str != null && !str.isBlank();
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
