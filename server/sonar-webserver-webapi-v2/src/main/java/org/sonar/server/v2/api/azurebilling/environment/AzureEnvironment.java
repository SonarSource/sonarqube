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
package org.sonar.server.v2.api.azurebilling.environment;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import org.sonar.process.System2;
import org.springframework.beans.factory.annotation.Autowired;

public class AzureEnvironment {

  public static final String MARKETPLACE_AZURE_BILLING = "MARKETPLACE_AZURE_BILLING";
  public static final String CLIENT_ID = "CLIENT_ID";
  public static final String EXTENSION_RESOURCE_ID = "EXTENSION_RESOURCE_ID";
  public static final String PLAN_ID = "PLAN_ID";

  private final System2 system2;

  @Autowired
  public AzureEnvironment() {
    this.system2 = System2.INSTANCE;
  }

  @VisibleForTesting
  public AzureEnvironment(System2 system2) {
    this.system2 = system2;
  }

  public boolean isAzureBillingEnabled() {
    return Boolean.parseBoolean(system2.getenv(MARKETPLACE_AZURE_BILLING));
  }

  public Optional<String> getAzureClientId() {
    return Optional.ofNullable(system2.getenv(CLIENT_ID));
  }

  public Optional<String> getResourceId() {
    return Optional.ofNullable(system2.getenv(EXTENSION_RESOURCE_ID));
  }

  public Optional<String> getPlanId() {
    return Optional.ofNullable(system2.getenv(PLAN_ID));
  }
}
