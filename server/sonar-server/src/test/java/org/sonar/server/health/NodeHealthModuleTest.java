/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Test;
import org.picocontainer.ComponentAdapter;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.process.NetworkUtils;
import org.sonar.process.cluster.health.SharedHealthStateImpl;
import org.sonar.process.cluster.hz.HazelcastMember;

import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeHealthModuleTest {
  private Random random = new Random();
  private MapSettings mapSettings = new MapSettings();
  private NodeHealthModule underTest = new NodeHealthModule();

  @Test
  public void no_broken_dependencies() {
    ComponentContainer container = new ComponentContainer();
    Server server = mock(Server.class);
    NetworkUtils networkUtils = mock(NetworkUtils.class);
    // settings required by NodeHealthProvider
    mapSettings.setProperty("sonar.cluster.node.name", randomAlphanumeric(3));
    mapSettings.setProperty("sonar.cluster.node.port", valueOf(1 + random.nextInt(10)));
    when(server.getStartedAt()).thenReturn(new Date());
    when(networkUtils.getHostname()).thenReturn(randomAlphanumeric(12));
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
    ComponentContainer container = new ComponentContainer();

    underTest.configure(container);

    assertThat(classesAddedToContainer(container))
      .contains(SharedHealthStateImpl.class);
  }

  private List<Class<?>> classesAddedToContainer(ComponentContainer container) {
    Collection<ComponentAdapter<?>> componentAdapters = container.getPicoContainer().getComponentAdapters();
    return componentAdapters.stream().map(ComponentAdapter::getComponentImplementation).collect(Collectors.toList());
  }
}
