/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;
import org.sonar.api.ce.ComputeEngineSide;

import static java.nio.charset.StandardCharsets.UTF_8;

@ComputeEngineSide
public class SarifSerializer {
  private final Gson gson;

  @Inject
  public SarifSerializer() {
    this(new Gson());
  }

  @VisibleForTesting
  SarifSerializer(Gson gson) {
    this.gson = gson;
  }

  public String serializeAndEncode(Sarif210 sarif210) {
    String serializedSarif = gson.toJson(sarif210);
    return compressToGzipAndEncodeBase64(serializedSarif);
  }

  private static String compressToGzipAndEncodeBase64(String input) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream)) {
      gzipStream.write(input.getBytes(UTF_8));
      gzipStream.finish();
      return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to compress and encode the input: %s", input), e);
    }
  }
}
