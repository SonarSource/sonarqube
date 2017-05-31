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
package org.sonar.server.qualityprofile.index;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.qualityprofile.ActiveRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.server.qualityprofile.ActiveRule.Inheritance.INHERITED;

public class ActiveRuleResultSetIteratorTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private RuleDefinitionDto rule;
  private OrganizationDto org;
  private QProfileDto profile1;
  private QProfileDto profile2;

  @Before
  public void before() {
    rule = db.rules().insert();
    org = db.organizations().insert();
    profile1 = db.qualityProfiles().insert(org);
    profile2 = db.qualityProfiles().insert(org);
  }
  @Test
  public void iterate_over_one_active_rule() {
    ActiveRuleDto dto = db.qualityProfiles().activateRule(profile1, rule, ar -> ar.setSeverity(CRITICAL).setInheritance(null));

    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(db.getDbClient(), db.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(1);

    ActiveRuleKey key = ActiveRuleKey.of(profile1.getKee(), rule.getKey());
    ActiveRuleDoc activeRule = activeRulesByKey.get(key);
    assertThat(activeRule.organizationUuid()).isEqualTo(org.getUuid());
    assertThat(activeRule.key()).isEqualTo(key);
    assertThat(activeRule.severity()).isEqualTo(CRITICAL);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRule.updatedAt()).isEqualTo(dto.getUpdatedAt());
  }

  @Test
  public void iterate_over_multiple_active_rules() {
    ActiveRuleDto dto1 = db.qualityProfiles().activateRule(profile1, rule);
    ActiveRuleDto dto2 = db.qualityProfiles().activateRule(profile2, rule);

    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(db.getDbClient(), db.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(2);
    assertThat(activeRulesByKey.keySet()).containsExactlyInAnyOrder(dto1.getKey(), dto2.getKey());
  }

  @Test
  public void iterate_inherited_active_rule() {
    ActiveRuleDto dto = db.qualityProfiles().activateRule(profile1, rule, ar -> ar.setInheritance(INHERITED.name()));

    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(db.getDbClient(), db.getSession(), 0L);
    Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey = activeRulesByKey(it);
    it.close();

    assertThat(activeRulesByKey).hasSize(1);
    assertThat(activeRulesByKey.get(dto.getKey()).inheritance()).isEqualTo(INHERITED);
  }

  @Test
  public void select_after_date() {
    ActiveRuleDto dto1 = db.qualityProfiles().activateRule(profile1, rule, ar -> ar.setUpdatedAt(1_500L));
    ActiveRuleDto dto2 = db.qualityProfiles().activateRule(profile2, rule, ar -> ar.setUpdatedAt(1_600L));

    ActiveRuleResultSetIterator it = ActiveRuleResultSetIterator.create(db.getDbClient(), db.getSession(), 1_550L);
    assertThat(it.hasNext()).isTrue();
    ActiveRuleDoc doc = it.next();
    assertThat(doc.key()).isEqualTo(dto2.getKey());

    assertThat(it.hasNext()).isFalse();
    it.close();
  }

  private static Map<ActiveRuleKey, ActiveRuleDoc> activeRulesByKey(ActiveRuleResultSetIterator it) {
    return Maps.uniqueIndex(it, ActiveRuleDoc::key);
  }

}
