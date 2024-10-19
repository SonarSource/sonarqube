/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.ws;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.core.platform.ListContainer;
import org.sonar.server.common.health.DbConnectionNodeCheck;
import org.sonar.server.common.health.EsStatusNodeCheck;
import org.sonar.server.health.HealthCheckerImpl;
import org.sonar.server.common.health.NodeHealthCheck;
import org.sonar.server.common.health.WebServerSafemodeNodeCheck;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeModeHealthCheckerModuleTest {
  private final SafeModeHealthCheckerModule underTest = new SafeModeHealthCheckerModule();

  @Test
  public void verify_HealthChecker() {
    ListContainer container = new ListContainer();

    underTest.configure(container);

    assertThat(container.getAddedObjects())
      .contains(HealthCheckerImpl.class)
      .doesNotContain(HealthActionSupport.class)
      .doesNotContain(SafeModeHealthAction.class)
      .doesNotContain(HealthAction.class);
  }

  @Test
  public void verify_installed_HealthChecks_implementations() {
    ListContainer container = new ListContainer();

    underTest.configure(container);

    List<Class<?>> checks = container.getAddedObjects().stream()
      .filter(o -> o instanceof Class)
      .map(o -> (Class<?>) o)
      .filter(NodeHealthCheck.class::isAssignableFrom)
      .collect(Collectors.toList());
    assertThat(checks).containsOnly(WebServerSafemodeNodeCheck.class, DbConnectionNodeCheck.class, EsStatusNodeCheck.class);
  }
}
