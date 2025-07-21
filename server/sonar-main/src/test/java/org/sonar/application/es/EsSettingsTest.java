/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.application.es;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.application.logging.ListAppender;
import org.sonar.core.extension.ServiceLoaderWrapper;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;
import org.sonar.process.ProcessProperties.Property;
import org.sonar.process.Props;
import org.sonar.process.System2;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HTTP_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_TRUSTSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_ES_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_SEARCH_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.ES_PORT;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_INITIAL_STATE_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

@RunWith(DataProviderRunner.class)
public class EsSettingsTest {

  private static final boolean CLUSTER_ENABLED = true;
  private static final boolean CLUSTER_DISABLED = false;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ListAppender listAppender;

  private final System2 system = mock(System2.class);

  @After
  public void tearDown() {
    if (listAppender != null) {
      ListAppender.detachMemoryAppenderToLoggerOf(EsSettings.class, listAppender);
    }
  }

  @Test
  public void constructor_does_not_logs_warning_if_env_variable_ES_JVM_OPTIONS_is_not_set() {
    this.listAppender = ListAppender.attachMemoryAppenderToLoggerOf(EsSettings.class);
    Props props = minimalProps();
    new EsSettings(props, new EsInstallation(props), system);

    assertThat(listAppender.getLogs()).isEmpty();
  }

  @Test
  public void constructor_does_not_logs_warning_if_env_variable_ES_JVM_OPTIONS_is_set_and_empty() {
    this.listAppender = ListAppender.attachMemoryAppenderToLoggerOf(EsSettings.class);
    Props props = minimalProps();
    when(system.getenv("ES_JVM_OPTIONS")).thenReturn("  ");
    new EsSettings(props, new EsInstallation(props), system);

    assertThat(listAppender.getLogs()).isEmpty();
  }

  @Test
  public void constructor_logs_warning_if_env_variable_ES_JVM_OPTIONS_is_set_and_non_empty() {
    this.listAppender = ListAppender.attachMemoryAppenderToLoggerOf(EsSettings.class);
    Props props = minimalProps();
    when(system.getenv("ES_JVM_OPTIONS")).thenReturn(secure().nextAlphanumeric(2));
    new EsSettings(props, new EsInstallation(props), system);

    assertThat(listAppender.getLogs())
      .extracting(ILoggingEvent::getMessage)
      .containsOnly("ES_JVM_OPTIONS is defined but will be ignored. " +
        "Use sonar.search.javaOpts and/or sonar.search.javaAdditionalOpts in sonar.properties to specify jvm options for Elasticsearch");
  }

  private Props minimalProps() {
    Props props = new Props(new Properties());
    props.set(PATH_HOME.getKey(), secure().nextAlphanumeric(12));
    props.set(PATH_DATA.getKey(), secure().nextAlphanumeric(12));
    props.set(PATH_TEMP.getKey(), secure().nextAlphanumeric(12));
    props.set(PATH_LOGS.getKey(), secure().nextAlphanumeric(12));
    props.set(CLUSTER_NAME.getKey(), secure().nextAlphanumeric(12));
    return props;
  }

  @Test
  public void test_default_settings_for_standalone_mode() throws Exception {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(SEARCH_PORT.getKey(), "1234");
    props.set(ES_PORT.getKey(), "5678");
    props.set(SEARCH_HOST.getKey(), "127.0.0.1");
    props.set(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    props.set(CLUSTER_NAME.getKey(), "sonarqube");

    EsSettings esSettings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> generated = esSettings.build();

    assertThat(generated).containsEntry("transport.port", "5678")
      .containsEntry("transport.host", "127.0.0.1")
      .containsEntry("http.port", "1234")
      .containsEntry("http.host", "127.0.0.1")
      // no cluster, but cluster and node names are set though
      .containsEntry("cluster.name", "sonarqube")
      .containsEntry("node.name", "sonarqube")
      .containsEntry("discovery.type", "single-node")
      .doesNotContainKey("discovery.seed_hosts")
      .doesNotContainKey("cluster.initial_master_nodes");

    assertThat(generated.get("path.data")).isNotNull();
    assertThat(generated.get("path.logs")).isNotNull();
    assertThat(generated.get("path.home")).isNull();
    assertThat(generated.get("path.conf")).isNull();

    assertThat(generated)
      .containsEntry("discovery.initial_state_timeout", "30s")
      .containsEntry("action.auto_create_index", "false");
  }

  @Test
  public void test_default_settings_for_cluster_mode() throws Exception {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(SEARCH_PORT.getKey(), "1234");
    props.set(SEARCH_HOST.getKey(), "127.0.0.1");
    props.set(CLUSTER_NODE_SEARCH_HOST.getKey(), "127.0.0.1");
    props.set(CLUSTER_NODE_SEARCH_PORT.getKey(), "1234");
    props.set(CLUSTER_NODE_ES_HOST.getKey(), "127.0.0.1");
    props.set(CLUSTER_NODE_ES_PORT.getKey(), "1234");
    props.set(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    props.set(CLUSTER_NAME.getKey(), "sonarqube-1");
    props.set(Property.CLUSTER_ENABLED.getKey(), "true");
    props.set(CLUSTER_NODE_NAME.getKey(), "node-1");

    EsSettings esSettings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> generated = esSettings.build();
    assertThat(generated)
      .containsEntry("cluster.name", "sonarqube-1")
      .containsEntry("node.name", "node-1");
  }

  @Test
  public void test_node_name_default_for_cluster_mode() throws Exception {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(CLUSTER_NAME.getKey(), "sonarqube");
    props.set(Property.CLUSTER_ENABLED.getKey(), "true");
    props.set(SEARCH_PORT.getKey(), "1234");
    props.set(SEARCH_HOST.getKey(), "127.0.0.1");
    props.set(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    EsSettings esSettings = new EsSettings(props, new EsInstallation(props), system);
    Map<String, String> generated = esSettings.build();
    assertThat(generated.get("node.name")).startsWith("sonarqube-");
  }

  @Test
  public void test_node_name_default_for_standalone_mode() throws Exception {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(CLUSTER_NAME.getKey(), "sonarqube");
    props.set(Property.CLUSTER_ENABLED.getKey(), "false");
    props.set(SEARCH_PORT.getKey(), "1234");
    props.set(ES_PORT.getKey(), "5678");
    props.set(SEARCH_HOST.getKey(), "127.0.0.1");
    props.set(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    EsSettings esSettings = new EsSettings(props, new EsInstallation(props), system);
    Map<String, String> generated = esSettings.build();
    assertThat(generated).containsEntry("node.name", "sonarqube");
  }

  @Test
  public void path_properties_are_values_from_EsFileSystem_argument() throws IOException {
    File foo = temp.newFolder();
    EsInstallation mockedEsInstallation = mock(EsInstallation.class);
    File home = new File(foo, "home");
    when(mockedEsInstallation.getHomeDirectory()).thenReturn(home);
    File conf = new File(foo, "conf");
    when(mockedEsInstallation.getConfDirectory()).thenReturn(conf);
    File log = new File(foo, "log");
    when(mockedEsInstallation.getLogDirectory()).thenReturn(log);
    File data = new File(foo, "data");
    when(mockedEsInstallation.getDataDirectory()).thenReturn(data);

    EsSettings underTest = new EsSettings(minProps(true), mockedEsInstallation, system);

    Map<String, String> generated = underTest.build();
    assertThat(generated)
      .containsEntry("path.data", data.getPath())
      .containsEntry("path.logs", log.getPath());
    assertThat(generated.get("path.conf")).isNull();
  }

  @Test
  public void set_discovery_settings_if_cluster_is_enabled() throws Exception {
    Props props = minProps(CLUSTER_ENABLED);
    props.set(CLUSTER_ES_HOSTS.getKey(), "1.2.3.4:9000,1.2.3.5:8080");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings)
      .containsEntry("discovery.seed_hosts", "1.2.3.4:9000,1.2.3.5:8080")
      .containsEntry("discovery.initial_state_timeout", "120s");
  }

  @Test
  public void set_initial_master_nodes_settings_if_cluster_is_enabled() throws Exception {
    Props props = minProps(CLUSTER_ENABLED);
    props.set(CLUSTER_ES_HOSTS.getKey(), "1.2.3.4:9000,1.2.3.5:8080");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), System2.INSTANCE).build();

    assertThat(settings)
      .containsEntry("cluster.initial_master_nodes", "1.2.3.4:9000,1.2.3.5:8080")
      .containsEntry("discovery.initial_state_timeout", "120s");
  }

  @Test
  public void cluster_is_enabled_with_defined_initialTimeout() throws Exception {
    Props props = minProps(CLUSTER_ENABLED);
    props.set(SEARCH_INITIAL_STATE_TIMEOUT.getKey(), "10s");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings).containsEntry("discovery.initial_state_timeout", "10s");
  }

  @Test
  public void in_standalone_initialTimeout_is_not_overridable() throws Exception {
    Props props = minProps(CLUSTER_DISABLED);
    props.set(SEARCH_INITIAL_STATE_TIMEOUT.getKey(), "10s");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings).containsEntry("discovery.initial_state_timeout", "30s");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void enable_http_connector_on_port_9001_by_default(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings)
      .containsEntry("http.port", "9001")
      .containsEntry("http.host", "127.0.0.1");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void enable_http_connector_on_specified_port(boolean clusterEnabled) throws Exception {
    String port = "" + 49150;
    Props props = minProps(clusterEnabled, null, port);
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), System2.INSTANCE).build();

    assertThat(settings)
      .containsEntry("http.port", port)
      .containsEntry("http.host", "127.0.0.1");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void enable_http_connector_different_host(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled, "127.0.0.2", null);
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings)
      .containsEntry("http.port", "9001")
      .containsEntry("http.host", "127.0.0.2");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void enable_seccomp_filter_by_default(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings.get("bootstrap.system_call_filter")).isNull();
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void disable_seccomp_filter_if_configured_in_search_additional_props(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    props.set("sonar.search.javaAdditionalOpts", "-Xmx1G -Dbootstrap.system_call_filter=false -Dfoo=bar");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings).containsEntry("bootstrap.system_call_filter", "false");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void disable_mmap_if_configured_in_search_additional_props(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    props.set("sonar.search.javaAdditionalOpts", "-Dnode.store.allow_mmap=false");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings).containsEntry("node.store.allow_mmap", "false");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void throw_exception_if_old_mmap_property_is_used(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    props.set("sonar.search.javaAdditionalOpts", "-Dnode.store.allow_mmapfs=false");
    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    assertThatThrownBy(settings::build)
      .isInstanceOf(MessageException.class)
      .hasMessage("Property 'node.store.allow_mmapfs' is no longer supported. Use 'node.store.allow_mmap' instead.");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void disable_disk_threshold_if_configured_in_search_additional_props(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    props.set("sonar.search.javaAdditionalOpts", "-Dcluster.routing.allocation.disk.threshold_enabled=false");
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings).containsEntry("cluster.routing.allocation.disk.threshold_enabled", "false");
  }

  @Test
  @UseDataProvider("clusterEnabledOrNot")
  public void disk_threshold_not_set_by_default(boolean clusterEnabled) throws Exception {
    Props props = minProps(clusterEnabled);
    Map<String, String> settings = new EsSettings(props, new EsInstallation(props), system).build();

    assertThat(settings.get("cluster.routing.allocation.disk.threshold_enabled")).isNull();
  }

  @Test
  public void configureSecurity_givenClusterSearchPasswordNotProvided_dontAddXpackParameters() throws Exception {
    Props props = minProps(true);

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> outputParams = settings.build();

    assertThat(outputParams.get("xpack.security.transport.ssl.enabled")).isNull();
  }

  @Test
  public void configureSecurity_givenClusterSearchPasswordProvided_addXpackParameters_file_exists() throws Exception {
    Props props = minProps(true);
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");
    File keystore = temp.newFile("keystore.p12");
    File truststore = temp.newFile("truststore.p12");

    props.set(CLUSTER_ES_KEYSTORE.getKey(), keystore.getAbsolutePath());
    props.set(CLUSTER_ES_TRUSTSTORE.getKey(), truststore.getAbsolutePath());

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> outputParams = settings.build();

    assertThat(outputParams)
      .containsEntry("xpack.security.transport.ssl.enabled", "true")
      .containsEntry("xpack.security.transport.ssl.supported_protocols", "TLSv1.3,TLSv1.2")
      .containsEntry("xpack.security.transport.ssl.keystore.path", keystore.getName())
      .containsEntry("xpack.security.transport.ssl.truststore.path", truststore.getName());
  }

  @Test
  public void configureSecurity_givenClusterSearchPasswordProvidedButKeystorePathMissing_throwException() throws Exception {
    Props props = minProps(true);
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    assertThatThrownBy(settings::build)
      .isInstanceOf(MessageException.class)
      .hasMessage("CLUSTER_ES_KEYSTORE property need to be set when using elastic search authentication");
  }

  @Test
  public void configureSecurity_givenClusterModeFalse_dontAddXpackParameters() throws Exception {
    Props props = minProps(false);
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> outputParams = settings.build();

    assertThat(outputParams.get("xpack.security.transport.ssl.enabled")).isNull();
  }

  @Test
  public void configureSecurity_givenFileNotExist_throwException() throws Exception {
    Props props = minProps(true);
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");
    File truststore = temp.newFile("truststore.p12");

    props.set(CLUSTER_ES_KEYSTORE.getKey(), "not-existing-file");
    props.set(CLUSTER_ES_TRUSTSTORE.getKey(), truststore.getAbsolutePath());

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    assertThatThrownBy(settings::build)
      .isInstanceOf(MessageException.class)
      .hasMessage("Unable to configure: sonar.cluster.es.ssl.keystore. File specified in [not-existing-file] does not exist");
  }

  @Test
  public void configureSecurity_whenHttpKeystoreProvided_shouldAddHttpProperties() throws Exception {
    Props props = minProps(true);
    File keystore = temp.newFile("keystore.p12");
    File truststore = temp.newFile("truststore.p12");
    File httpKeystore = temp.newFile("http-keystore.p12");
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");
    props.set(CLUSTER_ES_KEYSTORE.getKey(), keystore.getAbsolutePath());
    props.set(CLUSTER_ES_TRUSTSTORE.getKey(), truststore.getAbsolutePath());
    props.set(CLUSTER_ES_HTTP_KEYSTORE.getKey(), httpKeystore.getAbsolutePath());

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> outputParams = settings.build();

    assertThat(outputParams)
      .containsEntry("xpack.security.http.ssl.enabled", "true")
      .containsEntry("xpack.security.http.ssl.keystore.path", httpKeystore.getName());
  }

  @Test
  public void configureSecurity_whenHttpKeystoreNotProvided_shouldNotAddHttpProperties() throws Exception {
    Props props = minProps(true);
    File keystore = temp.newFile("keystore.p12");
    File truststore = temp.newFile("truststore.p12");
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");
    props.set(CLUSTER_ES_KEYSTORE.getKey(), keystore.getAbsolutePath());
    props.set(CLUSTER_ES_TRUSTSTORE.getKey(), truststore.getAbsolutePath());

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    Map<String, String> outputParams = settings.build();

    assertThat(outputParams)
      .doesNotContainKey("xpack.security.http.ssl.enabled")
      .doesNotContainKey("xpack.security.http.ssl.keystore.path");
  }

  @Test
  public void configureSecurity_whenHttpKeystoreProvided_shouldFailIfNotExists() throws Exception {
    Props props = minProps(true);
    File keystore = temp.newFile("keystore.p12");
    File truststore = temp.newFile("truststore.p12");
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "qwerty");
    props.set(CLUSTER_ES_KEYSTORE.getKey(), keystore.getAbsolutePath());
    props.set(CLUSTER_ES_TRUSTSTORE.getKey(), truststore.getAbsolutePath());
    props.set(CLUSTER_ES_HTTP_KEYSTORE.getKey(), "not-existing-file");

    EsSettings settings = new EsSettings(props, new EsInstallation(props), system);

    assertThatThrownBy(settings::build)
      .isInstanceOf(MessageException.class)
      .hasMessage("Unable to configure: sonar.cluster.es.http.ssl.keystore. File specified in [not-existing-file] does not exist");
  }

  @DataProvider
  public static Object[][] clusterEnabledOrNot() {
    return new Object[][] {
      {false},
      {true}
    };
  }

  private Props minProps(boolean cluster) throws IOException {
    return minProps(cluster, null, null);
  }

  private Props minProps(boolean cluster, @Nullable String host, @Nullable String port) throws IOException {
    File homeDir = temp.newFolder();
    Props props = new Props(new Properties());
    ServiceLoaderWrapper serviceLoaderWrapper = mock(ServiceLoaderWrapper.class);
    when(serviceLoaderWrapper.load()).thenReturn(ImmutableSet.of());
    new ProcessProperties(serviceLoaderWrapper).completeDefaults(props);
    props.set(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    props.set(Property.CLUSTER_ENABLED.getKey(), Boolean.toString(cluster));
    if (cluster) {
      ofNullable(host).ifPresent(h -> props.set(CLUSTER_NODE_ES_HOST.getKey(), h));
      ofNullable(port).ifPresent(p -> props.set(CLUSTER_NODE_ES_PORT.getKey(), p));
      ofNullable(host).ifPresent(h -> props.set(CLUSTER_NODE_SEARCH_HOST.getKey(), h));
      ofNullable(port).ifPresent(p -> props.set(CLUSTER_NODE_SEARCH_PORT.getKey(), p));
      ofNullable(port).ifPresent(h -> props.set(CLUSTER_ES_HOSTS.getKey(), h));
    } else {
      ofNullable(host).ifPresent(h -> props.set(SEARCH_HOST.getKey(), h));
      ofNullable(port).ifPresent(p -> props.set(SEARCH_PORT.getKey(), p));
    }

    return props;
  }
}
