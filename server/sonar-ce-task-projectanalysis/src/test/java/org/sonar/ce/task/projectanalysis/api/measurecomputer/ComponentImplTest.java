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
package org.sonar.ce.task.projectanalysis.api.measurecomputer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.Component;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_project() {
    ComponentImpl component = new ComponentImpl("Project", Component.Type.PROJECT, null);

    assertThat(component.getKey()).isEqualTo("Project");
    assertThat(component.getType()).isEqualTo(Component.Type.PROJECT);
  }

  @Test
  public void create_source_file() {
    ComponentImpl component = new ComponentImpl("File", Component.Type.FILE, new ComponentImpl.FileAttributesImpl("xoo", false));

    assertThat(component.getType()).isEqualTo(Component.Type.FILE);
    assertThat(component.getFileAttributes().getLanguageKey()).isEqualTo("xoo");
    assertThat(component.getFileAttributes().isUnitTest()).isFalse();
  }

  @Test
  public void create_test_file() {
    ComponentImpl component = new ComponentImpl("File", Component.Type.FILE, new ComponentImpl.FileAttributesImpl(null, true));

    assertThat(component.getType()).isEqualTo(Component.Type.FILE);
    assertThat(component.getFileAttributes().isUnitTest()).isTrue();
    assertThat(component.getFileAttributes().getLanguageKey()).isNull();
  }

  @Test
  public void fail_with_ISE_when_calling_get_file_attributes_on_not_file() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Only component of type FILE have a FileAttributes object");

    ComponentImpl component = new ComponentImpl("Project", Component.Type.PROJECT, null);
    component.getFileAttributes();
  }

  @Test
  public void fail_with_IAE_when_trying_to_create_a_file_without_file_attributes() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("omponent of type FILE must have a FileAttributes object");

    new ComponentImpl("File", Component.Type.FILE, null);
  }

  @Test
  public void fail_with_IAE_when_trying_to_create_not_a_file_with_file_attributes() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only component of type FILE have a FileAttributes object");

    new ComponentImpl("Project", Component.Type.PROJECT, new ComponentImpl.FileAttributesImpl(null, true));
  }

  @Test
  public void fail_with_NPE_when_creating_component_without_key() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Key cannot be null");

    new ComponentImpl(null, Component.Type.PROJECT, null);
  }

  @Test
  public void fail_with_NPE_when_creating_component_without_type() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Type cannot be null");

    new ComponentImpl("Project", null, null);
  }

  @Test
  public void test_equals_and_hashcode() {
    ComponentImpl component = new ComponentImpl("Project1", Component.Type.PROJECT, null);
    ComponentImpl sameComponent = new ComponentImpl("Project1", Component.Type.PROJECT, null);
    ComponentImpl anotherComponent = new ComponentImpl("Project2", Component.Type.PROJECT, null);

    assertThat(component).isEqualTo(component);
    assertThat(component).isEqualTo(sameComponent);
    assertThat(component).isNotEqualTo(anotherComponent);
    assertThat(component).isNotEqualTo(null);

    assertThat(component.hashCode()).isEqualTo(component.hashCode());
    assertThat(component.hashCode()).isEqualTo(sameComponent.hashCode());
    assertThat(component.hashCode()).isNotEqualTo(anotherComponent.hashCode());
  }

  @Test
  public void test_to_string() {
    assertThat(new ComponentImpl("File", Component.Type.FILE, new ComponentImpl.FileAttributesImpl("xoo", true)).toString())
      .isEqualTo("ComponentImpl{key=File, type='FILE', fileAttributes=FileAttributesImpl{languageKey='xoo', unitTest=true}}");
  }
}
