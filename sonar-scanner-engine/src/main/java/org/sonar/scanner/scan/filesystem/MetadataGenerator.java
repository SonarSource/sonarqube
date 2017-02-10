/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.Metadata;

class MetadataGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataGenerator.class);
  @VisibleForTesting
  static final Charset UTF_32BE = Charset.forName("UTF-32BE");

  @VisibleForTesting
  static final Charset UTF_32LE = Charset.forName("UTF-32LE");

  private final StatusDetection statusDetection;
  private final FileMetadata fileMetadata;
  private final DefaultInputModule inputModule;

  MetadataGenerator(DefaultInputModule inputModule, StatusDetection statusDetection, FileMetadata fileMetadata) {
    this.inputModule = inputModule;
    this.statusDetection = statusDetection;
    this.fileMetadata = fileMetadata;
  }

  /**
   * Sets all metadata in the file, including charset and status.
   * It is an expensive computation, reading the entire file.
   */
  public void setMetadata(final DefaultInputFile inputFile, Charset defaultEncoding) {
    try {
      Charset charset = detectCharset(inputFile.path(), defaultEncoding);
      inputFile.setCharset(charset);
      Metadata metadata = fileMetadata.readMetadata(inputFile.file(), charset);
      inputFile.setMetadata(metadata);
      inputFile.setStatus(statusDetection.status(inputModule.definition().getKeyWithBranch(), inputFile.relativePath(), metadata.hash()));
      LOG.debug("'{}' generated metadata {} with charset '{}'",
        inputFile.relativePath(), inputFile.type() == Type.TEST ? "as test " : "", charset);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return charset detected from BOM in given file or given defaultCharset
   * @throws IllegalStateException if an I/O error occurs
   */
  private static Charset detectCharset(Path path, Charset defaultCharset) {
    try (InputStream inputStream = Files.newInputStream(path)) {
      byte[] bom = new byte[4];
      int n = inputStream.read(bom, 0, bom.length);
      if ((n >= 3) && (bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
        return StandardCharsets.UTF_8;
      } else if ((n >= 4) && (bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
        return UTF_32BE;
      } else if ((n >= 4) && (bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
        return UTF_32LE;
      } else if ((n >= 2) && (bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
        return StandardCharsets.UTF_16BE;
      } else if ((n >= 2) && (bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
        return StandardCharsets.UTF_16LE;
      } else {
        return defaultCharset;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file " + path.toAbsolutePath().toString(), e);
    }
  }
}
