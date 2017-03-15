/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.application.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.ELASTICSEARCH;
import static org.sonar.process.ProcessId.WEB_SERVER;

public class ClusterSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestAppSettings settings = new TestAppSettings();

  @Test
  public void test_isClusterEnabled() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isTrue();

    settings.set(ProcessProperties.CLUSTER_ENABLED, "false");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isFalse();
  }

  @Test
  public void isClusterEnabled_returns_false_by_default() {
    assertThat(ClusterSettings.isClusterEnabled(settings)).isFalse();
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_by_default() {
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, ELASTICSEARCH, WEB_SERVER);
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_by_default_in_cluster_mode() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");

    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, ELASTICSEARCH, WEB_SERVER);
  }

  @Test
  public void getEnabledProcesses_returns_configured_processes_in_cluster_mode() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");

    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, WEB_SERVER);
  }

  @Test
  public void accept_throws_MessageException_if_internal_property_for_web_leader_is_configured() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set("sonar.cluster.web.startupLeader", "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property [sonar.cluster.web.startupLeader] is forbidden");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_does_nothing_if_cluster_is_disabled() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "false");
    // this property is supposed to fail if cluster is enabled
    settings.set("sonar.cluster.web.startupLeader", "true");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_h2() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set("sonar.jdbc.url", "jdbc:h2:mem");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_default_jdbc_url() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_true_by_default() {
    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_true_by_default_in_cluster_mode() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_false_if_local_es_node_is_disabled_in_cluster_mode() {
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isFalse();
  }
}
