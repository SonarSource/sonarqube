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

import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.sonar.process.System2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment.CLIENT_ID;
import static org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment.EXTENSION_RESOURCE_ID;
import static org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment.MARKETPLACE_AZURE_BILLING;
import static org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment.PLAN_ID;

class AzureEnvironmentTest {

  private final AzureEnvironment underTest = new AzureEnvironment(new testSystemEnv());

  @Test
  void testisAzureBillingEnabled() {
    assertTrue(underTest.isAzureBillingEnabled());
  }

  @Test
  void testGetAzureClientId() {
    assertEquals("client-id", underTest.getAzureClientId().orElse(null));
  }

  @Test
  void testGetResourceId() {
    assertEquals("resource-id", underTest.getResourceId().orElse(null));
  }

  @Test
  void testGetPlanId() {
    assertEquals("plan-id", underTest.getPlanId().orElse(null));
  }

  private static class testSystemEnv implements System2 {

    @Override
    public Map<String, String> getenv() {
      return Map.of(MARKETPLACE_AZURE_BILLING, "true",
                    CLIENT_ID, "client-id",
                    EXTENSION_RESOURCE_ID, "resource-id",
                    PLAN_ID, "plan-id");
    }

    @Override
    public String getenv(String name) {
      return getenv().get(name);
    }

    @Override
    public boolean isOsWindows() {
      throw new NotImplementedException("Method not implemented");
    }
  }

}
