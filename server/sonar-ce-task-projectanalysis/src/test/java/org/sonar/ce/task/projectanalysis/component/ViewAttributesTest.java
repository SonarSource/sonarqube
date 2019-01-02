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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.ViewAttributes.Type.APPLICATION;
import static org.sonar.ce.task.projectanalysis.component.ViewAttributes.Type.PORTFOLIO;

public class ViewAttributesTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ViewAttributes underTest;

  @Test
  public void create_portfolio() {
    underTest = new ViewAttributes(PORTFOLIO);

    assertThat(underTest.getType()).isEqualTo(PORTFOLIO);
    assertThat(underTest.getType().getQualifier()).isEqualTo(Qualifiers.VIEW);
  }

  @Test
  public void create_application() {
    underTest = new ViewAttributes(APPLICATION);

    assertThat(underTest.getType()).isEqualTo(APPLICATION);
    assertThat(underTest.getType().getQualifier()).isEqualTo(Qualifiers.APP);
  }

  @Test
  public void type_from_qualifier() {
    assertThat(ViewAttributes.Type.fromQualifier(Qualifiers.VIEW)).isEqualTo(PORTFOLIO);
    assertThat(ViewAttributes.Type.fromQualifier(Qualifiers.APP)).isEqualTo(APPLICATION);
  }

  @Test
  public void fail_if_unknown_view_qualifier() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Qualifier 'TRK' is not supported");

    ViewAttributes.Type.fromQualifier(Qualifiers.PROJECT);
  }
}
