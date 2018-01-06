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
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class SetNewProjectPrivateToFalseTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetNewProjectPrivateToFalseTest.class, "initial.sql");

  public SetNewProjectPrivateToFalse underTest = new SetNewProjectPrivateToFalse(db.database());

  @Test
  public void should_set_field() throws SQLException {
    db.executeInsert("ORGANIZATIONS",
      "UUID", RandomStringUtils.randomAlphabetic(10),
      "KEE", RandomStringUtils.randomAlphabetic(10),
      "NAME", RandomStringUtils.randomAlphabetic(10),
      "GUARDED", false,
      "NEW_PROJECT_PRIVATE", true,
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 1_000L);

    Assertions.assertThat(db.selectFirst("SELECT NEW_PROJECT_PRIVATE FROM ORGANIZATIONS")).containsValue(true);

    underTest.execute();

    Assertions.assertThat(db.selectFirst("SELECT NEW_PROJECT_PRIVATE FROM ORGANIZATIONS")).containsValue(false);
  }
}
