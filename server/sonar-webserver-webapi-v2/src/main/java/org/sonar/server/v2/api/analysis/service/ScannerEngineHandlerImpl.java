/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.ServerFileSystem;

import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.HiddenFileFilter.VISIBLE;

public class ScannerEngineHandlerImpl implements ScannerEngineHandler {

  private final ServerFileSystem fs;

  private ScannerEngineMetadata scannerEngineMetadata;

  public ScannerEngineHandlerImpl(ServerFileSystem fs) {
    this.fs = fs;
  }

  @Override
  public File getScannerEngine() {
    File scannerDir = new File(fs.getHomeDir(), "lib/scanner");
    if (!scannerDir.exists()) {
      throw new NotFoundException(format("Scanner directory not found: %s", scannerDir.getAbsolutePath()));
    }
    return listFiles(scannerDir, VISIBLE, directoryFileFilter())
      .stream()
      .filter(file -> file.getName().endsWith(".jar"))
      .findFirst()
      .orElseThrow(() -> new NotFoundException(format("Scanner JAR not found in directory: %s", scannerDir.getAbsolutePath())));
  }

  private static String getSha256(File file) {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      return sha256Hex(fileInputStream);
    } catch (IOException exception) {
      throw new UncheckedIOException(new IOException("Unable to compute SHA-256 checksum of the Scanner Engine", exception));
    }
  }

  @Override
  public ScannerEngineMetadata getScannerEngineMetadata() {
    if (scannerEngineMetadata == null) {
      File scannerEngine = getScannerEngine();
      scannerEngineMetadata = new ScannerEngineMetadata(scannerEngine.getName(), getSha256(scannerEngine));
    }
    return scannerEngineMetadata;
  }

}
