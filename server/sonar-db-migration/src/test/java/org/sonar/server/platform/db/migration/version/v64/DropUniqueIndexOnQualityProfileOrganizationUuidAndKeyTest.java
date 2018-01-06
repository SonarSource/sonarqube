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
package org.sonar.server.platform.db.migration.version.v64;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import java.sql.SQLException;

public class DropUniqueIndexOnQualityProfileOrganizationUuidAndKeyTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropUniqueIndexOnQualityProfileOrganizationUuidAndKeyTest.class, "initial.sql");

  public DropUniqueIndexOnQualityProfileOrganizationUuidAndKey underTest = new DropUniqueIndexOnQualityProfileOrganizationUuidAndKey(db.database());

  @Test
  public void test() throws SQLException {
    underTest.execute();
    db.assertIndexDoesNotExist("rules_profiles", "uniq_qprof_org_and_key");
  }
}
