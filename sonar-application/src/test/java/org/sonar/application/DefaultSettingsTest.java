/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.junit.Test;
import org.sonar.process.Props;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultSettingsTest {

  @Test
  public void init_defaults() throws Exception {
    Props props = new Props(new Properties());
    DefaultSettings.init(props);

    assertThat(props.of("sonar.search.javaOpts")).contains("-Xmx");
    assertThat(props.intOf("sonar.web.jmxPort")).isEqualTo(9003);
    assertThat(props.intOf("sonar.search.jmxPort")).isEqualTo(9002);
    assertThat(props.of("sonar.jdbc.username")).isEqualTo("sonar");
  }

  @Test
  public void do_not_override_existing_properties() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.jdbc.username", "angela");
    Props props = new Props(p);
    DefaultSettings.init(props);

    assertThat(props.of("sonar.jdbc.username")).isEqualTo("angela");
  }

  @Test
  public void use_random_port_if_zero() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.search.jmxPort", "0");
    Props props = new Props(p);

    DefaultSettings.init(props);
    assertThat(props.intOf("sonar.web.jmxPort")).isGreaterThan(0);
  }
}
