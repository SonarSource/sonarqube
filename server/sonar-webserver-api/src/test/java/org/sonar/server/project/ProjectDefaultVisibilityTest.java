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
package org.sonar.server.project;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProjectDefaultVisibilityTest {

  @Rule
  public final DbTester db = DbTester.create();

  private final ProjectDefaultVisibility underTest = new ProjectDefaultVisibility(db.getDbClient());

  @Test
  public void fail_if_project_visibility_property_not_exist() {
    DbSession dbSession = db.getSession();
    assertThatThrownBy(() -> underTest.get(dbSession))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Could not find default project visibility setting");
  }

  @Test
  public void set_default_project_visibility() {
    underTest.set(db.getSession(), Visibility.PUBLIC);
    assertThat(underTest.get(db.getSession())).isEqualTo(Visibility.PUBLIC);

    underTest.set(db.getSession(), Visibility.PRIVATE);
    assertThat(underTest.get(db.getSession())).isEqualTo(Visibility.PRIVATE);
  }

  @Test
  public void set_default_project_visibility_by_string() {
    underTest.set(db.getSession(), "private");
    assertThat(underTest.get(db.getSession())).isEqualTo(Visibility.PRIVATE);

    underTest.set(db.getSession(), "public");
    assertThat(underTest.get(db.getSession())).isEqualTo(Visibility.PUBLIC);
  }

}
