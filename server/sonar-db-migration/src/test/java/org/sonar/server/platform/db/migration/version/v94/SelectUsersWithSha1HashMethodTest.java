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
package org.sonar.server.platform.db.migration.version.v94;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class SelectUsersWithSha1HashMethodTest {

  private static final UuidFactory UUID_FACTORY = UuidFactoryFast.getInstance();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SelectUsersWithSha1HashMethodTest.class, "schema.sql");

  private final DataChange underTest = new SelectUsersWithSha1HashMethod(db.database());

  @Test
  public void migration_ifSomeUsersUseSha1_shouldLogThem() throws SQLException {
    String user1sha1 = insertUser("SHA1");
    String user2sha1 = insertUser("SHA1");
    insertUser(null);
    insertUser("PBKDF2");
    insertUser("BCRYPT");
    insertUser("");

    underTest.execute();

    assertThat(logTester.getLogs(LoggerLevel.WARN))
      .hasSize(1)
      .first()
      .extracting(LogAndArguments::getFormattedMsg)
      .asString()
      .startsWith("The following local accounts have their password hashed with an algorithm which is not longer supported. They will not be able to login anymore. "
        + "Please reset their password if the accounts need to be kept.")
      .contains(user1sha1, user2sha1);
  }

  @Test
  public void migration_ifAllUsersAreNotUsingSha1_shouldNotLogAnything() throws SQLException {
    insertUser(null);
    insertUser("PBKDF2");
    insertUser("BCRYPT");
    insertUser("");

    underTest.execute();

    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    underTest.execute();
    // re-entrant
    underTest.execute();
  }

  private String insertUser(@Nullable String hashMethod) {
    String login = hashMethod + randomAlphabetic(20);

    Map<String, Object> map = new HashMap<>();
    String uuid = UUID_FACTORY.create();
    map.put("UUID", uuid);
    map.put("LOGIN", login);
    map.put("HASH_METHOD", hashMethod);
    map.put("EXTERNAL_LOGIN", login);
    map.put("EXTERNAL_IDENTITY_PROVIDER", "sonarqube");
    map.put("EXTERNAL_ID", randomNumeric(5));
    map.put("IS_ROOT", false);
    map.put("ONBOARDED", false);
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("RESET_PASSWORD", false);
    db.executeInsert("users", map);
    return login;
  }
}
