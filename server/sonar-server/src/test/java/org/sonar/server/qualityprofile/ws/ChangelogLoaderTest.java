/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ChangelogLoaderTest {

  private static final String A_PROFILE_KEY = "P1";
  private static final long A_DATE = 1_500_000_000L;
  private static final RuleKey A_RULE_KEY = RuleKey.of("java", "S001");
  private static final String A_USER_LOGIN = "marcel";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = dbTester.getSession();

  private ChangelogLoader underTest = new ChangelogLoader(dbTester.getDbClient());

  @Test
  public void return_changes_in_reverse_chronological_order() {
    insertChange("C1", ActiveRuleChange.Type.ACTIVATED, A_DATE, A_USER_LOGIN, null);
    insertChange("C2", ActiveRuleChange.Type.DEACTIVATED, A_DATE + 10, "mazout", null);

    QProfileChangeQuery query = new QProfileChangeQuery(A_PROFILE_KEY);
    ChangelogLoader.Changelog changes = underTest.load(dbSession, query);

    assertThat(changes.getTotal()).isEqualTo(2);
    assertThat(changes.getChanges()).extracting(ChangelogLoader.Change::getKey).containsExactly("C2", "C1");
  }

  @Test
  public void return_change_with_only_required_fields() {
    insertChange("C1", ActiveRuleChange.Type.ACTIVATED, A_DATE, null, null);

    QProfileChangeQuery query = new QProfileChangeQuery(A_PROFILE_KEY);
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getKey()).isEqualTo("C1");
    assertThat(change.getCreatedAt()).isEqualTo(A_DATE);
    assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    // optional fields are null or empty
    assertThat(change.getInheritance()).isNull();
    assertThat(change.getRuleKey()).isNull();
    assertThat(change.getRuleName()).isNull();
    assertThat(change.getSeverity()).isNull();
    assertThat(change.getUserLogin()).isNull();
    assertThat(change.getUserName()).isNull();
    assertThat(change.getParams()).isEmpty();
  }

  @Test
  public void return_change_with_all_fields() {
    Map<String, String> data = ImmutableMap.of(
      "ruleKey", A_RULE_KEY.toString(),
      "severity", "MINOR",
      "inheritance", ActiveRule.Inheritance.INHERITED.name(),
      "param_foo", "foo_value",
      "param_bar", "bar_value");
    insertChange("C1", ActiveRuleChange.Type.ACTIVATED, A_DATE, A_USER_LOGIN, data);

    QProfileChangeQuery query = new QProfileChangeQuery(A_PROFILE_KEY);
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getKey()).isEqualTo("C1");
    assertThat(change.getCreatedAt()).isEqualTo(A_DATE);
    assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    assertThat(change.getInheritance()).isEqualTo(ActiveRule.Inheritance.INHERITED.name());
    assertThat(change.getRuleKey()).isEqualTo(A_RULE_KEY);
    assertThat(change.getRuleName()).isNull();
    assertThat(change.getSeverity()).isEqualTo("MINOR");
    assertThat(change.getUserLogin()).isEqualTo(A_USER_LOGIN);
    assertThat(change.getUserName()).isNull();
    assertThat(change.getParams()).containsOnly(entry("foo", "foo_value"), entry("bar", "bar_value"));
  }

  @Test
  public void return_name_of_rule() {
    Map<String, String> data = ImmutableMap.of("ruleKey", "java:S001");
    insertChange("C1", ActiveRuleChange.Type.ACTIVATED, A_DATE, A_USER_LOGIN, data);
    insertRule(A_RULE_KEY, "Potential NPE");

    QProfileChangeQuery query = new QProfileChangeQuery(A_PROFILE_KEY);
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getRuleKey()).isEqualTo(A_RULE_KEY);
    assertThat(change.getRuleName()).isEqualTo("Potential NPE");
  }

  @Test
  public void return_name_of_user() {
    insertChange("C1", ActiveRuleChange.Type.ACTIVATED, A_DATE, A_USER_LOGIN, null);
    insertUser(A_USER_LOGIN, "Marcel");

    QProfileChangeQuery query = new QProfileChangeQuery(A_PROFILE_KEY);
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getUserLogin()).isEqualTo(A_USER_LOGIN);
    assertThat(change.getUserName()).isEqualTo("Marcel");
  }

  @Test
  public void return_empty_changelog() {
    QProfileChangeQuery query = new QProfileChangeQuery("P1");

    ChangelogLoader.Changelog changelog = underTest.load(dbSession, query);

    assertThat(changelog.getTotal()).isEqualTo(0);
    assertThat(changelog.getChanges()).isEmpty();
  }

  private void insertChange(String key, ActiveRuleChange.Type type, long date,
    @Nullable String login, @Nullable Map<String, String> data) {
    QProfileChangeDto dto = QualityProfileTesting.newQProfileChangeDto()
      .setProfileKey(A_PROFILE_KEY)
      .setKey(key)
      .setCreatedAt(date)
      .setLogin(login)
      .setChangeType(type.name())
      .setData(data);
    QualityProfileTesting.insert(dbTester, dto);
  }

  private void insertRule(RuleKey key, String name) {
    RuleDefinitionDto dto = RuleTesting.newRule(key).setName(name);
    dbTester.rules().insert(dto);
    dbTester.getSession().commit();
  }

  private void insertUser(String login, String name) {
    UserDto dto = UserTesting.newUserDto()
      .setLogin(login)
      .setName(name);
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
  }
}
