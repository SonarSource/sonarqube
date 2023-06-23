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
package org.sonar.db.qualityprofile;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newRuleProfileDto;

public class QualityProfileDaoTest {

  private final System2 system = mock(System2.class);
  @Rule
  public DbTester db = DbTester.create(system);

  private final DbSession dbSession = db.getSession();
  private final QualityProfileDao underTest = db.getDbClient().qualityProfileDao();

  @Before
  public void before() {
    when(system.now()).thenReturn(null);
  }

  @After
  public void deleteData() {
    dbSession.rollback();
  }

  @Test
  public void test_insert() {
    QProfileDto dto = new QProfileDto()
      .setKee("theUuid")
      .setRulesProfileUuid("theRulesProfileUuid")
      .setName("theName")
      .setLanguage("theLang")
      .setLastUsed(1_000L)
      .setParentKee("theParentUuid")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(true);

    underTest.insert(dbSession, dto);

    QProfileDto reloaded = underTest.selectByUuid(dbSession, dto.getKee());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getKee()).isEqualTo(dto.getKee());
    assertThat(reloaded.getRulesProfileUuid()).isEqualTo(dto.getRulesProfileUuid());
    assertThat(reloaded.getLanguage()).isEqualTo(dto.getLanguage());
    assertThat(reloaded.getName()).isEqualTo(dto.getName());
    assertThat(reloaded.getLastUsed()).isEqualTo(dto.getLastUsed());
    assertThat(reloaded.getRulesUpdatedAt()).isEqualTo(dto.getRulesUpdatedAt());
    assertThat(reloaded.getParentKee()).isEqualTo(dto.getParentKee());
    assertThat(reloaded.isBuiltIn()).isEqualTo(dto.isBuiltIn());
  }

  @Test
  public void test_update() {
    QProfileDto initial = new QProfileDto()
      .setKee("theUuid")
      .setRulesProfileUuid("theRulesProfileUuid")
      .setName("theName")
      .setLanguage("theLang")
      .setLastUsed(1_000L)
      .setParentKee("theParentUuid")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(true);
    underTest.insert(dbSession, initial);

    QProfileDto update = new QProfileDto()
      .setKee(initial.getKee())
      .setRulesProfileUuid(initial.getRulesProfileUuid())
      .setName("theNewName")
      .setLanguage("theNewLang")
      .setLastUsed(11_000L)
      .setParentKee("theNewParentUuid")
      .setUserUpdatedAt(12_000L)
      .setRulesUpdatedAt("2017-06-01")
      .setIsBuiltIn(false);
    underTest.update(dbSession, update);

    QProfileDto reloaded = underTest.selectByUuid(dbSession, initial.getKee());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getKee()).isEqualTo(initial.getKee());

    // updated fields
    assertThat(reloaded.getLanguage()).isEqualTo(update.getLanguage());
    assertThat(reloaded.getName()).isEqualTo(update.getName());
    assertThat(reloaded.getLastUsed()).isEqualTo(update.getLastUsed());
    assertThat(reloaded.getRulesUpdatedAt()).isEqualTo(update.getRulesUpdatedAt());
    assertThat(reloaded.getParentKee()).isEqualTo(update.getParentKee());
    assertThat(reloaded.isBuiltIn()).isEqualTo(update.isBuiltIn());
  }

  @Test
  public void test_updateLastUsedDate_if_never_been_set_yet() {
    QProfileDto initial = QualityProfileTesting.newQualityProfileDto()
      .setLastUsed(null);
    underTest.insert(dbSession, initial);

    int count = underTest.updateLastUsedDate(dbSession, initial, 15_000L);

    assertThat(count).isOne();
    QProfileDto reloaded = underTest.selectByUuid(dbSession, initial.getKee());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getLastUsed()).isEqualTo(15_000L);
  }

  @Test
  public void test_updateLastUsedDate_if_more_recent() {
    QProfileDto initial = QualityProfileTesting.newQualityProfileDto()
      .setLastUsed(10_000L);
    underTest.insert(dbSession, initial);

    int count = underTest.updateLastUsedDate(dbSession, initial, 15_000L);

    assertThat(count).isOne();
    QProfileDto reloaded = underTest.selectByUuid(dbSession, initial.getKee());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getLastUsed()).isEqualTo(15_000L);
  }

  @Test
  public void updateLastUsedDate_does_not_touch_row_if_last_used_is_more_recent() {
    QProfileDto initial = QualityProfileTesting.newQualityProfileDto()
      .setLastUsed(10_000L);
    underTest.insert(dbSession, initial);

    int count = underTest.updateLastUsedDate(dbSession, initial, 8_000L);

    assertThat(count).isZero();
    QProfileDto reloaded = underTest.selectByUuid(dbSession, initial.getKee());
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getLastUsed()).isEqualTo(10_000L);
  }

  @Test
  public void selectRuleProfile() {
    RulesProfileDto rp = insertRulesProfile();

    assertThat(underTest.selectRuleProfile(dbSession, rp.getUuid())).extracting(RulesProfileDto::getName).isEqualTo(rp.getName());
    assertThat(underTest.selectRuleProfile(dbSession, "missing")).isNull();
  }

  @Test
  public void deleteRulesProfilesByUuids() {
    RulesProfileDto rp1 = insertRulesProfile();
    RulesProfileDto rp2 = insertRulesProfile();

    underTest.deleteRulesProfilesByUuids(dbSession, singletonList(rp1.getUuid()));

    List<Map<String, Object>> uuids = db.select(dbSession, "select uuid as \"uuid\" from rules_profiles");
    assertThat(uuids).hasSize(1);
    assertThat(uuids.get(0)).containsEntry("uuid", rp2.getUuid());
  }

  @Test
  public void deleteRulesProfilesByUuids_does_nothing_if_empty_input() {
    insertRulesProfile();

    underTest.deleteRulesProfilesByUuids(dbSession, emptyList());

    assertThat(db.countRowsOfTable(dbSession, "rules_profiles")).isOne();
  }

  @Test
  public void deleteRulesProfilesByUuids_does_nothing_if_specified_uuid_does_not_exist() {
    insertRulesProfile();

    underTest.deleteRulesProfilesByUuids(dbSession, singletonList("does_not_exist"));

    assertThat(db.countRowsOfTable(dbSession, "rules_profiles")).isOne();
  }

  private RulesProfileDto insertRulesProfile() {
    RulesProfileDto dto = new RulesProfileDto()
      .setName(randomAlphanumeric(10))
      .setLanguage(randomAlphanumeric(3))
      .setUuid(Uuids.createFast())
      .setIsBuiltIn(false);
    db.getDbClient().qualityProfileDao().insert(dbSession, dto);
    return dto;
  }

  @Test
  public void test_deleteProjectAssociationsByProfileUuids() {
    QProfileDto profile1 = db.qualityProfiles().insert();
    QProfileDto profile2 = db.qualityProfiles().insert();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    ProjectDto project3 = db.components().insertPrivateProjectDto();

    db.qualityProfiles().associateWithProject(project1, profile1);
    db.qualityProfiles().associateWithProject(project2, profile1);
    db.qualityProfiles().associateWithProject(project3, profile2);

    underTest.deleteProjectAssociationsByProfileUuids(dbSession, asList(profile1.getKee(), "does_not_exist"));

    List<Map<String, Object>> rows = db.select(dbSession, "select project_uuid as \"projectUuid\", profile_key as \"profileKey\" from project_qprofiles");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("projectUuid", project3.getUuid());
    assertThat(rows.get(0)).containsEntry("profileKey", profile2.getKee());
  }

  @Test
  public void deleteProjectAssociationsByProfileUuids_does_nothing_if_empty_uuids() {
    QProfileDto profile = db.qualityProfiles().insert();
    ProjectDto project = db.components().insertPrivateProjectDto();
    db.qualityProfiles().associateWithProject(project, profile);

    underTest.deleteProjectAssociationsByProfileUuids(dbSession, Collections.emptyList());

    assertThat(db.countRowsOfTable(dbSession, "project_qprofiles")).isOne();
  }

  @Test
  public void test_selectAll() {
    List<QProfileDto> sharedData = createSharedData();

    List<QProfileDto> reloadeds = underTest.selectOrderedByOrganizationUuid(dbSession, null);

    assertThat(reloadeds).hasSize(sharedData.size());

    IntStream.range(1, reloadeds.size())
      .forEach(
        i -> {
          QProfileDto reloaded = reloadeds.get(i - 1);
          QProfileDto original = sharedData.get(i - 1);

          assertThat(reloaded.getRulesProfileUuid()).isEqualTo(original.getRulesProfileUuid());
          assertThat(reloaded.getName()).isEqualTo(original.getName());
          assertThat(reloaded.getKee()).isEqualTo(original.getKee());
          assertThat(reloaded.getLanguage()).isEqualTo(original.getLanguage());
          assertThat(reloaded.getParentKee()).isEqualTo(original.getParentKee());
          assertThat(reloaded.getRulesUpdatedAt()).isEqualTo(original.getRulesUpdatedAt());
          assertThat(reloaded.getLastUsed()).isEqualTo(original.getLastUsed());
          assertThat(reloaded.getUserUpdatedAt()).isEqualTo(original.getUserUpdatedAt());
          assertThat(reloaded.isBuiltIn()).isEqualTo(original.isBuiltIn());
        });
  }

  @Test
  public void selectAll_is_sorted_by_profile_name() {
    QProfileDto dto1 = new QProfileDto()
      .setKee("js_first")
      .setRulesProfileUuid("rp-js_first")
      .setName("First")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, dto1);

    QProfileDto dto2 = new QProfileDto()
      .setKee("js_second")
      .setRulesProfileUuid("rp-js_second")
      .setName("Second")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, dto2);

    QProfileDto dto3 = new QProfileDto()
      .setKee("js_third")
      .setRulesProfileUuid("rp-js_third")
      .setName("Third")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, dto3);

    List<QProfileDto> dtos = underTest.selectOrderedByOrganizationUuid(dbSession, null);

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getName()).isEqualTo("First");
    assertThat(dtos.get(1).getName()).isEqualTo("Second");
    assertThat(dtos.get(2).getName()).isEqualTo("Third");
  }

  @Test
  public void selectDefaultProfile() {
    createSharedData();

    QProfileDto java = underTest.selectDefaultProfile(dbSession, null, "java");
    assertThat(java).isNotNull();
    assertThat(java.getKee()).isEqualTo("java_sonar_way");

    assertThat(underTest.selectDefaultProfile(dbSession, null, "js")).isNull();
  }

  @Test
  public void selectDefaultProfileUuid() {
    createSharedData();

    assertThat(underTest.selectDefaultProfileUuid(dbSession, "java")).isEqualTo("java_sonar_way");
    assertThat(underTest.selectDefaultProfileUuid(dbSession, "js")).isNull();
  }

  @Test
  public void selectDefaultProfiles() {
    createSharedData();

    List<QProfileDto> java = underTest.selectDefaultProfiles(dbSession, null, singletonList("java"));
    assertThat(java).extracting(QProfileDto::getKee).containsOnly("java_sonar_way");

    assertThat(underTest.selectDefaultProfiles(dbSession, null, singletonList("js"))).isEmpty();
    assertThat(underTest.selectDefaultProfiles(dbSession, null, of("java", "js"))).extracting(QProfileDto::getKee).containsOnly("java_sonar_way");
    assertThat(underTest.selectDefaultProfiles(dbSession, null, of("js", "java"))).extracting(QProfileDto::getKee).containsOnly("java_sonar_way");
    assertThat(underTest.selectDefaultProfiles(dbSession, null, Collections.emptyList())).isEmpty();
  }

  @Test
  public void selectByNameAndLanguage() {
    createSharedData();

    QProfileDto dto = underTest.selectByNameAndLanguage(dbSession, null, "Sonar Way", "java");
    assertThat(dto).isNotNull();
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguage(dbSession, null, "Sonar Way", "java")).isNotNull();
    assertThat(underTest.selectByNameAndLanguage(dbSession, null, "Sonar Way", "unknown")).isNull();
  }

  @Test
  public void selectByNameAndLanguages() {
    createSharedData();

    List<QProfileDto> dtos = underTest.selectByNameAndLanguages(dbSession, "Sonar Way", singletonList("java"));
    assertThat(dtos).hasSize(1);
    QProfileDto dto = dtos.iterator().next();
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguages(dbSession, "Sonar Way", singletonList("unknown"))).isEmpty();
    assertThat(underTest.selectByNameAndLanguages(dbSession, "Sonar Way", of("java", "unknown")))
      .extracting(QProfileDto::getKee).containsOnly(dto.getKee());
  }

  @Test
  public void selectByLanguage() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto();
    underTest.insert(dbSession, profile);

    List<QProfileDto> results = underTest.selectByLanguage(dbSession, profile.getLanguage());
    assertThat(results).hasSize(1);
    QProfileDto result = results.get(0);

    assertThat(result.getName()).isEqualTo(profile.getName());
    assertThat(result.getKee()).isEqualTo(profile.getKee());
    assertThat(result.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(result.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
  }

  @Test
  public void should_not_selectByLanguage_with_wrong_language() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto();
    underTest.insert(dbSession, profile);

    List<QProfileDto> results = underTest.selectByLanguage(dbSession, "another language");
    assertThat(results).isEmpty();
  }

  @Test
  public void selectChildren() {
    QProfileDto original1 = new QProfileDto()
      .setKee("java_child1")
      .setRulesProfileUuid("rp-java_child1")
      .setName("Child1")
      .setLanguage("java")
      .setLastUsed(1_000L)
      .setParentKee("java_parent")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, original1);

    QProfileDto original2 = new QProfileDto()
      .setKee("java_child2")
      .setRulesProfileUuid("rp-java_child2")
      .setName("Child2")
      .setLanguage("java")
      .setLastUsed(1_000L)
      .setParentKee("java_parent")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, original2);

    QProfileDto original3 = new QProfileDto()
      .setKee("java_parent")
      .setRulesProfileUuid("rp-java_parent")
      .setName("Parent")
      .setLanguage("java")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, original3);

    QProfileDto original4 = new QProfileDto()
      .setKee("js_child1")
      .setRulesProfileUuid("rp-js_child1")
      .setName("Child1")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setParentKee("js_parent")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, original4);

    QProfileDto original5 = new QProfileDto()
      .setKee("js_child2")
      .setRulesProfileUuid("rp-js_child2")
      .setName("Child2")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setParentKee("js_parent")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, original5);

    QProfileDto original6 = new QProfileDto()
      .setKee("js_parent")
      .setRulesProfileUuid("rp-js_parent")
      .setName("Parent")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, original6);

    List<QProfileDto> dtos = underTest.selectChildren(dbSession, singleton(original3));

    assertThat(dtos).hasSize(2);

    QProfileDto dto1 = dtos.get(0);
    assertThat(dto1.getName()).isEqualTo("Child1");
    assertThat(dto1.getLanguage()).isEqualTo("java");
    assertThat(dto1.getParentKee()).isEqualTo("java_parent");

    QProfileDto dto2 = dtos.get(1);
    assertThat(dto2.getName()).isEqualTo("Child2");
    assertThat(dto2.getLanguage()).isEqualTo("java");
    assertThat(dto2.getParentKee()).isEqualTo("java_parent");
  }

  @Test
  public void selectBuiltInRuleProfilesWithActiveRules() {
    // a quality profile without active rules but not builtin
    db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(false));

    // a built-in quality profile without active rules
    db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true));

    // a built-in quality profile with active rules
    QProfileDto builtInQPWithActiveRules = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true));
    RuleDto ruleDefinitionDto = db.rules().insert();
    db.qualityProfiles().activateRule(builtInQPWithActiveRules, ruleDefinitionDto);

    dbSession.commit();

    List<RulesProfileDto> rulesProfileDtos = underTest.selectBuiltInRuleProfilesWithActiveRules(dbSession);
    assertThat(rulesProfileDtos).extracting(RulesProfileDto::getName)
      .containsOnly(builtInQPWithActiveRules.getName());
  }

  @Test
  public void selectByRuleProfileUuid() {
    db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(false));
    db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true));
    QProfileDto qprofile1 = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true));

    dbSession.commit();

    assertThat(underTest.selectByRuleProfileUuid(dbSession, null, qprofile1.getRulesProfileUuid()))
      .extracting(QProfileDto::getName)
      .isEqualTo(qprofile1.getName());

    assertThat(underTest.selectByRuleProfileUuid(dbSession, null, "A"))
      .isNull();
  }

  @Test
  public void selectDefaultBuiltInProfilesWithoutActiveRules() {
    // a quality profile without active rules but not builtin
    db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(false).setLanguage("java"));

    // a built-in quality profile without active rules
    QProfileDto javaQPWithoutActiveRules = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true).setLanguage("java"));
    db.qualityProfiles().setAsDefault(javaQPWithoutActiveRules);

    // a built-in quality profile without active rules
    QProfileDto cppQPWithoutActiveRules = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true).setLanguage("cpp"));
    db.qualityProfiles().setAsDefault(cppQPWithoutActiveRules);

    // a built-in quality profile with active rules
    QProfileDto builtInQPWithActiveRules = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true).setLanguage("java"));
    RuleDto ruleDefinitionDto = db.rules().insert();
    db.qualityProfiles().activateRule(builtInQPWithActiveRules, ruleDefinitionDto);

    dbSession.commit();

    assertThat(underTest.selectDefaultBuiltInProfilesWithoutActiveRules(dbSession, Sets.newHashSet("java", "cpp")))
      .extracting(QProfileDto::getName)
      .containsOnly(javaQPWithoutActiveRules.getName(), cppQPWithoutActiveRules.getName());

    assertThat(underTest.selectDefaultBuiltInProfilesWithoutActiveRules(dbSession, Sets.newHashSet("java")))
      .extracting(QProfileDto::getName)
      .containsOnly(javaQPWithoutActiveRules.getName());

    assertThat(underTest.selectDefaultBuiltInProfilesWithoutActiveRules(dbSession, Sets.newHashSet("cobol")))
      .isEmpty();

    assertThat(underTest.selectDefaultBuiltInProfilesWithoutActiveRules(dbSession, Sets.newHashSet()))
      .isEmpty();
  }

  @Test
  public void selectDescendants_returns_empty_if_no_children() {
    QProfileDto base = db.qualityProfiles().insert();

    Collection<QProfileDto> descendants = underTest.selectDescendants(dbSession, singleton(base));

    assertThat(descendants).isEmpty();
  }

  @Test
  public void selectDescendants_returns_profile_does_not_exist() {
    Collection<QProfileDto> descendants = underTest.selectDescendants(dbSession, singleton(new QProfileDto().setKee("unknown")));

    assertThat(descendants).isEmpty();
  }

  @Test
  public void selectDescendants_returns_descendants_in_any_order() {
    QProfileDto base1 = db.qualityProfiles().insert();
    QProfileDto child1OfBase1 = db.qualityProfiles().insert(p -> p.setParentKee(base1.getKee()));
    QProfileDto child2OfBase1 = db.qualityProfiles().insert(p -> p.setParentKee(base1.getKee()));
    QProfileDto grandChildOfBase1 = db.qualityProfiles().insert(p -> p.setParentKee(child1OfBase1.getKee()));
    QProfileDto base2 = db.qualityProfiles().insert();
    QProfileDto childOfBase2 = db.qualityProfiles().insert(p -> p.setParentKee(base2.getKee()));
    QProfileDto grandChildOfBase2 = db.qualityProfiles().insert(p -> p.setParentKee(childOfBase2.getKee()));
    QProfileDto other = db.qualityProfiles().insert();

    // descendants of a single base profile
    verifyDescendants(singleton(base1), asList(child1OfBase1, child2OfBase1, grandChildOfBase1));
    verifyDescendants(singleton(child1OfBase1), singletonList(grandChildOfBase1));
    verifyDescendants(singleton(child2OfBase1), emptyList());
    verifyDescendants(singleton(grandChildOfBase1), emptyList());

    // descendants of a multiple base profiles
    verifyDescendants(asList(base1, base2), asList(child1OfBase1, child2OfBase1, grandChildOfBase1, childOfBase2, grandChildOfBase2));
    verifyDescendants(asList(base1, childOfBase2), asList(child1OfBase1, child2OfBase1, grandChildOfBase1, grandChildOfBase2));
    verifyDescendants(asList(child1OfBase1, grandChildOfBase2), singletonList(grandChildOfBase1));
    verifyDescendants(asList(other, base2), asList(childOfBase2, grandChildOfBase2));

  }

  private void verifyDescendants(Collection<QProfileDto> baseProfiles, Collection<QProfileDto> expectedDescendants) {
    Collection<QProfileDto> descendants = underTest.selectDescendants(dbSession, baseProfiles);
    String[] expectedUuids = expectedDescendants.stream().map(QProfileDto::getKee).toArray(String[]::new);
    assertThat(descendants)
      .extracting(QProfileDto::getKee)
      .containsExactlyInAnyOrder(expectedUuids);
  }

  @Test
  public void countProjectsByProfileKey() {
    QProfileDto profileWithoutProjects = db.qualityProfiles().insert();
    QProfileDto profileWithProjects = db.qualityProfiles().insert();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();

    db.qualityProfiles().associateWithProject(project1, profileWithProjects);
    db.qualityProfiles().associateWithProject(project2, profileWithProjects);

    assertThat(underTest.countProjectsByOrganizationAndProfiles(dbSession, null, asList(profileWithoutProjects, profileWithProjects))).containsOnly(
      MapEntry.entry(profileWithProjects.getKee(), 2L));
    assertThat(underTest.countProjectsByOrganizationAndProfiles(dbSession, null, Collections.emptyList())).isEmpty();
  }

  @Test
  public void test_selectAssociatedToProjectAndLanguage() {
    ProjectDto project1 = db.components().insertPublicProjectDto();
    ProjectDto project2 = db.components().insertPublicProjectDto();
    QProfileDto javaProfile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(p -> p.setLanguage("js"));
    db.qualityProfiles().associateWithProject(project1, javaProfile, jsProfile);

    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project1, "java")).extracting(QProfileDto::getKee).isEqualTo(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project1, "js")).extracting(QProfileDto::getKee).isEqualTo(jsProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project1, "cobol")).isNull();
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project2, "java")).isNull();
  }

  @Test
  public void test_selectAssociatedToProjectUuidAndLanguages() {
    ProjectDto project1 = db.components().insertPublicProjectDto();
    ProjectDto project2 = db.components().insertPublicProjectDto();
    QProfileDto javaProfile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(p -> p.setLanguage("js"));
    db.qualityProfiles().associateWithProject(project1, javaProfile, jsProfile);

    assertThat(underTest.selectAssociatedToProjectAndLanguages(dbSession, project1, singletonList("java")))
      .extracting(QProfileDto::getKee).containsOnly(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguages(dbSession, project1, singletonList("unknown")))
      .isEmpty();
    assertThat(underTest.selectAssociatedToProjectAndLanguages(dbSession, project1, of("java", "unknown")))
      .extracting(QProfileDto::getKee).containsExactly(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguages(dbSession, project1, of("java", "js")))
      .extracting(QProfileDto::getKee).containsExactlyInAnyOrder(javaProfile.getKee(), jsProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguages(dbSession, project2, singletonList("java")))
      .isEmpty();
    assertThat(underTest.selectAssociatedToProjectAndLanguages(dbSession, project2, Collections.emptyList()))
      .isEmpty();
  }

  @Test
  public void test_selectQProfileUuidsByProjectUuid() {
    ProjectDto project1 = db.components().insertPublicProjectDto();
    ProjectDto project2 = db.components().insertPublicProjectDto();
    ProjectDto project3 = db.components().insertPublicProjectDto();
    QProfileDto javaProfile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(p -> p.setLanguage("js"));
    QProfileDto cProfile = db.qualityProfiles().insert(p -> p.setLanguage("c"));
    db.qualityProfiles().associateWithProject(project1, javaProfile, cProfile);
    db.qualityProfiles().associateWithProject(project2, jsProfile);

    assertThat(underTest.selectQProfileUuidsByProjectUuid(dbSession, project1.getUuid()))
      .hasSize(2)
      .containsExactly(javaProfile.getKee(), cProfile.getKee());

    assertThat(underTest.selectQProfileUuidsByProjectUuid(dbSession, project2.getUuid()))
      .hasSize(1)
      .containsExactly(jsProfile.getKee());

    assertThat(underTest.selectQProfileUuidsByProjectUuid(dbSession, project3.getUuid()))
      .isEmpty();
  }

  @Test
  public void test_selectQProfilesByProjectUuid() {
    ProjectDto project1 = db.components().insertPublicProjectDto();
    ProjectDto project2 = db.components().insertPublicProjectDto();
    ProjectDto project3 = db.components().insertPublicProjectDto();
    QProfileDto javaProfile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(p -> p.setLanguage("js"));
    QProfileDto cProfile = db.qualityProfiles().insert(p -> p.setLanguage("c"));
    db.qualityProfiles().associateWithProject(project1, javaProfile, cProfile);
    db.qualityProfiles().associateWithProject(project2, jsProfile);

    assertThat(underTest.selectQProfilesByProjectUuid(dbSession, project1.getUuid()))
      .containsExactly(javaProfile, cProfile);

    assertThat(underTest.selectQProfilesByProjectUuid(dbSession, project2.getUuid()))
      .containsExactly(jsProfile);

    assertThat(underTest.selectQProfilesByProjectUuid(dbSession, project3.getUuid()))
      .isEmpty();
  }

  @Test
  public void test_updateProjectProfileAssociation() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto javaProfile1 = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(p -> p.setLanguage("js"));
    QProfileDto javaProfile2 = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    db.qualityProfiles().associateWithProject(project, javaProfile1, jsProfile);

    underTest.updateProjectProfileAssociation(dbSession, project, javaProfile2.getKee(), javaProfile1.getKee());

    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project, "java")).extracting(QProfileDto::getKee).isEqualTo(javaProfile2.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project, "js")).extracting(QProfileDto::getKee).isEqualTo(jsProfile.getKee());
  }

  @Test
  public void selectByKeys() {
    db.qualityProfiles().insert(newQualityProfileDto().setKee("qp-key-1"), newQualityProfileDto().setKee("qp-key-2"), newQualityProfileDto().setKee("qp-key-3"));

    assertThat(underTest.selectOrFailByUuid(dbSession, "qp-key-1")).isNotNull();
    assertThat(underTest.selectByUuid(dbSession, "qp-key-1")).isNotNull();
    assertThat(underTest.selectByUuid(dbSession, "qp-key-42")).isNull();
    assertThat(underTest.selectByUuids(dbSession, newArrayList("qp-key-1", "qp-key-3", "qp-key-42")))
      .hasSize(2)
      .extracting(QProfileDto::getKee).containsOnlyOnce("qp-key-1", "qp-key-3");
    assertThat(underTest.selectByUuids(dbSession, emptyList())).isEmpty();
  }

  @Test
  public void select_selected_projects() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setName("Project1 name"));
    ComponentDto project2 = db.components().insertPrivateProject(t -> t.setName("Project2 name"));
    ComponentDto project3 = db.components().insertPrivateProject(t -> t.setName("Project3 name"));
    db.components().insertPrivateProject(t -> t.setName("Project4 name"));
    db.components().insertProjectBranch(project1, t -> t.setKey("branch"));

    QProfileDto profile1 = newQualityProfileDto();
    db.qualityProfiles().insert(profile1);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project1), profile1);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project2), profile1);

    QProfileDto profile2 = newQualityProfileDto();
    db.qualityProfiles().insert(profile2);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project3), profile2);
    QProfileDto profile3 = newQualityProfileDto();

    assertThat(underTest.selectSelectedProjects(dbSession, null, profile1, null))
      .extracting("projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.uuid(), project1.getKey(), project1.name(), profile1.getKee()),
        tuple(project2.uuid(), project2.getKey(), project2.name(), profile1.getKee()));

    assertThat(underTest.selectSelectedProjects(dbSession, null, profile1, "ect1")).hasSize(1);
    assertThat(underTest.selectSelectedProjects(dbSession, null, profile3, null)).isEmpty();
  }

  @Test
  public void select_deselected_projects() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setName("Project1 name"));
    ComponentDto project2 = db.components().insertPrivateProject(t -> t.setName("Project2 name"));
    ComponentDto project3 = db.components().insertPrivateProject(t -> t.setName("Project3 name"));

    QProfileDto profile1 = newQualityProfileDto();
    db.qualityProfiles().insert(profile1);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project1), profile1);

    QProfileDto profile2 = newQualityProfileDto();
    db.qualityProfiles().insert(profile2);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project2), profile2);
    QProfileDto profile3 = newQualityProfileDto();

    assertThat(underTest.selectDeselectedProjects(dbSession, null, profile1, null))
      .extracting("projectUuid", "projectKey", "projectName", "profileKey")
      .containsExactly(
        tuple(project2.uuid(), project2.getKey(), project2.name(), null),
        tuple(project3.uuid(), project3.getKey(), project3.name(), null));

    assertThat(underTest.selectDeselectedProjects(dbSession, null, profile1, "ect2")).hasSize(1);
    assertThat(underTest.selectDeselectedProjects(dbSession, null, profile3, null)).hasSize(3);
  }

  @Test
  public void select_project_associations() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setName("Project1 name"));
    ComponentDto project2 = db.components().insertPrivateProject(t -> t.setName("Project2 name"));
    ComponentDto project3 = db.components().insertPrivateProject(t -> t.setName("Project3 name"));
    db.components().insertProjectBranch(project1, t -> t.setKey("branch"));

    QProfileDto profile1 = newQualityProfileDto();
    db.qualityProfiles().insert(profile1);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project1), profile1);

    QProfileDto profile2 = newQualityProfileDto();
    db.qualityProfiles().insert(profile2);
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(project2), profile2);
    QProfileDto profile3 = newQualityProfileDto();

    assertThat(underTest.selectProjectAssociations(dbSession, null, profile1, null))
      .extracting("projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.uuid(), project1.getKey(), project1.name(), profile1.getKee()),
        tuple(project2.uuid(), project2.getKey(), project2.name(), null),
        tuple(project3.uuid(), project3.getKey(), project3.name(), null));

    assertThat(underTest.selectProjectAssociations(dbSession, null, profile1, "ect2")).hasSize(1);
    assertThat(underTest.selectProjectAssociations(dbSession, null, profile3, null)).hasSize(3);
  }

  @Test
  public void selectUuidsOfCustomRulesProfiles_returns_the_custom_profiles_with_specified_name() {
    QProfileDto outdatedProfile1 = db.qualityProfiles().insert(p -> p.setIsBuiltIn(false).setLanguage("java").setName("foo"));
    db.qualityProfiles().insert(p -> p.setIsBuiltIn(false).setLanguage("cobol").setName("foo"));
    db.qualityProfiles().insert(p -> p.setIsBuiltIn(false).setLanguage("java").setName("bar"));

    Collection<String> keys = underTest.selectUuidsOfCustomRulesProfiles(dbSession, "java", "foo");
    assertThat(keys).containsOnly(outdatedProfile1.getRulesProfileUuid());
  }

  @Test
  public void selectOutdatedProfiles_returns_empty_list_if_no_match() {
    assertThat(underTest.selectUuidsOfCustomRulesProfiles(dbSession, "java", "foo")).isEmpty();
  }

  @Test
  public void renameAndCommit_updates_name_of_specified_profiles() {
    QProfileDto fooInOrg1 = db.qualityProfiles().insert(p -> p.setName("foo"));
    QProfileDto bar = db.qualityProfiles().insert(p -> p.setName("bar"));

    underTest.renameRulesProfilesAndCommit(dbSession, singletonList(fooInOrg1.getRulesProfileUuid()), "foo (copy)");

    assertThat(underTest.selectOrFailByUuid(dbSession, fooInOrg1.getKee()).getName()).isEqualTo("foo (copy)");
    assertThat(underTest.selectOrFailByUuid(dbSession, bar.getKee()).getName()).isEqualTo("bar");
  }

  @Test
  public void renameAndCommit_does_nothing_if_empty_keys() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setName("foo"));

    underTest.renameRulesProfilesAndCommit(dbSession, Collections.emptyList(), "foo (copy)");

    assertThat(underTest.selectOrFailByUuid(dbSession, profile.getKee()).getName()).isEqualTo("foo");
  }

  @Test
  public void selectQProfilesByRuleProfileUuid() {
    RulesProfileDto ruleProfile1 = newRuleProfileDto();
    OrgQProfileDto profile1InOrg1 = new OrgQProfileDto().setRulesProfileUuid(ruleProfile1.getUuid()).setUuid(Uuids.create());
    db.getDbClient().qualityProfileDao().insert(db.getSession(), ruleProfile1);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), profile1InOrg1);

    List<QProfileDto> result = db.getDbClient().qualityProfileDao().selectQProfilesByRuleProfile(db.getSession(), ruleProfile1);
    assertThat(result).extracting(QProfileDto::getKee).containsExactlyInAnyOrder(profile1InOrg1.getUuid());
  }

  @Test
  public void selectQProfilesByRuleProfileUuid_returns_empty_list_if_rule_profile_does_not_exist() {
    List<QProfileDto> result = db.getDbClient().qualityProfileDao().selectQProfilesByRuleProfile(db.getSession(), new RulesProfileDto().setUuid("unknown"));

    assertThat(result).isEmpty();
  }

  @Test
  public void selectProjectAssociations_shouldFindResult_whenQueryMatchingKey() {
    ComponentDto privateProject = db.components().insertPrivateProject(project -> project.setName("project name"), project -> project.setKey("project_key"));
    QProfileDto qProfileDto = db.qualityProfiles().insert();

    List<ProjectQprofileAssociationDto> results = underTest.selectProjectAssociations(dbSession, null, qProfileDto, "key");

    assertThat(results).extracting(ProjectQprofileAssociationDto::getProjectUuid).containsOnly(privateProject.uuid());
  }

  @Test
  public void selectSelectedProjects_shouldFindResult_whenQueryMatchingKey() {
    ComponentDto privateProject = db.components().insertPrivateProject(project -> project.setName("project name"), project -> project.setKey("project_key"));
    QProfileDto qProfileDto = db.qualityProfiles().insert();
    db.qualityProfiles().associateWithProject(db.components().getProjectDto(privateProject), qProfileDto);

    List<ProjectQprofileAssociationDto> results = underTest.selectSelectedProjects(dbSession, null, qProfileDto, "key");

    assertThat(results).extracting(ProjectQprofileAssociationDto::getProjectUuid).containsOnly(privateProject.uuid());
  }

  @Test
  public void selectDeselectedProjects_shouldFindResult_whenQueryMatchingKey() {
    ComponentDto privateProject = db.components().insertPrivateProject(project -> project.setName("project name"), project -> project.setKey("project_key"));
    QProfileDto qProfileDto = db.qualityProfiles().insert();

    List<ProjectQprofileAssociationDto> results = underTest.selectDeselectedProjects(dbSession, null, qProfileDto, "key");

    assertThat(results).extracting(ProjectQprofileAssociationDto::getProjectUuid).containsOnly(privateProject.uuid());
  }

  private List<QProfileDto> createSharedData() {
    QProfileDto dto1 = new QProfileDto()
      .setKee("java_sonar_way")
      .setRulesProfileUuid("rp-java_sonar_way")
      .setName("Sonar Way")
      .setLanguage("java")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(true);
    underTest.insert(dbSession, dto1);

    QProfileDto dto2 = new QProfileDto()
      .setKee("js_sonar_way")
      .setRulesProfileUuid("rp-js_sonar_way")
      .setName("Sonar Way")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(true);
    underTest.insert(dbSession, dto2);

    db.getDbClient().defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(dto1));

    return Arrays.asList(dto1, dto2);
  }
}
