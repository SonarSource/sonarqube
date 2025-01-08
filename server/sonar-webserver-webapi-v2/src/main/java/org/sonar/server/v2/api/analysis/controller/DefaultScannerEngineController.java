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
package org.sonar.server.v2.api.analysis.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.v2.api.analysis.response.EngineInfoRestResponse;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandler;
import org.sonar.server.v2.api.analysis.service.ScannerEngineMetadata;
import org.springframework.core.io.InputStreamResource;

import static java.lang.String.format;

public class DefaultScannerEngineController implements ScannerEngineController {

  private final ScannerEngineHandler scannerEngineHandler;

  public DefaultScannerEngineController(ScannerEngineHandler scannerEngineHandler) {
    this.scannerEngineHandler = scannerEngineHandler;
  }

  @Override
  public EngineInfoRestResponse getScannerEngineMetadata() {
    ScannerEngineMetadata metadata = scannerEngineHandler.getScannerEngineMetadata();
    return new EngineInfoRestResponse(metadata.filename(), metadata.checksum());
  }

  @Override
  public InputStreamResource downloadScannerEngine() {
    File scannerEngine = scannerEngineHandler.getScannerEngine();
    try {
      return new InputStreamResource(new FileInputStream(scannerEngine));
    } catch (FileNotFoundException e) {
      throw new NotFoundException(format("Unable to find file: %s", scannerEngine.getName()));
    }
  }
}
