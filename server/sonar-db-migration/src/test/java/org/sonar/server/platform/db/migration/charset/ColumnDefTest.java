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
package org.sonar.server.platform.db.migration.charset;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ColumnDefTest {

  @Test
  public void isInSonarQubeTable_returns_false_if_sqlazure_system_table() {
    ColumnDef underTest = new ColumnDef("sys.sysusers", "login", "charset", "collation", "NVARCHAR", 100L, false);
    assertThat(underTest.isInSonarQubeTable()).isFalse();

    underTest = new ColumnDef("SYS.SYSUSERS", "login", "charset", "collation", "NVARCHAR", 100L, false);
    assertThat(underTest.isInSonarQubeTable()).isFalse();
  }

  @Test
  public void isInSonarQubeTable_returns_true_if_table_created_by_sonarqube() {
    ColumnDef underTest = new ColumnDef("project_measures", "text_value", "charset", "collation", "NVARCHAR", 100L, false);
    assertThat(underTest.isInSonarQubeTable()).isTrue();

    underTest = new ColumnDef("PROJECT_MEASURES", "text_value", "charset", "collation", "NVARCHAR", 100L, false);
    assertThat(underTest.isInSonarQubeTable()).isTrue();
  }

  @Test
  public void isInSonarQubeTable_returns_true_if_table_existed_in_previous_versions_of_sonarqube() {
    ColumnDef underTest = new ColumnDef("activities", "kee", "charset", "collation", "NVARCHAR", 100L, false);
    assertThat(underTest.isInSonarQubeTable()).isTrue();
  }
}
