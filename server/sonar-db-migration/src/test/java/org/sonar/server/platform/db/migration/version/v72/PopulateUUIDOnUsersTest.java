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
package org.sonar.server.platform.db.migration.version.v72;

import com.google.common.base.Strings;
import java.sql.SQLException;
import java.util.ArrayList;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.db.CoreDbTester.createForSchema;

public class PopulateUUIDOnUsersTest {

  private final static long PAST = 10_000_000_000L;
  private static final long NOW = 50_000_000_000L;
  private System2 system2 = new TestSystem2().setNow(NOW);

  private final static String NO_LOGIN = null;
  private final static String NO_UUID = null;

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public CoreDbTester db = createForSchema(PopulateUUIDOnUsersTest.class, "users.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private PopulateUUIDOnUsers underTest = new PopulateUUIDOnUsers(db.database(), system2, uuidFactory);

  @Test
  public void update_uuid_when_login_is_present() throws SQLException {
    String login1 = insertUser(NO_UUID, randomAlphanumeric(10));
    String login2 = insertUser(NO_UUID, randomAlphanumeric(10));
    String login3 = insertUser(NO_UUID, randomAlphanumeric(10));

    underTest.execute();

    assertUser(
      tuple(login1, login1, PAST, NOW),
      tuple(login2, login2, PAST, NOW),
      tuple(login3, login3, PAST, NOW)
    );
  }

  @Test
  public void check_max_length() throws Exception {
    String login = insertUser(NO_UUID, Strings.repeat("a", 255));

    underTest.execute();

    assertUser(tuple(login, login, PAST, NOW));
  }

  @Test
  public void generate_random_uuid_when_login_is_null() throws SQLException {
    insertUser(NO_UUID, NO_LOGIN);
    insertUser(NO_UUID, NO_LOGIN);
    insertUser(NO_UUID, NO_LOGIN);

    underTest.execute();

    assertThat(new ArrayList<>(db.select("SELECT distinct UUID FROM USERS"))).hasSize(3);
  }

  @Test
  public void do_nothing_when_uuid_is_already_present() throws SQLException {
    String login1 = insertUser(NO_UUID, randomAlphanumeric(10));
    String login2 = insertUser("existing-uuid", randomAlphanumeric(10));

    underTest.execute();

    assertUser(
      tuple(login1, login1, PAST, NOW),
      tuple("existing-uuid", login2, PAST, PAST)
    );
  }

  @Test
  public void is_reentrant() throws SQLException {
    String login1 = insertUser(NO_UUID, randomAlphanumeric(10));
    String login2 = insertUser("existing-uuid", randomAlphanumeric(10));

    underTest.execute();
    underTest.execute();

    assertUser(
      tuple(login1, login1, PAST, NOW),
      tuple("existing-uuid", login2, PAST, PAST)
    );
  }

  private String insertUser(String uuid, String login) {
    db.executeInsert("USERS", "UUID", uuid, "LOGIN", login, "IS_ROOT", false, "ONBOARDED", false, "CREATED_AT", PAST, "UPDATED_AT", PAST);
    return login;
  }

  private void assertUser(Tuple... expectedTuples) {
    assertThat(db.select("SELECT LOGIN, UUID, CREATED_AT, UPDATED_AT FROM USERS")
      .stream()
      .map(map -> new Tuple(map.get("UUID"), map.get("LOGIN"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }
}
