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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sonar.server.v2.api.analysis.response.JreInfoRestResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH;
import static org.sonar.server.v2.WebApiEndpoints.JRE_ENDPOINT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RequestMapping(value = JRE_ENDPOINT, produces = APPLICATION_JSON_VALUE)
@ResponseStatus(OK)
@RestController
@Tag(name = "Analysis")
public interface JresController {

  String OS_PARAM_DESCRIPTION = "Filter the JRE by operating system. Accepted values are 'windows', 'linux', 'macos', 'alpine' (case-insensitive), with some aliases";
  String ARCH_PARAM_DESCRIPTION = "Filter the JRE by CPU architecture. Accepted values are 'x64' and 'aarch64' (case-insensitive), with some aliases.";
  String GET_JRE_DESCRIPTION =
    "This endpoint return the JRE metadata by default. To download the JRE binary asset, set the Accept header of the request to 'application/octet-stream'.";
  String GET_JRE_SUMMARY = "JRE download/metadata";
  String ID_PARAM_DESCRIPTION = "The ID of the JRE";

  @GetMapping
  @Operation(summary = "All JREs metadata", description = "Get metadata of all available JREs")
  List<JreInfoRestResponse> getJresMetadata(
    @RequestParam(value = "os", required = false) @Parameter(description = OS_PARAM_DESCRIPTION) String os,
    @RequestParam(value = "arch", required = false) @Parameter(description = ARCH_PARAM_DESCRIPTION) String arch);

  @GetMapping(value = "/{id}")
  @Operation(summary = GET_JRE_SUMMARY, description = GET_JRE_DESCRIPTION)
  JreInfoRestResponse getJreMetadata(
    @PathVariable(value = "id") @Parameter(description = ID_PARAM_DESCRIPTION, required = true, in = PATH) String id);

  @GetMapping(value = "/{id}", produces = APPLICATION_OCTET_STREAM_VALUE)
  @Operation(summary = GET_JRE_SUMMARY, description = GET_JRE_DESCRIPTION)
  InputStreamResource downloadJre(
    @PathVariable(value = "id") @Parameter(description = ID_PARAM_DESCRIPTION, required = true, in = PATH) String id);
}
