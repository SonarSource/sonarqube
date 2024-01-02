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

import java.lang.management.ManagementFactory;
import javax.annotation.CheckForNull;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.junit.Test;
import org.sonar.process.jmx.Fake;
import org.sonar.process.jmx.FakeMBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JmxTest {

  static final String FAKE_NAME = "SonarQube:name=Fake";


  FakeMBean mbean = new Fake();

  @Test
  public void register_and_unregister() throws Exception {
    assertThat(lookupMBean()).isNull();

    Jmx.register(FAKE_NAME, mbean);
    assertThat(lookupMBean()).isNotNull();

    Jmx.unregister(FAKE_NAME);
    assertThat(lookupMBean()).isNull();
  }

  @Test
  public void do_not_fail_when_unregistering_a_non_registered_bean() throws Exception {
    Jmx.unregister(FAKE_NAME);
    assertThat(lookupMBean()).isNull();
  }

  @Test
  public void register_fails_if_mbean_interface_can_not_be_found() {
    assertThatThrownBy(() -> Jmx.register(FAKE_NAME, "not a mbean"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Can not find the MBean interface of class java.lang.String");
  }

  @Test
  public void register_fails_if_name_is_not_valid() {
    assertThatThrownBy(() -> Jmx.register("/", new Fake()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not register MBean [/]");
  }

  @Test
  public void support_implementation_in_different_package_than_interface() throws Exception {
    assertThat(lookupMBean()).isNull();

    Jmx.register(FAKE_NAME, new org.sonar.process.jmx.test.Fake());
    assertThat(lookupMBean()).isNotNull();

    Jmx.unregister(FAKE_NAME);
    assertThat(lookupMBean()).isNull();
  }

  @CheckForNull
  private ObjectInstance lookupMBean() throws Exception {
    try {
      return ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(FAKE_NAME));
    } catch (InstanceNotFoundException e) {
      return null;
    }
  }

}
