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
package org.sonar.server.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class WebServerImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();

  @Test
  public void cluster_is_disabled_by_default() {
    WebServerImpl underTest = new WebServerImpl(settings.asConfig());

    assertThat(underTest.isStandalone()).isTrue();
    assertThat(underTest.isStartupLeader()).isTrue();
  }

  @Test
  public void node_is_startup_leader_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.web.startupLeader", "true");

    WebServerImpl underTest = new WebServerImpl(settings.asConfig());

    assertThat(underTest.isStandalone()).isFalse();
    assertThat(underTest.isStartupLeader()).isTrue();
  }

  @Test
  public void node_is_startup_follower_by_default_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");

    WebServerImpl underTest = new WebServerImpl(settings.asConfig());

    assertThat(underTest.isStandalone()).isFalse();
    assertThat(underTest.isStartupLeader()).isFalse();
  }

  @Test
  public void node_is_startup_follower_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.web.startupLeader", "false");

    WebServerImpl underTest = new WebServerImpl(settings.asConfig());

    assertThat(underTest.isStandalone()).isFalse();
    assertThat(underTest.isStartupLeader()).isFalse();
  }

}
