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
package org.sonar.process.systeminfo;

import java.lang.management.ManagementFactory;
import javax.annotation.CheckForNull;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.junit.Test;
import org.sonar.process.jmx.FakeMBean;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseSectionMBeanTest {

  private static final String EXPECTED_NAME = "SonarQube:name=FakeName";

  private final FakeBaseSectionMBean underTest = new FakeBaseSectionMBean();

  @Test
  public void verify_mbean() throws Exception {
    assertThat(lookupMBean()).isNull();

    underTest.start();
    assertThat(lookupMBean()).isNotNull();

    underTest.stop();
    assertThat(lookupMBean()).isNull();

    assertThat(underTest.name()).isEqualTo("FakeName");
    assertThat(underTest.toProtobuf()).isNotNull();
  }

  @CheckForNull
  private ObjectInstance lookupMBean() throws Exception {
    try {
      return ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(EXPECTED_NAME));
    } catch (InstanceNotFoundException e) {
      return null;
    }
  }

  private static class FakeBaseSectionMBean extends BaseSectionMBean implements FakeMBean {

    @Override
    protected String name() {
      return "FakeName";
    }

    @Override
    public Section toProtobuf() {
      return Section.newBuilder().build();
    }

    @Override
    public void foo() {
      // nothing to do
    }
  }
}
