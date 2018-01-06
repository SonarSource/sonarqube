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
package org.sonar.scanner.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;

public class MetadataGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataGenerator.class);
  @VisibleForTesting
  static final Charset UTF_32BE = Charset.forName("UTF-32BE");

  @VisibleForTesting
  static final Charset UTF_32LE = Charset.forName("UTF-32LE");

  private final StatusDetection statusDetection;
  private final FileMetadata fileMetadata;
  private final DefaultInputModule inputModule;
  private final IssueExclusionsLoader exclusionsScanner;

  public MetadataGenerator(DefaultInputModule inputModule, StatusDetection statusDetection, FileMetadata fileMetadata, IssueExclusionsLoader exclusionsScanner) {
    this.inputModule = inputModule;
    this.statusDetection = statusDetection;
    this.fileMetadata = fileMetadata;
    this.exclusionsScanner = exclusionsScanner;
  }

  /**
   * Sets all metadata in the file, including charset and status.
   * It is an expensive computation, reading the entire file.
   */
  public void setMetadata(final DefaultInputFile inputFile, Charset defaultEncoding) {
    CharsetDetector charsetDetector = new CharsetDetector(inputFile.path(), defaultEncoding);
    try {
      Charset charset;
      if (charsetDetector.run()) {
        charset = charsetDetector.charset();
      } else {
        LOG.debug("Failed to detect a valid charset for file '{}'. Using default charset.", inputFile);
        charset = defaultEncoding;
      }
      InputStream is = charsetDetector.inputStream();
      inputFile.setCharset(charset);
      Metadata metadata = fileMetadata.readMetadata(is, charset, inputFile.absolutePath(), exclusionsScanner.createCharHandlerFor(inputFile.key()));
      inputFile.setMetadata(metadata);
      inputFile.setStatus(statusDetection.status(inputModule.definition().getKeyWithBranch(), inputFile, metadata.hash()));
      LOG.debug("'{}' generated metadata {} with charset '{}'", inputFile, inputFile.type() == Type.TEST ? "as test " : "", charset);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}
