/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.server.platform.db.migration.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.version.v101.MigrateScmAccountsFromUsersToScmAccounts.ScmAccountRow;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.sonar.server.platform.db.migration.version.v101.MigrateScmAccountsFromUsersToScmAccounts.SCM_ACCOUNTS_SEPARATOR_CHAR;

public class MigrateScmAccountsFromUsersToScmAccountsTest {

  private static final UuidFactory UUID_FACTORY = UuidFactoryFast.getInstance();
  private static final String SCM_ACCOUNT1 = "scmaccount";
  private static final String SCM_ACCOUNT2 = "scmaccount2";
  private static final String SCM_ACCOUNT_CAMELCASE = "scmAccount3";

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateScmAccountsFromUsersToScmAccounts.class);
  private final DataChange migrateScmAccountsFromUsersToScmAccounts = new MigrateScmAccountsFromUsersToScmAccounts(db.database());

  @Test
  public void execute_whenUserHasNullScmAccounts_doNotInsertInScmAccounts() throws SQLException {
    insertUserAndGetUuid(null);

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).isEmpty();
  }

  @Test
  public void execute_whenUserHasEmptyScmAccounts_doNotInsertInScmAccounts() throws SQLException {
    insertUserAndGetUuid("");

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).isEmpty();
  }

  @Test
  public void execute_whenUserHasEmptyScmAccountsWithOneSeparator_doNotInsertInScmAccounts() throws SQLException {
    insertUserAndGetUuid(String.valueOf(SCM_ACCOUNTS_SEPARATOR_CHAR));

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).isEmpty();
  }

  @Test
  public void execute_whenUserHasEmptyScmAccountsWithTwoSeparators_doNotInsertInScmAccounts() throws SQLException {
    insertUserAndGetUuid(SCM_ACCOUNTS_SEPARATOR_CHAR + String.valueOf(SCM_ACCOUNTS_SEPARATOR_CHAR));

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).isEmpty();
  }

  @Test
  public void execute_whenUserHasOneScmAccountWithoutSeparator_insertsInScmAccounts() throws SQLException {
    String userUuid = insertUserAndGetUuid(SCM_ACCOUNT1);

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).containsExactly(new ScmAccountRow(userUuid, SCM_ACCOUNT1));
  }

  @Test
  public void execute_whenUserHasOneScmAccountWithSeparators_insertsInScmAccounts() throws SQLException {
    String userUuid = insertUserAndGetUuid(format("%s%s%s", SCM_ACCOUNTS_SEPARATOR_CHAR, SCM_ACCOUNT1, SCM_ACCOUNTS_SEPARATOR_CHAR));

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).containsExactly(new ScmAccountRow(userUuid, SCM_ACCOUNT1));
  }

  @Test
  public void execute_whenUserHasOneScmAccountWithMixedCase_insertsInScmAccountsInLowerCase() throws SQLException {
    String userUuid = insertUserAndGetUuid(format("%s%s%s", SCM_ACCOUNTS_SEPARATOR_CHAR, SCM_ACCOUNT_CAMELCASE, SCM_ACCOUNTS_SEPARATOR_CHAR));

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).containsExactly(new ScmAccountRow(userUuid, SCM_ACCOUNT_CAMELCASE.toLowerCase(Locale.ENGLISH)));
  }

  @Test
  public void execute_whenUserHasTwoScmAccount_insertsInScmAccounts() throws SQLException {
    String userUuid = insertUserAndGetUuid(format("%s%s%s%s%s",
      SCM_ACCOUNTS_SEPARATOR_CHAR, SCM_ACCOUNT1, SCM_ACCOUNTS_SEPARATOR_CHAR, SCM_ACCOUNT2, SCM_ACCOUNTS_SEPARATOR_CHAR));

    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).containsExactlyInAnyOrder(
      new ScmAccountRow(userUuid, SCM_ACCOUNT1),
      new ScmAccountRow(userUuid, SCM_ACCOUNT2)
    );
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String userUuid = insertUserAndGetUuid(SCM_ACCOUNT1);

    migrateScmAccountsFromUsersToScmAccounts.execute();
    migrateScmAccountsFromUsersToScmAccounts.execute();

    Set<ScmAccountRow> scmAccounts = findAllScmAccounts();
    assertThat(scmAccounts).containsExactly(new ScmAccountRow(userUuid, SCM_ACCOUNT1));
  }

  @Test
  public void migration_should_be_reentrant_if_scm_account_column_dropped() {
    db.executeDdl("alter table users drop column scm_accounts");

    assertThatNoException().isThrownBy(migrateScmAccountsFromUsersToScmAccounts::execute);
  }


  private Set<ScmAccountRow> findAllScmAccounts() {
    Set<ScmAccountRow> scmAccounts = db.select("select user_uuid USERUUID, scm_account SCMACCOUNT from scm_accounts")
      .stream()
      .map(row -> new ScmAccountRow((String) row.get("USERUUID"), (String) row.get("SCMACCOUNT")))
      .collect(toSet());
    return scmAccounts;
  }

  private String insertUserAndGetUuid(@Nullable String scmAccounts) {

    Map<String, Object> map = new HashMap<>();
    String uuid = UUID_FACTORY.create();
    String login = "login_" + uuid;
    map.put("UUID", uuid);
    map.put("LOGIN", login);
    map.put("HASH_METHOD", "tagada");
    map.put("EXTERNAL_LOGIN", login);
    map.put("EXTERNAL_IDENTITY_PROVIDER", "sonarqube");
    map.put("EXTERNAL_ID", login);
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("RESET_PASSWORD", false);
    map.put("USER_LOCAL", true);
    map.put("SCM_ACCOUNTS", scmAccounts);
    db.executeInsert("users", map);
    return uuid;
  }

}
