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
package org.sonar.server.v2.api.azurebilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.sonar.server.v2.api.azurebilling.response.AzureBillingRestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.AZURE_BILLING_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(value = AZURE_BILLING_ENDPOINT, produces = APPLICATION_JSON_VALUE)
@RestController
public interface AzureBillingController {

  @PostMapping
  @Operation(summary = "Bills user's Azure account with the cost of SonarQube Server license",
    description = "Used by admin to bill user's Azure account with the cost of SonarQube Server license.")
  ResponseEntity<AzureBillingRestResponse> billAzureAccount();

}
