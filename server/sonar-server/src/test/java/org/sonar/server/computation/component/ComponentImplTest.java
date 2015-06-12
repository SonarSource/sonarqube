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
package org.sonar.server.computation.component;

import java.util.Collections;
import org.junit.Test;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentImplTest {

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_component_arg_is_Null() {
    new ComponentImpl(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getUuid_throws_UOE_if_uuid_has_not_been_set_yet() {
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), Collections.<Component>emptyList());
    component.getUuid();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getKey_throws_UOE_if_uuid_has_not_been_set_yet() {
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), Collections.<Component>emptyList());
    component.getKey();
  }

  @Test
  public void verify_setUuid() {
    String uuid = "toto";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), Collections.<Component>emptyList()).setUuid(uuid);
    assertThat(component.getUuid()).isEqualTo(uuid);
  }

  @Test
  public void verify_setKey() {
    String key = "toto";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), Collections.<Component>emptyList()).setKey(key);
    assertThat(component.getKey()).isEqualTo(key);
  }

  @Test
  public void get_name_from_batch_component() throws Exception {
    String name = "project";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setName(name).build(), Collections.<Component>emptyList());
    assertThat(component.getName()).isEqualTo(name);
  }

  @Test
  public void get_version_from_batch_component() throws Exception {
    String version = "1.0";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setVersion(version).build(), Collections.<Component>emptyList());
    assertThat(component.getVersion()).isEqualTo(version);
  }

  @Test
  public void get_is_unit_test_from_batch_component() throws Exception {
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setIsTest(true).build(), Collections.<Component>emptyList());
    assertThat(component.isUnitTest()).isTrue();
  }

  @Test
  public void convertType() {
    for (Constants.ComponentType componentType : Constants.ComponentType.values()) {
      assertThat(ComponentImpl.convertType(componentType)).isEqualTo(Component.Type.valueOf(componentType.name()));
    }
  }
}
