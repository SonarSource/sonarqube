/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.organization;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTemplatesTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultTemplates underTest = new DefaultTemplates();

  @Test
  public void setProject_throws_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("defaultTemplates.project can't be null");

    underTest.setProjectUuid(null);
  }

  @Test
  public void getProject_throws_NPE_if_project_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("defaultTemplates.project can't be null");

    underTest.getProjectUuid();
  }

  @Test
  public void setView_accepts_null() {
    underTest.setViewUuid(null);
  }

  @Test
  public void check_toString() {
    assertThat(underTest.toString()).isEqualTo("DefaultTemplates{projectUuid='null', viewUuid='null'}");
    underTest
        .setProjectUuid("a project")
        .setViewUuid("a view");
    assertThat(underTest.toString()).isEqualTo("DefaultTemplates{projectUuid='a project', viewUuid='a view'}");
  }
}
