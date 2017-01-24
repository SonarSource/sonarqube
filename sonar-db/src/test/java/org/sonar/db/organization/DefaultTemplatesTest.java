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
    expectedException.expectMessage("project default template can't be null");

    underTest.setProject(null);
  }

  @Test
  public void setView_accepts_null() {
    underTest.setView(null);
  }

  @Test
  public void check_toString() {
    assertThat(underTest.toString()).isEqualTo("DefaultTemplates{project='null', view='null'}");
    underTest
        .setProject("a project")
        .setView("a view");
    assertThat(underTest.toString()).isEqualTo("DefaultTemplates{project='a project', view='a view'}");
  }
}
