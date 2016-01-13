/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Properties;
import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessPropertiesTest {

  @Test
  public void init_defaults() {
    Props props = new Props(new Properties());
    ProcessProperties.completeDefaults(props);

    assertThat(props.value("sonar.search.javaOpts")).contains("-Xmx");
    assertThat(props.valueAsInt("sonar.jdbc.maxActive")).isEqualTo(60);
  }

  @Test
  public void do_not_override_existing_properties() {
    Properties p = new Properties();
    p.setProperty("sonar.jdbc.username", "angela");
    Props props = new Props(p);
    ProcessProperties.completeDefaults(props);

    assertThat(props.value("sonar.jdbc.username")).isEqualTo("angela");
  }

  @Test
  public void use_random_port_if_zero() {
    Properties p = new Properties();
    p.setProperty("sonar.search.port", "0");
    Props props = new Props(p);

    ProcessProperties.completeDefaults(props);
    assertThat(props.valueAsInt("sonar.search.port")).isGreaterThan(0);
  }

  @Test
  public void private_constructor() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ProcessProperties.class)).isTrue();
  }
}
