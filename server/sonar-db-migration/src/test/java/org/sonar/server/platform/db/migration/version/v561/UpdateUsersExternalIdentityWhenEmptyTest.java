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
package org.sonar.server.platform.db.migration.version.v561;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateUsersExternalIdentityWhenEmptyTest {

  private static final long PAST = 1_000_000_000_000L;
  private static final long NOW = 1_500_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpdateUsersExternalIdentityWhenEmptyTest.class, "schema.sql");

  private System2 system = mock(System2.class);

  private UpdateUsersExternalIdentityWhenEmpty underTest = new UpdateUsersExternalIdentityWhenEmpty(db.database(), system);

  @Before
  public void setUp() throws Exception {
    when(system.now()).thenReturn(NOW);
  }

  @Test
  public void migrate_users() throws Exception {
    insertUser("user-without-eternal-identity", null, null, PAST);
    insertUser("user-with-only-eternal-identity-provider", "github", null, PAST);
    insertUser("user-with-only-eternal-identity", null, "login1", PAST);
    insertUser("user-with-both-eternal-identity", "github", "login2", PAST);

    underTest.execute();

    checkUserIsUpdated("user-without-eternal-identity");
    checkUserIsUpdated("user-with-only-eternal-identity-provider");
    checkUserIsUpdated("user-with-only-eternal-identity");

    checkUserIsNotUpdated("user-with-both-eternal-identity");
  }

  @Test
  public void doest_not_fail_when_no_user() throws Exception {
    underTest.execute();
  }

  private void insertUser(String login, @Nullable String externalIdentity, @Nullable String externalIdentityProvider, long updatedAt) {
    Map<String, Object> params = new HashMap<>();
    params.put("LOGIN", login);
    params.put("CREATED_AT", Long.toString(PAST));
    params.put("UPDATED_AT", Long.toString(updatedAt));
    if (externalIdentity != null) {
      params.put("EXTERNAL_IDENTITY", externalIdentity);
    }
    if (externalIdentityProvider != null) {
      params.put("EXTERNAL_IDENTITY_PROVIDER", externalIdentityProvider);
    }

    db.executeInsert("users", params);
  }

  private void checkUserIsUpdated(String login) {
    Map<String, Object> row = db.selectFirst("select EXTERNAL_IDENTITY, EXTERNAL_IDENTITY_PROVIDER, UPDATED_AT from users where LOGIN='" + login + "'");
    assertThat((String) row.get("EXTERNAL_IDENTITY_PROVIDER")).isEqualTo("sonarqube");
    assertThat((String) row.get("EXTERNAL_IDENTITY")).isEqualTo(login);
    assertThat(row.get("UPDATED_AT")).isEqualTo(NOW);
  }

  private void checkUserIsNotUpdated(String login) {
    Map<String, Object> row = db.selectFirst("select EXTERNAL_IDENTITY, EXTERNAL_IDENTITY_PROVIDER, UPDATED_AT from users where LOGIN='" + login + "'");
    assertThat((String) row.get("EXTERNAL_IDENTITY_PROVIDER")).isNotEmpty();
    assertThat((String) row.get("EXTERNAL_IDENTITY")).isNotEmpty();
    assertThat(row.get("UPDATED_AT")).isEqualTo(PAST);
  }

}
