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
package org.sonar.server.platform.monitoring;

import org.junit.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.monitoring.cluster.ServerPushSection;
import org.sonar.server.pushapi.sonarlint.SonarLintClientsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPushSectionTest {

  private final SonarLintClientsRegistry sonarLintClientsRegistry = mock(SonarLintClientsRegistry.class);

  private final ServerPushSection underTest = new ServerPushSection(sonarLintClientsRegistry);

  @Test
  public void toProtobuf_with5ConnectedSonarLintClients() {

    when(sonarLintClientsRegistry.countConnectedClients()).thenReturn(5L);

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThat(section.getName()).isEqualTo("Server Push Connections");
    assertThat(section.getAttributesList())
      .extracting(ProtobufSystemInfo.Attribute::getKey, ProtobufSystemInfo.Attribute::getLongValue)
      .containsExactlyInAnyOrder(tuple("SonarLint Connected Clients", 5L));
  }
}
