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
package org.sonar.ce.monitoring;

import java.lang.management.ManagementFactory;
import javax.annotation.CheckForNull;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class CeDatabaseMBeanImplTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private CeDatabaseMBeanImpl underTest = new CeDatabaseMBeanImpl(dbTester.getDbClient());

  @Test
  public void register_and_unregister() throws Exception {
    assertThat(getMBean()).isNull();

    underTest.start();
    assertThat(getMBean()).isNotNull();

    underTest.stop();
    assertThat(getMBean()).isNull();
  }

  @Test
  public void export_system_info() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(section.getName()).isEqualTo("Compute Engine Database Connection");
    assertThat(section.getAttributesCount()).isEqualTo(9);
    assertThat(section.getAttributes(0).getKey()).isEqualTo("Pool Initial Size");
    assertThat(section.getAttributes(0).getLongValue()).isGreaterThanOrEqualTo(0);
  }

  @CheckForNull
  private ObjectInstance getMBean() throws Exception {
    try {
      return ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(CeDatabaseMBean.OBJECT_NAME));
    } catch (InstanceNotFoundException e) {
      return null;
    }
  }
}
