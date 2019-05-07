/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.ServiceLoaderWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.parseTimeoutMs;

public class ProcessPropertiesTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServiceLoaderWrapper serviceLoaderWrapper = mock(ServiceLoaderWrapper.class);
  private ProcessProperties processProperties = new ProcessProperties(serviceLoaderWrapper);

  @Test
  public void completeDefaults_adds_default_values() {
    Props props = new Props(new Properties());

    processProperties.completeDefaults(props);

    assertThat(props.value("sonar.search.javaOpts")).contains("-Xmx");
    assertThat(props.valueAsInt("sonar.jdbc.maxActive")).isEqualTo(60);
    assertThat(props.valueAsBoolean("sonar.sonarcloud.enabled")).isEqualTo(false);
    assertThat(props.valueAsBoolean("sonar.updatecenter.activate")).isEqualTo(true);
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
  public void completeDefaults_set_default_elasticsearch_port_and_bind_address() throws Exception {
    Properties p = new Properties();
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    String address = props.value("sonar.search.host");
    assertThat(address).isNotEmpty();
    assertThat(InetAddress.getByName(address).isLoopbackAddress()).isTrue();
    assertThat(props.valueAsInt("sonar.search.port")).isEqualTo(9001);
  }

  @Test
  public void completeDefaults_sets_the_port_of_elasticsearch_if_value_is_zero() {
    Properties p = new Properties();
    p.setProperty("sonar.search.port", "0");
    Props props = new Props(p);

    processProperties.completeDefaults(props);

    assertThat(props.valueAsInt("sonar.search.port")).isGreaterThan(0);
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
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Configuration error: property definition named 'sonar.some.property2' found in multiple extensions.");

    processProperties.completeDefaults(p);
  }

  private class FakeExtension1 implements CoreExtension {

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

  private class FakeExtension2 implements CoreExtension {

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

  private class FakeExtension3 implements CoreExtension {

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
    expectedException.expect(NumberFormatException.class);

    parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, "tru");
  }

  @Test
  public void parseTimeoutMs_returns_long_from_value() {
    long expected = 1 + new Random().nextInt(5_999_663);

    long res = parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, expected + "");

    assertThat(res).isEqualTo(expected);
  }

  @Test
  public void parseTimeoutMs_throws_ISE_if_value_is_0() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("value of WEB_GRACEFUL_STOP_TIMEOUT must be >= 1");

    parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, 0 + "");
  }

  @Test
  public void parseTimeoutMs_throws_ISE_if_value_is_less_than_0() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("value of WEB_GRACEFUL_STOP_TIMEOUT must be >= 1");

    parseTimeoutMs(ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT, -(1 + new Random().nextInt(5_999_663)) + "");
  }
}
