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
package org.sonar.server.health;

import java.util.Date;
import java.util.Random;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.ListContainer;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.process.NetworkUtils;
import org.sonar.process.cluster.health.SharedHealthStateImpl;
import org.sonar.process.cluster.hz.HazelcastMember;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeHealthModuleTest {
  private final Random random = new Random();
  private final MapSettings mapSettings = new MapSettings();
  private final NodeHealthModule underTest = new NodeHealthModule();

  @Test
  public void no_broken_dependencies() {
    SpringComponentContainer container = new SpringComponentContainer();
    Server server = mock(Server.class);
    NetworkUtils networkUtils = mock(NetworkUtils.class);
    // settings required by NodeHealthProvider
    mapSettings.setProperty("sonar.cluster.node.name", secure().nextAlphanumeric(3));
    mapSettings.setProperty("sonar.cluster.node.port", valueOf(1 + random.nextInt(10)));
    when(server.getStartedAt()).thenReturn(new Date());
    when(networkUtils.getHostname()).thenReturn(secure().nextAlphanumeric(12));
    // upper level dependencies
    container.add(
      mock(System2.class),
      mapSettings.asConfig(),
      server,
      networkUtils,
      mock(HazelcastMember.class));
    // HealthAction dependencies
    container.add(mock(HealthChecker.class));

    underTest.configure(container);

    container.startComponents();
  }

  @Test
  public void provides_implementation_of_SharedHealthState() {
    ListContainer container = new ListContainer();
    underTest.configure(container);

    assertThat(container.getAddedObjects())
      .contains(SharedHealthStateImpl.class);
  }
}
