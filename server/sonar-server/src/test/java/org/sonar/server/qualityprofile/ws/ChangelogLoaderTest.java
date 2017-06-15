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
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ChangelogLoaderTest {

  private System2 system2 = new AlwaysIncreasingSystem2();
  @Rule
  public DbTester db = DbTester.create(system2);
  private DbSession dbSession = db.getSession();

  private ChangelogLoader underTest = new ChangelogLoader(db.getDbClient());

  @Test
  public void return_changes_in_reverse_chronological_order() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    QProfileChangeDto change1 = insertChange(profile, ActiveRuleChange.Type.ACTIVATED, null, null);
    QProfileChangeDto change2 = insertChange(profile, ActiveRuleChange.Type.DEACTIVATED, "mazout", null);

    QProfileChangeQuery query = new QProfileChangeQuery(profile.getKee());
    ChangelogLoader.Changelog changes = underTest.load(dbSession, query);

    assertThat(changes.getChanges())
      .extracting(ChangelogLoader.Change::getKey)
      .containsExactly(change2.getUuid(), change1.getUuid());
  }

  @Test
  public void return_change_with_only_required_fields() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    QProfileChangeDto inserted = insertChange(profile, ActiveRuleChange.Type.ACTIVATED, null, null);

    QProfileChangeQuery query = new QProfileChangeQuery(profile.getKee());
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getKey()).isEqualTo(inserted.getUuid());
    assertThat(change.getCreatedAt()).isEqualTo(inserted.getCreatedAt());
    assertThat(change.getType()).isEqualTo(inserted.getChangeType());
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
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    Map<String, Object> data = ImmutableMap.of(
      "ruleKey", "java:S001",
      "severity", "MINOR",
      "inheritance", ActiveRule.Inheritance.INHERITED.name(),
      "param_foo", "foo_value",
      "param_bar", "bar_value");
    QProfileChangeDto inserted = insertChange(profile, ActiveRuleChange.Type.ACTIVATED, "theLogin", data);

    QProfileChangeQuery query = new QProfileChangeQuery(profile.getKee());
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getKey()).isEqualTo(inserted.getUuid());
    assertThat(change.getCreatedAt()).isEqualTo(inserted.getCreatedAt());
    assertThat(change.getType()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    assertThat(change.getInheritance()).isEqualTo(ActiveRule.Inheritance.INHERITED.name());
    assertThat(change.getRuleKey().toString()).isEqualTo("java:S001");
    assertThat(change.getRuleName()).isNull();
    assertThat(change.getSeverity()).isEqualTo("MINOR");
    assertThat(change.getUserLogin()).isEqualTo("theLogin");
    assertThat(change.getUserName()).isNull();
    assertThat(change.getParams()).containsOnly(entry("foo", "foo_value"), entry("bar", "bar_value"));
  }

  @Test
  public void return_name_of_rule() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    RuleDefinitionDto rule = db.rules().insert();
    Map<String, Object> data = ImmutableMap.of("ruleKey", rule.getKey().toString());
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, null, data);

    QProfileChangeQuery query = new QProfileChangeQuery(profile.getKee());
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(change.getRuleName()).isEqualTo(rule.getName());
  }

  @Test
  public void return_name_of_user() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    UserDto user = db.users().insertUser();
    insertChange(profile, ActiveRuleChange.Type.ACTIVATED, user.getLogin(), null);

    QProfileChangeQuery query = new QProfileChangeQuery(profile.getKee());
    ChangelogLoader.Change change = underTest.load(dbSession, query).getChanges().get(0);

    assertThat(change.getUserLogin()).isEqualTo(user.getLogin());
    assertThat(change.getUserName()).isEqualTo(user.getName());
  }

  @Test
  public void return_empty_changelog() {
    QProfileChangeQuery query = new QProfileChangeQuery("P1");

    ChangelogLoader.Changelog changelog = underTest.load(dbSession, query);

    assertThat(changelog.getTotal()).isEqualTo(0);
    assertThat(changelog.getChanges()).isEmpty();
  }

  private QProfileChangeDto insertChange(QProfileDto profile, ActiveRuleChange.Type type, @Nullable String login, @Nullable Map<String, Object> data) {
    QProfileChangeDto dto = new QProfileChangeDto()
      .setRulesProfileUuid(profile.getRulesProfileUuid())
      .setLogin(login)
      .setChangeType(type.name())
      .setData(data);
    db.getDbClient().qProfileChangeDao().insert(dbSession, dto);
    return dto;
  }

}
