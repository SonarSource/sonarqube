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

package org.sonar.application;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ClusterPropertiesTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_default_values() throws Exception {
    ClusterProperties props = new ClusterProperties(new Props(new Properties()));

    assertThat(props.getInterfaces())
      .isEqualTo(Collections.emptyList());
    assertThat(props.isPortAutoincrement())
      .isEqualTo(false);
    assertThat(props.getPort())
      .isEqualTo(9003);
    assertThat(props.isEnabled())
      .isEqualTo(false);
    assertThat(props.getMembers())
      .isEqualTo(Collections.emptyList());
    assertThat(props.getName())
      .isEqualTo("");
    assertThat(props.getLogLevel())
      .isEqualTo("WARN");
  }


  @Test
  public void test_port_parameter() {
    Props props = new Props(new Properties());
    props.set(ClusterParameters.ENABLED.getName(), "true");
    props.set(ClusterParameters.NAME.getName(), "sonarqube");

    Stream.of("-50", "0", "65536", "128563").forEach(
      port -> {
        props.set(ClusterParameters.PORT.getName(), port);

        ClusterProperties clusterProperties = new ClusterProperties(props);
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
          String.format("Cluster port have been set to %s which is outside the range [1-65535].", port)
        );
        clusterProperties.validate();

      }
    );
  }

  @Test
  public void test_interfaces_parameter() {
    Props props = new Props(new Properties());
    props.set(ClusterParameters.ENABLED.getName(), "true");
    props.set(ClusterParameters.NAME.getName(), "sonarqube");
    props.set(ClusterParameters.INTERFACES.getName(), "8.8.8.8"); // This IP belongs to Google

    ClusterProperties clusterProperties = new ClusterProperties(props);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(
      String.format("Interface %s is not available on this machine.", "8.8.8.8")
    );
    clusterProperties.validate();
  }

  @Test
  public void test_missing_name() {
    Props props = new Props(new Properties());
    props.set(ClusterParameters.ENABLED.getName(), "true");
    props.set(ClusterParameters.NAME.getName(), "");
    ClusterProperties clusterProperties = new ClusterProperties(props);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(
      String.format("Cluster have been enabled but a %s has not been defined.",
        ClusterParameters.NAME.getName())
    );
    clusterProperties.validate();
  }

  @Test
  public void validate_does_not_fail_if_cluster_enabled_and_name_specified() {
    Props props = new Props(new Properties());
    props.set(ClusterParameters.ENABLED.getName(), "true");
    props.set(ClusterParameters.NAME.getName(), "sonarqube");
    ClusterProperties clusterProperties = new ClusterProperties(props);
    clusterProperties.validate();
  }

  @Test
  public void test_members() {
    Props props = new Props(new Properties());
    props.set(ClusterParameters.ENABLED.getName(), "true");

    assertThat(
      retrieveMembers(props)
    ).isEqualTo(
      Collections.emptyList()
    );

    props.set(ClusterParameters.MEMBERS.getName(), "192.168.1.1");
    assertThat(
      retrieveMembers(props)
    ).isEqualTo(
      Arrays.asList("192.168.1.1:" + ClusterParameters.PORT.getDefaultValue())
    );

    props.set(ClusterParameters.MEMBERS.getName(), "192.168.1.2:5501");
    assertThat(
      retrieveMembers(props)
    ).containsExactlyInAnyOrder(
      "192.168.1.2:5501"
    );

    props.set(ClusterParameters.MEMBERS.getName(), "192.168.1.2:5501,192.168.1.1");
    assertThat(
      retrieveMembers(props)
    ).containsExactlyInAnyOrder(
      "192.168.1.2:5501", "192.168.1.1:" + ClusterParameters.PORT.getDefaultValue()
    );
  }

  @Test
  public void test_cluster_properties() {
    Props props = new Props(new Properties());
    props.set(ClusterParameters.ENABLED.getName(), "true");
    props.set(ClusterParameters.MEMBERS.getName(), "192.168.1.1");
    props.set(ClusterParameters.INTERFACES.getName(), "192.168.1.30");
    props.set(ClusterParameters.PORT.getName(), "9003");
    props.set(ClusterParameters.HAZELCAST_LOG_LEVEL.getName(), "INFO");
    props.set(ClusterParameters.PORT_AUTOINCREMENT.getName(), "false");
    props.set(ClusterParameters.NAME.getName(), "sonarqube");

    ClusterProperties clusterProperties = new ClusterProperties(props);
    clusterProperties.populateProps(props);

    assertThat(props.rawProperties()).containsOnly(
      entry(ClusterParameters.HAZELCAST_LOG_LEVEL.getName(), "INFO"),
      entry(ClusterParameters.PORT.getName(), "9003"),
      entry(ClusterParameters.ENABLED.getName(), "true"),
      entry(ClusterParameters.INTERFACES.getName(), "192.168.1.30"),
      entry(ClusterParameters.MEMBERS.getName(), "192.168.1.1:" + ClusterParameters.PORT.getDefaultValue()),
      entry(ClusterParameters.NAME.getName(), "sonarqube"),
      entry(ClusterParameters.PORT_AUTOINCREMENT.getName(), "false")
    );
  }

  private List<String> retrieveMembers(Props props) {
    return new ClusterProperties(props).getMembers();
  }
}
