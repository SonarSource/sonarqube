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

import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;
import org.sonar.server.platform.db.migration.version.v63.TestDefaultOrganizationUuidProvider;

public class SetQualityProfileOrganizationUuidToDefaultTest {

  private static final String DEFAULT_ORG = "some uuid";
  private static final String PROFILE_KEY = "java-sonar-way-999999";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetQualityProfileOrganizationUuidToDefaultTest.class, "initial.sql");

  private DefaultOrganizationUuidProvider defaultOrganization = new TestDefaultOrganizationUuidProvider(DEFAULT_ORG);

  public SetQualityProfileOrganizationUuidToDefault underTest = new SetQualityProfileOrganizationUuidToDefault(db.database(), defaultOrganization);

  @Test
  public void should_change_profile_without_organization() throws SQLException {
    db.executeInsert("RULES_PROFILES", "NAME", "java", "kee", PROFILE_KEY);

    underTest.execute();

    Assertions.assertThat(db.selectFirst("SELECT ORGANIZATION_UUID FROM RULES_PROFILES WHERE KEE = '" + PROFILE_KEY + "'")).containsValue(DEFAULT_ORG);
  }

  @Test
  public void should_keep_existing_organization() throws SQLException {
    String otherOrg = "existing uuid";
    db.executeInsert("RULES_PROFILES", "NAME", "java", "kee", PROFILE_KEY, "organization_uuid", otherOrg);

    underTest.execute();

    Assertions.assertThat(db.selectFirst("SELECT ORGANIZATION_UUID FROM RULES_PROFILES WHERE KEE = '" + PROFILE_KEY + "'")).containsValue(otherOrg);
  }
}
