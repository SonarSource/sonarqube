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
package org.sonar.server.v2.api.mode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.mode.resources.ModeResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.MODE_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(MODE_ENDPOINT)
@RestController
@Tag(name = "Clean Code Policy")
public interface ModeController {

  @GetMapping(path = "", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Retrieve current instance Mode", description = """
    Fetch the current instance mode. Can be Multi-Quality Rules (MQR) Mode or Standard Experience.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  ModeResource getMode();

  @PatchMapping(path = "")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update current instance Mode", description = """
    Update the current instance mode. Can be Multi-Quality Rules (MQR) Mode or Standard Experience.
    Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  ModeResource patchMode(@Valid @RequestBody ModeResource modeResource);
}
