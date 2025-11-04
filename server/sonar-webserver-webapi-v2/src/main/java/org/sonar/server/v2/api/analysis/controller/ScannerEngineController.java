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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sonar.server.v2.api.analysis.response.EngineInfoRestResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.SCANNER_ENGINE_ENDPOINT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RequestMapping(value = SCANNER_ENGINE_ENDPOINT, produces = APPLICATION_JSON_VALUE)
@RestController
@Tag(name = "Analysis")
public interface ScannerEngineController {

  String GET_ENGINE_SUMMARY = "Scanner engine download/metadata";
  String GET_ENGINE_DESCRIPTION =
    "This endpoint return the Scanner Engine metadata by default. To download the Scanner Engine, set the Accept header of the request to 'application/octet-stream'.";

  @GetMapping
  @ResponseStatus(OK)
  @Operation(summary = GET_ENGINE_SUMMARY, description = GET_ENGINE_DESCRIPTION)
  EngineInfoRestResponse getScannerEngineMetadata();

  @GetMapping(produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(OK)
  @Operation(summary = GET_ENGINE_SUMMARY, description = GET_ENGINE_DESCRIPTION)
  InputStreamResource downloadScannerEngine();

}
