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
import java.util.List;
import org.junit.Test;
import org.sonar.batch.protocol.output.BatchReport;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.batch.protocol.Constants.ComponentType;

public class ComponentImplTest {

  private static final List<Component> EMPTY_CHILD_LIST = Collections.<Component>emptyList();

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_component_arg_is_Null() {
    new ComponentImpl(null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getUuid_throws_UOE_if_uuid_has_not_been_set_yet() {
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), EMPTY_CHILD_LIST);
    component.getUuid();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getKey_throws_UOE_if_uuid_has_not_been_set_yet() {
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), EMPTY_CHILD_LIST);
    component.getKey();
  }

  @Test
  public void verify_setUuid() {
    String uuid = "toto";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), EMPTY_CHILD_LIST).setUuid(uuid);
    assertThat(component.getUuid()).isEqualTo(uuid);
  }

  @Test
  public void verify_setKey() {
    String key = "toto";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), EMPTY_CHILD_LIST).setKey(key);
    assertThat(component.getKey()).isEqualTo(key);
  }

  @Test
  public void get_name_from_batch_component() {
    String name = "project";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setName(name).build(), EMPTY_CHILD_LIST);
    assertThat(component.getName()).isEqualTo(name);
  }

  @Test
  public void get_version_from_batch_component() {
    String version = "1.0";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setVersion(version).build(), EMPTY_CHILD_LIST);
    assertThat(component.getReportAttributes().getVersion()).isEqualTo(version);
  }

  @Test
  public void getFileAttributes_throws_ISE_if_BatchComponent_does_not_have_type_FILE() {
    for (ComponentType componentType : from(asList(ComponentType.values())).filter(not(equalTo(ComponentType.FILE)))) {
      ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setType(componentType).build(), EMPTY_CHILD_LIST);
      try {
        component.getFileAttributes();
        fail("A IllegalStateException should have been raised");
      } catch (IllegalStateException e) {
        assertThat(e).hasMessage("Only component of type FILE have a FileAttributes object");
      }
    }
  }

  @Test
  public void isUnitTest_returns_true_if_IsTest_is_set_in_BatchComponent() {
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setType(ComponentType.FILE).setIsTest(true).build(), EMPTY_CHILD_LIST);

    assertThat(component.getFileAttributes().isUnitTest()).isTrue();
  }

  @Test
  public void isUnitTest_returns_value_of_language_of_BatchComponent() {
    String languageKey = "some language key";
    ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().setType(ComponentType.FILE).setLanguage(languageKey).build(), EMPTY_CHILD_LIST);

    assertThat(component.getFileAttributes().getLanguageKey()).isEqualTo(languageKey);
  }

  @Test
  public void convertType() {
    for (ComponentType componentType : ComponentType.values()) {
      assertThat(ComponentImpl.convertType(componentType)).isEqualTo(Component.Type.valueOf(componentType.name()));
    }
  }
}
