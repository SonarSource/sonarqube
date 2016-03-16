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
package org.sonar.process.jmx;

import java.lang.management.ManagementFactory;
import javax.annotation.CheckForNull;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxTest {

  static final String FAKE_NAME = "SonarQube:name=Fake";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  FakeMBean mbean = new Fake();
  Jmx underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new Jmx(temp.newFolder());
  }

  @Test
  public void register_and_unregister() throws Exception {
    assertThat(lookupMBean()).isNull();

    underTest.register(FAKE_NAME, mbean);
    assertThat(lookupMBean()).isNotNull();

    underTest.unregister(FAKE_NAME);
    assertThat(lookupMBean()).isNull();
  }

  @Test
  public void do_not_fail_when_unregistering_a_non_registered_bean() throws Exception {
    underTest.unregister(FAKE_NAME);
    assertThat(lookupMBean()).isNull();
  }

  @Test
  public void fail_if_mbean_interface_can_not_be_found() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Can not find the MBean interface of class java.lang.String");

    underTest.register(FAKE_NAME, "not a mbean");
  }

  @Test
  public void support_implementation_in_different_package_than_interface() throws Exception {
    assertThat(lookupMBean()).isNull();

    underTest.register(FAKE_NAME, new org.sonar.process.jmx.test.Fake());
    assertThat(lookupMBean()).isNotNull();

    underTest.unregister(FAKE_NAME);
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
