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
package org.sonar.process;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.junit.Test;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.ServiceLoaderWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.parseTimeoutMs;

public class ProcessPropertiesTest {

  private final ServiceLoaderWrapper serviceLoaderWrapper = mock(ServiceLoaderWrapper.class);
  private final ProcessProperties processProperties = new ProcessProperties(serviceLoaderWrapper);

  @Test
  public void completeDefaults_adds_default_values() {
    Props props = new Props(new Properties());

    processProperties.completeDefaults(props);

    assertThat(props.value("sonar.search.javaOpts")).contains("-Xmx");
    assertThat(props.valueAsInt("sonar.jdbc.maxActive")).isEqualTo(60);
    assertThat(props.valueAsBoolean("sonar.updatecenter.activate")).isTrue();
  }

  @Test
  public void completeDefaults_does_not_override_existing_properties() {
    Properties p = new Properties();
    p.setProperty("sonar.jdbc.username", "angela");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.value("sonar.jdbc.username")).isEqualTo("angela");
  }

  @Test
  public void completeDefaults_sets_default_values_for_sonar_search_host_and_sonar_search_port_and_random_port_for_sonar_es_port_in_non_cluster_mode() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.cluster.enabled", "false");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    String address = props.value("sonar.search.host");
    assertThat(address).isNotEmpty();
    assertThat(InetAddress.getByName(address).isLoopbackAddress()).isTrue();
    assertThat(props.valueAsInt("sonar.search.port")).isEqualTo(9001);
    assertThat(props.valueAsInt("sonar.es.port")).isPositive();
  }

  @Test
  public void completeDefaults_does_not_set_default_values_for_sonar_search_host_and_sonar_search_port_and_sonar_es_port_in_cluster_mode() {
    Properties p = new Properties();
    p.setProperty("sonar.cluster.enabled", "true");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.contains("sonar.search.port")).isFalse();
    assertThat(props.contains("sonar.search.port")).isFalse();
    assertThat(props.contains("sonar.es.port")).isFalse();
  }

  @Test
  public void completeDefaults_sets_the_http_port_of_elasticsearch_if_value_is_zero_in_standalone_mode() {
    Properties p = new Properties();
    p.setProperty("sonar.search.port", "0");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.valueAsInt("sonar.search.port")).isPositive();
  }

  @Test
  public void completeDefaults_does_not_fall_back_to_default_if_transport_port_of_elasticsearch_set_in_standalone_mode() {
    Properties p = new Properties();
    p.setProperty("sonar.es.port", "9002");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.valueAsInt("sonar.es.port")).isEqualTo(9002);
  }

  @Test
  public void completeDefaults_does_not_set_the_http_port_of_elasticsearch_if_value_is_zero_in_search_node_in_cluster() {
    Properties p = new Properties();
    p.setProperty("sonar.cluster.enabled", "true");
    p.setProperty("sonar.search.port", "0");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.valueAsInt("sonar.search.port")).isZero();
  }

  @Test
  public void completeDefaults_does_not_set_the_transport_port_of_elasticsearch_if_value_is_zero_in_search_node_in_cluster() {
    Properties p = new Properties();
    p.setProperty("sonar.cluster.enabled", "true");
    p.setProperty("sonar.es.port", "0");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.valueAsInt("sonar.es.port")).isZero();
  }

  @Test
  public void defaults_loads_properties_defaults_from_base_and_extensions() {
    Props p = new Props(new Properties());
    when(serviceLoaderWrapper.load()).thenReturn(ImmutableSet.of(new FakeExtension1(), new FakeExtension3()));

    processProperties.completeDefaults(p);

    assertThat(p.value("sonar.some.property")).isEqualTo("1");
    assertThat(p.value("sonar.some.property2")).isEqualTo("455");
    assertThat(p.value("sonar.some.property4")).isEqualTo("abc");
    assertThat(p.value("sonar.some.property5")).isEqualTo("def");
    assertThat(p.value("sonar.some.property5")).isEqualTo("def");
    assertThat(p.value("sonar.search.port")).isEqualTo("9001");
  }

  @Test
  public void defaults_throws_exception_on_same_property_defined_more_than_once_in_extensions() {
    Props p = new Props(new Properties());
    when(serviceLoaderWrapper.load()).thenReturn(ImmutableSet.of(new FakeExtension1(), new FakeExtension2()));

    assertThatThrownBy(() -> processProperties.completeDefaults(p))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Configuration error: property definition named 'sonar.some.property2' found in multiple extensions.");
  }

  private static class FakeExtension1 implements CoreExtension {

    @Override
    public String getName() {
      return "fakeExt1";
    }

    @Override
    public void load(Context context) {
      // do nothing
    }

    @Override
    public Map<String, String> getExtensionProperties() {
      return ImmutableMap.of(
        "sonar.some.property", "1",
        "sonar.some.property2", "455");
    }
  }

  private static class FakeExtension2 implements CoreExtension {

    @Override
    public String getName() {
      return "fakeExt2";
    }

    @Override
    public void load(Context context) {
      // do nothing
    }

    @Override
    public Map<String, String> getExtensionProperties() {
      return ImmutableMap.of(
        "sonar.some.property2", "5435",
        "sonar.some.property3", "32131");
    }
  }

  private static class FakeExtension3 implements CoreExtension {

    @Override
    public String getName() {
      return "fakeExt3";
    }

    @Override
    public void load(Context context) {
      // do nothing
    }

    @Override
    public Map<String, String> getExtensionProperties() {
      return ImmutableMap.of(
        "sonar.some.property4", "abc",
        "sonar.some.property5", "def");
    }
  }

  @Test
  public void parseTimeoutMs_throws_NumberFormat_exception_if_value_is_not_long() {
    assertThatThrownBy(() -> parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, "tru"))
      .isInstanceOf(NumberFormatException.class);
  }

  @Test
  public void parseTimeoutMs_returns_long_from_value() {
    long expected = 1 + new Random().nextInt(5_999_663);

    long res = parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, expected + "");

    assertThat(res).isEqualTo(expected);
  }

  @Test
  public void parseTimeoutMs_throws_ISE_if_value_is_0() {
    assertThatThrownBy(() -> parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, 0 + ""))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("value of WEB_GRACEFUL_STOP_TIMEOUT must be >= 1");
  }

  @Test
  public void parseTimeoutMs_throws_ISE_if_value_is_less_than_0() {
    int timeoutValue = -(1 + new Random().nextInt(5_999_663));
    assertThatThrownBy(() -> parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, timeoutValue + ""))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("value of WEB_GRACEFUL_STOP_TIMEOUT must be >= 1");
  }
}
