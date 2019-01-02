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
package org.sonar.server.platform.db.migration.version.v66;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FixEmptyIdentityProviderInUsersTest {

  private final static long PAST = 100_000_000_000l;
  private final static long NOW = 500_000_000_000l;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(FixEmptyIdentityProviderInUsersTest.class, "users.sql");

  private FixEmptyIdentityProviderInUsers underTest = new FixEmptyIdentityProviderInUsers(db.database(), system2);

  @Test
  public void execute_has_no_effect_if_tables_are_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void migrate_user_without_external_identity_info() throws SQLException {
    insertUser("userWithoutExternalIdentityInfo", null, null);

    underTest.execute();

    assertUsers(tuple("userWithoutExternalIdentityInfo", "userWithoutExternalIdentityInfo", "sonarqube", NOW));
  }

  @Test
  public void migrate_user_with_partial_external_identity_info() throws SQLException {
    insertUser("userWithoutExternalIdentity", "user", null);
    insertUser("userWithoutExternalIdentityProvider", null, "github");

    underTest.execute();

    assertUsers(tuple("userWithoutExternalIdentity", "userWithoutExternalIdentity", "sonarqube", NOW),
      tuple("userWithoutExternalIdentityProvider", "userWithoutExternalIdentityProvider", "sonarqube", NOW));
  }

  @Test
  public void does_not_migrate_user_with_external_identity_info() throws SQLException {
    insertUser("userWithIdentityInfo", "user", "sonarqube");

    underTest.execute();

    assertUsers(tuple("userWithIdentityInfo", "user", "sonarqube", PAST));
  }

  private void insertUser(String login, @Nullable String externalIdentity, @Nullable String externalIdentityProvider) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.<String, Object>builder()
      .put("LOGIN", login)
      .put("IS_ROOT", true)
      .put("ONBOARDED", true)
      .put("CREATED_AT", PAST)
      .put("UPDATED_AT", PAST);
    if (externalIdentity != null) {
      map.put("EXTERNAL_IDENTITY", externalIdentity);
    }
    if (externalIdentityProvider != null) {
      map.put("EXTERNAL_IDENTITY_PROVIDER", externalIdentityProvider);
    }
    db.executeInsert("USERS", map.build());
  }

  private void assertUsers(Tuple... expectedTuples) {
    assertThat(db.select("SELECT LOGIN, EXTERNAL_IDENTITY, EXTERNAL_IDENTITY_PROVIDER, UPDATED_AT FROM USERS")
      .stream()
      .map(map -> new Tuple(map.get("LOGIN"), map.get("EXTERNAL_IDENTITY"), map.get("EXTERNAL_IDENTITY_PROVIDER"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }
}
