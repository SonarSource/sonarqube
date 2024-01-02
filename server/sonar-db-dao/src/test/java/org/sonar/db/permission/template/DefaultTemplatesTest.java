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
package org.sonar.db.permission.template;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultTemplatesTest {

  private final DefaultTemplates underTest = new DefaultTemplates();

  @Test
  public void setProject_throws_NPE_if_argument_is_null() {
    assertThatThrownBy(() -> underTest.setProjectUuid(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("defaultTemplates.project can't be null");
  }

  @Test
  public void getProject_throws_NPE_if_project_is_null() {
    assertThatThrownBy(underTest::getProjectUuid)
      .isInstanceOf(NullPointerException.class)
      .hasMessage("defaultTemplates.project can't be null");
  }

  @Test
  public void setProjectU() {
    String uuid = "uuid-1";
    underTest.setProjectUuid(uuid);

    assertThat(underTest.getProjectUuid()).isEqualTo(uuid);
  }

  @Test
  public void setApplicationsUuid_accepts_null() {
    underTest.setApplicationsUuid(null);

    assertThat(underTest.getApplicationsUuid()).isNull();
  }

  @Test
  public void setPortfoliosUuid_accepts_null() {
    underTest.setPortfoliosUuid(null);

    assertThat(underTest.getPortfoliosUuid()).isNull();
  }

  @Test
  public void check_toString() {
    assertThat(underTest).hasToString("DefaultTemplates{projectUuid='null', portfoliosUuid='null', applicationsUuid='null'}");
    underTest
      .setProjectUuid("a project")
      .setApplicationsUuid("an application")
      .setPortfoliosUuid("a portfolio");
    assertThat(underTest).hasToString("DefaultTemplates{projectUuid='a project', portfoliosUuid='a portfolio', applicationsUuid='an application'}");
  }
}
