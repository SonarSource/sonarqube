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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;
import static org.sonar.process.ProcessId.ELASTICSEARCH;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.ProcessProperties.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.CLUSTER_HOSTS;
import static org.sonar.process.ProcessProperties.CLUSTER_SEARCH_DISABLED;
import static org.sonar.process.ProcessProperties.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.JDBC_URL;
import static org.sonar.process.ProcessProperties.SEARCH_HOST;


public class ClusterSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestAppSettings settings;

  @Before
  public void resetSettings() {
    settings = getClusterSettings();
  }

  @Test
  public void test_isClusterEnabled() {
    settings.set(CLUSTER_ENABLED, "true");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isTrue();

    settings.set(CLUSTER_ENABLED, "false");
    assertThat(ClusterSettings.isClusterEnabled(settings)).isFalse();
  }

  @Test
  public void isClusterEnabled_returns_false_by_default() {
    assertThat(ClusterSettings.isClusterEnabled(new TestAppSettings())).isFalse();
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_by_default() {
    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, ELASTICSEARCH, WEB_SERVER);
  }

  @Test
  public void getEnabledProcesses_returns_all_processes_by_default_in_cluster_mode() {
    settings.set(CLUSTER_ENABLED, "true");

    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, ELASTICSEARCH, WEB_SERVER);
  }

  @Test
  public void getEnabledProcesses_returns_configured_processes_in_cluster_mode() {
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");

    assertThat(ClusterSettings.getEnabledProcesses(settings)).containsOnly(COMPUTE_ENGINE, WEB_SERVER);
  }

  @Test
  public void accept_throws_MessageException_if_internal_property_for_web_leader_is_configured() {
    settings.set(CLUSTER_ENABLED, "true");
    settings.set("sonar.cluster.web.startupLeader", "true");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Property [sonar.cluster.web.startupLeader] is forbidden");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_search_enabled_with_loopback() {
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(CLUSTER_SEARCH_DISABLED, "false");
    settings.set(CLUSTER_SEARCH_HOSTS, "192.168.1.1,192.168.1.2");
    settings.set(SEARCH_HOST, "::1");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("The interface address [::1] of [sonar.search.host] must not be a loopback address");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_not_throwing_MessageException_if_search_disabled_with_loopback() {
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(CLUSTER_SEARCH_DISABLED, "true");
    settings.set(CLUSTER_SEARCH_HOSTS, "192.168.1.1,192.168.1.2");
    settings.set(SEARCH_HOST, "127.0.0.1");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_does_nothing_if_cluster_is_disabled() {
    settings.set(CLUSTER_ENABLED, "false");
    // this property is supposed to fail if cluster is enabled
    settings.set("sonar.cluster.web.startupLeader", "true");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_h2() {
    settings.set(CLUSTER_ENABLED, "true");
    settings.set("sonar.jdbc.url", "jdbc:h2:mem");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Embedded database is not supported in cluster mode");

    new ClusterSettings().accept(settings.getProps());
  }

  @Test
  public void accept_throws_MessageException_if_default_jdbc_url() {
    settings.clearProperty(JDBC_URL);

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
    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isTrue();
  }

  @Test
  public void isLocalElasticsearchEnabled_returns_false_if_local_es_node_is_disabled_in_cluster_mode() {
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");

    assertThat(ClusterSettings.isLocalElasticsearchEnabled(settings)).isFalse();
  }

  @Test
  public void accept_throws_MessageException_if_searchHost_is_missing() {
    settings.clearProperty(SEARCH_HOST);
    checkMandatoryProperty(SEARCH_HOST);
  }

  @Test
  public void accept_throws_MessageException_if_searchHost_is_blank() {
    settings.set(SEARCH_HOST, " ");
    checkMandatoryProperty(SEARCH_HOST);
  }

  @Test
  public void accept_throws_MessageException_if_clusterHosts_is_missing() {
    settings.clearProperty(CLUSTER_HOSTS);
    checkMandatoryProperty(CLUSTER_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_clusterHosts_is_blank() {
    settings.set(CLUSTER_HOSTS, " ");
    checkMandatoryProperty(CLUSTER_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_clusterSearchHosts_is_missing() {
    settings.clearProperty(CLUSTER_SEARCH_HOSTS);
    checkMandatoryProperty(CLUSTER_SEARCH_HOSTS);
  }

  @Test
  public void accept_throws_MessageException_if_clusterSearchHosts_is_blank() {
    settings.set(CLUSTER_SEARCH_HOSTS, " ");
    checkMandatoryProperty(CLUSTER_SEARCH_HOSTS);
  }

  private void checkMandatoryProperty(String key) {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage(format("Property [%s] is mandatory", key));

    new ClusterSettings().accept(settings.getProps());
  }

  private static TestAppSettings getClusterSettings() {
    TestAppSettings testAppSettings = new TestAppSettings()
      .set(CLUSTER_ENABLED, "true")
      .set(CLUSTER_SEARCH_HOSTS, "localhost")
      .set(CLUSTER_HOSTS, "192.168.233.1, 192.168.233.2,192.168.233.3")
      .set(SEARCH_HOST, "192.168.233.1")
      .set(JDBC_URL, "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance");
    return testAppSettings;
  }
}
