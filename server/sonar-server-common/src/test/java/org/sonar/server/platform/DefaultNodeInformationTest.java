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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultNodeInformationTest {


  private MapSettings settings = new MapSettings();

  @Test
  public void cluster_is_disabled_by_default() {
    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.isStandalone()).isTrue();
    assertThat(underTest.isStartupLeader()).isTrue();
  }

  @Test
  public void node_is_startup_leader_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.web.startupLeader", "true");

    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.isStandalone()).isFalse();
    assertThat(underTest.isStartupLeader()).isTrue();
  }

  @Test
  public void node_is_startup_follower_by_default_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");

    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.isStandalone()).isFalse();
    assertThat(underTest.isStartupLeader()).isFalse();
  }

  @Test
  public void node_is_startup_follower_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.web.startupLeader", "false");

    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.isStandalone()).isFalse();
    assertThat(underTest.isStartupLeader()).isFalse();
  }

  @Test
  public void getNodeName_whenNotACluster_isEmpty() {
    settings.setProperty("sonar.cluster.enabled", "false");
    settings.setProperty("sonar.cluster.node.name", "nameIgnored");

    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.getNodeName()).isEmpty();
  }

  @Test
  public void getNodeName_whenClusterAndNameNotDefined_fallbacksToDefaultName() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.removeProperty("sonar.cluster.node.name");

    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.getNodeName()).isNotEmpty();
    String nodeNameFirstCallToGetNodeName = underTest.getNodeName().get();
    assertThat(nodeNameFirstCallToGetNodeName).startsWith("sonarqube-");
    String nodeNameSecondCallToGetNodeName = underTest.getNodeName().get();
    assertThat(nodeNameFirstCallToGetNodeName).isEqualTo(nodeNameSecondCallToGetNodeName);
  }

  @Test
  public void getNodeName_whenClusterAndNameDefined_returnName() {
    String nodeName = "nodeName1";
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.node.name", nodeName);

    DefaultNodeInformation underTest = new DefaultNodeInformation(settings.asConfig());

    assertThat(underTest.getNodeName()).isNotEmpty();
    assertThat(underTest.getNodeName().get()).startsWith(nodeName);
  }

}
