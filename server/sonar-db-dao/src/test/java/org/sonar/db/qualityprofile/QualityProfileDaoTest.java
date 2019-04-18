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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDefinitionDto;

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

public class QualityProfileDaoTest {

  private System2 system = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system);

  private DbSession dbSession = db.getSession();
  private QualityProfileDao underTest = db.getDbClient().qualityProfileDao();
  private OrganizationDto organization;

  @Before
  public void before() {
    when(system.now()).thenReturn(UtcDateUtils.parseDateTime("2014-01-20T12:00:00+0000").getTime());
    organization = db.organizations().insertForUuid("QualityProfileDaoTest-ORG");
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
      .setOrganizationUuid(organization.getUuid())
      .setName("theName")
      .setLanguage("theLang")
      .setLastUsed(1_000L)
      .setParentKee("theParentUuid")
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(true);

    underTest.insert(dbSession, dto);

    QProfileDto reloaded = underTest.selectByUuid(dbSession, dto.getKee());
    assertThat(reloaded.getKee()).isEqualTo(dto.getKee());
    assertThat(reloaded.getRulesProfileUuid()).isEqualTo(dto.getRulesProfileUuid());
    assertThat(reloaded.getId()).isNotNull().isNotZero();
    assertThat(reloaded.getLanguage()).isEqualTo(dto.getLanguage());
    assertThat(reloaded.getName()).isEqualTo(dto.getName());
    assertThat(reloaded.getLastUsed()).isEqualTo(dto.getLastUsed());
    assertThat(reloaded.getRulesUpdatedAt()).isEqualTo(dto.getRulesUpdatedAt());
    assertThat(reloaded.getParentKee()).isEqualTo(dto.getParentKee());
    assertThat(reloaded.getOrganizationUuid()).isEqualTo(dto.getOrganizationUuid());
    assertThat(reloaded.isBuiltIn()).isEqualTo(dto.isBuiltIn());
  }

  @Test
  public void test_update() {
    QProfileDto initial = new QProfileDto()
      .setKee("theUuid")
      .setRulesProfileUuid("theRulesProfileUuid")
      .setOrganizationUuid(organization.getUuid())
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
      .setIsBuiltIn(false)

      // field that cannot be changed
      .setOrganizationUuid("theNewOrg");
    underTest.update(dbSession, update);

    QProfileDto reloaded = underTest.selectByUuid(dbSession, initial.getKee());
    assertThat(reloaded.getKee()).isEqualTo(initial.getKee());
    assertThat(reloaded.getOrganizationUuid()).isEqualTo(initial.getOrganizationUuid());

    // updated fields
    assertThat(reloaded.getLanguage()).isEqualTo(update.getLanguage());
    assertThat(reloaded.getName()).isEqualTo(update.getName());
    assertThat(reloaded.getLastUsed()).isEqualTo(update.getLastUsed());
    assertThat(reloaded.getRulesUpdatedAt()).isEqualTo(update.getRulesUpdatedAt());
    assertThat(reloaded.getParentKee()).isEqualTo(update.getParentKee());
    assertThat(reloaded.isBuiltIn()).isEqualTo(update.isBuiltIn());
  }

  @Test
  public void test_updateLastUsedDate() {
    QProfileDto initial = QualityProfileTesting.newQualityProfileDto()
      .setLastUsed(10_000L);
    underTest.insert(dbSession, initial);

    int count = underTest.updateLastUsedDate(dbSession, initial, 15_000L);

    assertThat(count).isEqualTo(1);
    QProfileDto reloaded = underTest.selectByUuid(dbSession, initial.getKee());
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
    assertThat(reloaded.getLastUsed()).isEqualTo(10_000L);
  }

  @Test
  public void selectRuleProfile() {
    RulesProfileDto rp = insertRulesProfile();

    assertThat(underTest.selectRuleProfile(dbSession, rp.getKee()).getId()).isEqualTo(rp.getId());
    assertThat(underTest.selectRuleProfile(dbSession, "missing")).isNull();
  }

  @Test
  public void deleteRulesProfilesByUuids() {
    RulesProfileDto rp1 = insertRulesProfile();
    RulesProfileDto rp2 = insertRulesProfile();

    underTest.deleteRulesProfilesByUuids(dbSession, asList(rp1.getKee()));

    List<Map<String, Object>> uuids = db.select(dbSession, "select kee as \"uuid\" from rules_profiles");
    assertThat(uuids).hasSize(1);
    assertThat(uuids.get(0).get("uuid")).isEqualTo(rp2.getKee());
  }

  @Test
  public void deleteRulesProfilesByUuids_does_nothing_if_empty_input() {
    insertRulesProfile();

    underTest.deleteRulesProfilesByUuids(dbSession, emptyList());

    assertThat(db.countRowsOfTable(dbSession, "rules_profiles")).isEqualTo(1);
  }

  @Test
  public void deleteRulesProfilesByUuids_does_nothing_if_specified_uuid_does_not_exist() {
    insertRulesProfile();

    underTest.deleteRulesProfilesByUuids(dbSession, asList("does_not_exist"));

    assertThat(db.countRowsOfTable(dbSession, "rules_profiles")).isEqualTo(1);
  }

  private RulesProfileDto insertRulesProfile() {
    RulesProfileDto dto = new RulesProfileDto()
      .setName(randomAlphanumeric(10))
      .setLanguage(randomAlphanumeric(3))
      .setKee(Uuids.createFast())
      .setIsBuiltIn(false);
    db.getDbClient().qualityProfileDao().insert(dbSession, dto);
    return dto;
  }

  @Test
  public void test_deleteProjectAssociationsByProfileUuids() {
    QProfileDto profile1 = db.qualityProfiles().insert(organization);
    QProfileDto profile2 = db.qualityProfiles().insert(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    ComponentDto project3 = db.components().insertPrivateProject(organization);
    db.qualityProfiles().associateWithProject(project1, profile1);
    db.qualityProfiles().associateWithProject(project2, profile1);
    db.qualityProfiles().associateWithProject(project3, profile2);

    underTest.deleteProjectAssociationsByProfileUuids(dbSession, asList(profile1.getKee(), "does_not_exist"));

    List<Map<String, Object>> rows = db.select(dbSession, "select project_uuid as \"projectUuid\", profile_key as \"profileKey\" from project_qprofiles");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("projectUuid")).isEqualTo(project3.uuid());
    assertThat(rows.get(0).get("profileKey")).isEqualTo(profile2.getKee());
  }

  @Test
  public void deleteProjectAssociationsByProfileUuids_does_nothing_if_empty_uuids() {
    QProfileDto profile = db.qualityProfiles().insert(organization);
    ComponentDto project = db.components().insertPrivateProject();
    db.qualityProfiles().associateWithProject(project, profile);

    underTest.deleteProjectAssociationsByProfileUuids(dbSession, Collections.emptyList());

    assertThat(db.countRowsOfTable(dbSession, "project_qprofiles")).isEqualTo(1);
  }

  @Test
  public void test_selectAll() {
    List<QProfileDto> sharedData = createSharedData();

    List<QProfileDto> reloadeds = underTest.selectOrderedByOrganizationUuid(dbSession, organization);

    assertThat(reloadeds).hasSize(sharedData.size());

    IntStream.range(1, reloadeds.size())
      .forEach(
        i -> {
          QProfileDto reloaded = reloadeds.get(i - 1);
          QProfileDto original = sharedData.get(i - 1);

          assertThat(reloaded.getId()).isEqualTo(original.getId());
          assertThat(reloaded.getName()).isEqualTo(original.getName());
          assertThat(reloaded.getKee()).isEqualTo(original.getKee());
          assertThat(reloaded.getOrganizationUuid()).isEqualTo(original.getOrganizationUuid());
          assertThat(reloaded.getLanguage()).isEqualTo(original.getLanguage());
          assertThat(reloaded.getParentKee()).isEqualTo(original.getParentKee());
          assertThat(reloaded.getRulesUpdatedAt()).isEqualTo(original.getRulesUpdatedAt());
          assertThat(reloaded.getLastUsed()).isEqualTo(original.getLastUsed());
          assertThat(reloaded.getUserUpdatedAt()).isEqualTo(original.getUserUpdatedAt());
          assertThat(reloaded.isBuiltIn()).isEqualTo(original.isBuiltIn());
        });
  }

  @Test
  public void selectOrderedByOrganizationUuid_is_sorted_by_profile_name() {
    QProfileDto dto1 = new QProfileDto()
      .setKee("js_first")
      .setRulesProfileUuid("rp-js_first")
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
      .setName("Third")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(false);
    underTest.insert(dbSession, dto3);

    List<QProfileDto> dtos = underTest.selectOrderedByOrganizationUuid(dbSession, organization);

    assertThat(dtos).hasSize(3);
    assertThat(dtos.get(0).getName()).isEqualTo("First");
    assertThat(dtos.get(1).getName()).isEqualTo("Second");
    assertThat(dtos.get(2).getName()).isEqualTo("Third");
  }

  @Test
  public void selectDefaultProfile() {
    List<QProfileDto> sharedData = createSharedData();

    QProfileDto java = underTest.selectDefaultProfile(dbSession, organization, "java");
    assertThat(java).isNotNull();
    assertThat(java.getKee()).isEqualTo("java_sonar_way");

    assertThat(underTest.selectDefaultProfile(dbSession, db.organizations().insert(), "java")).isNull();
    assertThat(underTest.selectDefaultProfile(dbSession, organization, "js")).isNull();
  }

  @Test
  public void selectDefaultProfiles() {
    createSharedData();

    List<QProfileDto> java = underTest.selectDefaultProfiles(dbSession, organization, singletonList("java"));
    assertThat(java).extracting(QProfileDto::getKee).containsOnly("java_sonar_way");

    assertThat(underTest.selectDefaultProfiles(dbSession, organization, singletonList("js"))).isEmpty();
    assertThat(underTest.selectDefaultProfiles(dbSession, organization, of("java", "js"))).extracting(QProfileDto::getKee).containsOnly("java_sonar_way");
    assertThat(underTest.selectDefaultProfiles(dbSession, organization, of("js", "java"))).extracting(QProfileDto::getKee).containsOnly("java_sonar_way");
    assertThat(underTest.selectDefaultProfiles(dbSession, organization, Collections.emptyList())).isEmpty();
  }

  @Test
  public void selectByNameAndLanguage() {
    List<QProfileDto> sharedData = createSharedData();

    QProfileDto dto = underTest.selectByNameAndLanguage(dbSession, organization, "Sonar Way", "java");
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguage(dbSession, organization, "Sonar Way", "java")).isNotNull();
    assertThat(underTest.selectByNameAndLanguage(dbSession, organization, "Sonar Way", "unknown")).isNull();
  }

  @Test
  public void selectByNameAndLanguages() {
    createSharedData();

    List<QProfileDto> dtos = underTest.selectByNameAndLanguages(dbSession, organization, "Sonar Way", singletonList("java"));
    assertThat(dtos).hasSize(1);
    QProfileDto dto = dtos.iterator().next();
    assertThat(dto.getName()).isEqualTo("Sonar Way");
    assertThat(dto.getLanguage()).isEqualTo("java");
    assertThat(dto.getParentKee()).isNull();

    assertThat(underTest.selectByNameAndLanguages(dbSession, organization, "Sonar Way", singletonList("unknown"))).isEmpty();
    assertThat(underTest.selectByNameAndLanguages(dbSession, organization, "Sonar Way", of("java", "unknown")))
      .extracting(QProfileDto::getKee).containsOnly(dto.getKee());
  }

  @Test
  public void selectByLanguage() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<QProfileDto> results = underTest.selectByLanguage(dbSession, organization, profile.getLanguage());
    assertThat(results).hasSize(1);
    QProfileDto result = results.get(0);

    assertThat(result.getId()).isEqualTo(profile.getId());
    assertThat(result.getName()).isEqualTo(profile.getName());
    assertThat(result.getKee()).isEqualTo(profile.getKee());
    assertThat(result.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(result.getOrganizationUuid()).isEqualTo(profile.getOrganizationUuid());
    assertThat(result.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
  }

  @Test
  public void should_not_selectByLanguage_in_wrong_organization() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<QProfileDto> results = underTest.selectByLanguage(dbSession, OrganizationTesting.newOrganizationDto(), profile.getLanguage());
    assertThat(results).isEmpty();
  }

  @Test
  public void should_not_selectByLanguage_with_wrong_language() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    underTest.insert(dbSession, profile);

    List<QProfileDto> results = underTest.selectByLanguage(dbSession, organization, "another language");
    assertThat(results).isEmpty();
  }

  @Test
  public void selectChildren() {
    QProfileDto original1 = new QProfileDto()
      .setKee("java_child1")
      .setRulesProfileUuid("rp-java_child1")
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
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
    db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(false));

    // a built-in quality profile without active rules
    db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true));

    // a built-in quality profile with active rules
    QProfileDto builtInQPWithActiveRules = db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true));
    RuleDefinitionDto ruleDefinitionDto = db.rules().insert();
    db.qualityProfiles().activateRule(builtInQPWithActiveRules, ruleDefinitionDto);

    dbSession.commit();

    List<RulesProfileDto> rulesProfileDtos = underTest.selectBuiltInRuleProfilesWithActiveRules(dbSession);
    assertThat(rulesProfileDtos).extracting(RulesProfileDto::getName)
      .containsOnly(builtInQPWithActiveRules.getName());
  }

  @Test
  public void selectByRuleProfileUuid() {
    db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(false));
    db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true));
    QProfileDto qprofile1 = db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true));

    dbSession.commit();

    assertThat(underTest.selectByRuleProfileUuid(dbSession, db.getDefaultOrganization().getUuid(), qprofile1.getRulesProfileUuid()))
      .extracting(QProfileDto::getName)
      .isEqualTo(qprofile1.getName());

    assertThat(underTest.selectByRuleProfileUuid(dbSession, "A", qprofile1.getRulesProfileUuid()))
      .isNull();

    assertThat(underTest.selectByRuleProfileUuid(dbSession, db.getDefaultOrganization().getUuid(), "A"))
      .isNull();
  }


  @Test
  public void selectDefaultBuiltInProfilesWithoutActiveRules() {
    // a quality profile without active rules but not builtin
    db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(false).setLanguage("java"));

    // a built-in quality profile without active rules
    QProfileDto javaQPWithoutActiveRules = db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true).setLanguage("java"));
    db.qualityProfiles().setAsDefault(javaQPWithoutActiveRules);

    // a built-in quality profile without active rules
    QProfileDto cppQPWithoutActiveRules = db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true).setLanguage("cpp"));
    db.qualityProfiles().setAsDefault(cppQPWithoutActiveRules);

    // a built-in quality profile with active rules
    QProfileDto builtInQPWithActiveRules = db.qualityProfiles().insert(db.getDefaultOrganization(), qp -> qp.setIsBuiltIn(true).setLanguage("java"));
    RuleDefinitionDto ruleDefinitionDto = db.rules().insert();
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
    QProfileDto base = db.qualityProfiles().insert(db.getDefaultOrganization());

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
    QProfileDto base1 = db.qualityProfiles().insert(db.getDefaultOrganization());
    QProfileDto child1OfBase1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setParentKee(base1.getKee()));
    QProfileDto child2OfBase1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setParentKee(base1.getKee()));
    QProfileDto grandChildOfBase1 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setParentKee(child1OfBase1.getKee()));
    QProfileDto base2 = db.qualityProfiles().insert(db.getDefaultOrganization());
    QProfileDto childOfBase2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setParentKee(base2.getKee()));
    QProfileDto grandChildOfBase2 = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setParentKee(childOfBase2.getKee()));
    QProfileDto other = db.qualityProfiles().insert(db.getDefaultOrganization());

    // descendants of a single base profile
    verifyDescendants(singleton(base1), asList(child1OfBase1, child2OfBase1, grandChildOfBase1));
    verifyDescendants(singleton(child1OfBase1), asList(grandChildOfBase1));
    verifyDescendants(singleton(child2OfBase1), emptyList());
    verifyDescendants(singleton(grandChildOfBase1), emptyList());

    // descendants of a multiple base profiles
    verifyDescendants(asList(base1, base2), asList(child1OfBase1, child2OfBase1, grandChildOfBase1, childOfBase2, grandChildOfBase2));
    verifyDescendants(asList(base1, childOfBase2), asList(child1OfBase1, child2OfBase1, grandChildOfBase1, grandChildOfBase2));
    verifyDescendants(asList(child1OfBase1, grandChildOfBase2), asList(grandChildOfBase1));
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
    QProfileDto profileWithoutProjects = db.qualityProfiles().insert(organization);
    QProfileDto profileWithProjects = db.qualityProfiles().insert(organization);
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);

    db.qualityProfiles().associateWithProject(project1, profileWithProjects);
    db.qualityProfiles().associateWithProject(project2, profileWithProjects);

    OrganizationDto otherOrg = db.organizations().insert();
    QProfileDto profileInOtherOrg = db.qualityProfiles().insert(otherOrg);
    ComponentDto projectInOtherOrg = db.components().insertPrivateProject(otherOrg);
    db.qualityProfiles().associateWithProject(projectInOtherOrg, profileInOtherOrg);

    assertThat(underTest.countProjectsByOrganizationAndProfiles(dbSession, organization, asList(profileWithoutProjects, profileWithProjects, profileInOtherOrg))).containsOnly(
      MapEntry.entry(profileWithProjects.getKee(), 2L));
    assertThat(underTest.countProjectsByOrganizationAndProfiles(dbSession, otherOrg, singletonList(profileWithoutProjects))).isEmpty();
    assertThat(underTest.countProjectsByOrganizationAndProfiles(dbSession, organization, Collections.emptyList())).isEmpty();
  }

  @Test
  public void test_selectAssociatedToProjectAndLanguage() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(org);
    ComponentDto project2 = db.components().insertPublicProject(org);
    QProfileDto javaProfile = db.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(org, p -> p.setLanguage("js"));
    db.qualityProfiles().associateWithProject(project1, javaProfile, jsProfile);

    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project1, "java").getKee())
      .isEqualTo(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project1, "js").getKee())
      .isEqualTo(jsProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project1, "cobol"))
      .isNull();
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project2, "java"))
      .isNull();
  }

  @Test
  public void test_selectAssociatedToProjectUuidAndLanguages() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(org);
    ComponentDto project2 = db.components().insertPublicProject(org);
    QProfileDto javaProfile = db.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(org, p -> p.setLanguage("js"));
    db.qualityProfiles().associateWithProject(project1, javaProfile, jsProfile);

    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbSession, project1, singletonList("java")))
      .extracting(QProfileDto::getKee).containsOnly(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbSession, project1, singletonList("unknown")))
      .isEmpty();
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbSession, project1, of("java", "unknown")))
      .extracting(QProfileDto::getKee).containsExactly(javaProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbSession, project1, of("java", "js")))
      .extracting(QProfileDto::getKee).containsExactlyInAnyOrder(javaProfile.getKee(), jsProfile.getKee());
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbSession, project2, singletonList("java")))
      .isEmpty();
    assertThat(underTest.selectAssociatedToProjectUuidAndLanguages(dbSession, project2, Collections.emptyList()))
      .isEmpty();
  }

  @Test
  public void test_updateProjectProfileAssociation() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    QProfileDto javaProfile1 = db.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    QProfileDto jsProfile = db.qualityProfiles().insert(org, p -> p.setLanguage("js"));
    QProfileDto javaProfile2 = db.qualityProfiles().insert(org, p -> p.setLanguage("java"));
    db.qualityProfiles().associateWithProject(project, javaProfile1, jsProfile);

    underTest.updateProjectProfileAssociation(dbSession, project, javaProfile2.getKee(), javaProfile1.getKee());

    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project, "java").getKee()).isEqualTo(javaProfile2.getKee());
    assertThat(underTest.selectAssociatedToProjectAndLanguage(dbSession, project, "js").getKee()).isEqualTo(jsProfile.getKee());
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
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = db.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = db.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project4 = db.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));
    ComponentDto branch = db.components().insertProjectBranch(project1, t -> t.setKey("branch"));

    QProfileDto profile1 = newQualityProfileDto();
    db.qualityProfiles().insert(profile1);
    db.qualityProfiles().associateWithProject(project1, profile1);
    db.qualityProfiles().associateWithProject(project2, profile1);

    QProfileDto profile2 = newQualityProfileDto();
    db.qualityProfiles().insert(profile2);
    db.qualityProfiles().associateWithProject(project3, profile2);
    QProfileDto profile3 = newQualityProfileDto();

    assertThat(underTest.selectSelectedProjects(dbSession, organization, profile1, null))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.getDbKey(), project1.name(), profile1.getKee()),
        tuple(project2.getId(), project2.uuid(), project2.getDbKey(), project2.name(), profile1.getKee()));

    assertThat(underTest.selectSelectedProjects(dbSession, organization, profile1, "ect1")).hasSize(1);
    assertThat(underTest.selectSelectedProjects(dbSession, organization, profile3, null)).isEmpty();
  }

  @Test
  public void select_deselected_projects() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = db.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = db.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project4 = db.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));
    ComponentDto branch = db.components().insertProjectBranch(project1, t -> t.setKey("branch"));

    QProfileDto profile1 = newQualityProfileDto();
    db.qualityProfiles().insert(profile1);
    db.qualityProfiles().associateWithProject(project1, profile1);

    QProfileDto profile2 = newQualityProfileDto();
    db.qualityProfiles().insert(profile2);
    db.qualityProfiles().associateWithProject(project2, profile2);
    QProfileDto profile3 = newQualityProfileDto();

    assertThat(underTest.selectDeselectedProjects(dbSession, organization, profile1, null))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsExactly(
        tuple(project2.getId(), project2.uuid(), project2.getDbKey(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.getDbKey(), project3.name(), null));

    assertThat(underTest.selectDeselectedProjects(dbSession, organization, profile1, "ect2")).hasSize(1);
    assertThat(underTest.selectDeselectedProjects(dbSession, organization, profile3, null)).hasSize(3);
  }

  @Test
  public void select_project_associations() {
    ComponentDto project1 = db.components().insertPrivateProject(t -> t.setName("Project1 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project2 = db.components().insertPrivateProject(t -> t.setName("Project2 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    ComponentDto project3 = db.components().insertPrivateProject(t -> t.setName("Project3 name"), t -> t.setOrganizationUuid(organization.getUuid()));
    OrganizationDto organization2 = db.organizations().insert();
    ComponentDto project4 = db.components().insertPrivateProject(t -> t.setName("Project4 name"), t -> t.setOrganizationUuid(organization2.getUuid()));
    ComponentDto branch = db.components().insertProjectBranch(project1, t -> t.setKey("branch"));

    QProfileDto profile1 = newQualityProfileDto();
    db.qualityProfiles().insert(profile1);
    db.qualityProfiles().associateWithProject(project1, profile1);

    QProfileDto profile2 = newQualityProfileDto();
    db.qualityProfiles().insert(profile2);
    db.qualityProfiles().associateWithProject(project2, profile2);
    QProfileDto profile3 = newQualityProfileDto();

    assertThat(underTest.selectProjectAssociations(dbSession, organization, profile1, null))
      .extracting("projectId", "projectUuid", "projectKey", "projectName", "profileKey")
      .containsOnly(
        tuple(project1.getId(), project1.uuid(), project1.getDbKey(), project1.name(), profile1.getKee()),
        tuple(project2.getId(), project2.uuid(), project2.getDbKey(), project2.name(), null),
        tuple(project3.getId(), project3.uuid(), project3.getDbKey(), project3.name(), null));

    assertThat(underTest.selectProjectAssociations(dbSession, organization, profile1, "ect2")).hasSize(1);
    assertThat(underTest.selectProjectAssociations(dbSession, organization, profile3, null)).hasSize(3);
  }

  @Test
  public void selectUuidsOfCustomRulesProfiles_returns_the_custom_profiles_with_specified_name() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    OrganizationDto org3 = db.organizations().insert();
    QProfileDto outdatedProfile1 = db.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage("java").setName("foo"));
    QProfileDto outdatedProfile2 = db.qualityProfiles().insert(org2, p -> p.setIsBuiltIn(false).setLanguage("java").setName("foo"));
    QProfileDto builtInProfile = db.qualityProfiles().insert(org3, p -> p.setIsBuiltIn(true).setLanguage("java").setName("foo"));
    QProfileDto differentLanguage = db.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage("cobol").setName("foo"));
    QProfileDto differentName = db.qualityProfiles().insert(org1, p -> p.setIsBuiltIn(false).setLanguage("java").setName("bar"));

    Collection<String> keys = underTest.selectUuidsOfCustomRulesProfiles(dbSession, "java", "foo");

    assertThat(keys).containsExactlyInAnyOrder(outdatedProfile1.getRulesProfileUuid(), outdatedProfile2.getRulesProfileUuid());
  }

  @Test
  public void selectOutdatedProfiles_returns_empty_list_if_no_match() {
    assertThat(underTest.selectUuidsOfCustomRulesProfiles(dbSession, "java", "foo")).isEmpty();
  }

  @Test
  public void renameAndCommit_updates_name_of_specified_profiles() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    QProfileDto fooInOrg1 = db.qualityProfiles().insert(org1, p -> p.setName("foo"));
    QProfileDto fooInOrg2 = db.qualityProfiles().insert(org2, p -> p.setName("foo"));
    QProfileDto bar = db.qualityProfiles().insert(org1, p -> p.setName("bar"));

    underTest.renameRulesProfilesAndCommit(dbSession, asList(fooInOrg1.getRulesProfileUuid(), fooInOrg2.getRulesProfileUuid()), "foo (copy)");

    assertThat(underTest.selectOrFailByUuid(dbSession, fooInOrg1.getKee()).getName()).isEqualTo("foo (copy)");
    assertThat(underTest.selectOrFailByUuid(dbSession, fooInOrg2.getKee()).getName()).isEqualTo("foo (copy)");
    assertThat(underTest.selectOrFailByUuid(dbSession, bar.getKee()).getName()).isEqualTo("bar");
  }

  @Test
  public void renameAndCommit_does_nothing_if_empty_keys() {
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(org, p -> p.setName("foo"));

    underTest.renameRulesProfilesAndCommit(dbSession, Collections.emptyList(), "foo (copy)");

    assertThat(underTest.selectOrFailByUuid(dbSession, profile.getKee()).getName()).isEqualTo("foo");
  }

  @Test
  public void selectQProfilesByRuleProfileUuid() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();

    RulesProfileDto ruleProfile1 = QualityProfileTesting.newRuleProfileDto();
    OrgQProfileDto profile1InOrg1 = new OrgQProfileDto().setOrganizationUuid(org1.getUuid()).setRulesProfileUuid(ruleProfile1.getKee()).setUuid(Uuids.create());
    OrgQProfileDto profile1InOrg2 = new OrgQProfileDto().setOrganizationUuid(org2.getUuid()).setRulesProfileUuid(ruleProfile1.getKee()).setUuid(Uuids.create());
    RulesProfileDto ruleProfile2 = QualityProfileTesting.newRuleProfileDto();
    OrgQProfileDto profile2InOrg1 = new OrgQProfileDto().setOrganizationUuid(org1.getUuid()).setRulesProfileUuid(ruleProfile2.getKee()).setUuid(Uuids.create());
    db.getDbClient().qualityProfileDao().insert(db.getSession(), ruleProfile1);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), profile1InOrg1);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), profile1InOrg2);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), ruleProfile2);
    db.getDbClient().qualityProfileDao().insert(db.getSession(), profile2InOrg1);

    List<QProfileDto> result = db.getDbClient().qualityProfileDao().selectQProfilesByRuleProfile(db.getSession(), ruleProfile1);
    assertThat(result).extracting(QProfileDto::getKee).containsExactlyInAnyOrder(profile1InOrg1.getUuid(), profile1InOrg2.getUuid());

    result = db.getDbClient().qualityProfileDao().selectQProfilesByRuleProfile(db.getSession(), ruleProfile2);
    assertThat(result).extracting(QProfileDto::getKee).containsExactlyInAnyOrder(profile2InOrg1.getUuid());
  }

  @Test
  public void selectQProfilesByRuleProfileUuid_returns_empty_list_if_rule_profile_does_not_exist() {
    List<QProfileDto> result = db.getDbClient().qualityProfileDao().selectQProfilesByRuleProfile(db.getSession(), new RulesProfileDto().setKee("unknown"));

    assertThat(result).isEmpty();
  }

  private List<QProfileDto> createSharedData() {
    QProfileDto dto1 = new QProfileDto()
      .setKee("java_sonar_way")
      .setRulesProfileUuid("rp-java_sonar_way")
      .setOrganizationUuid(organization.getUuid())
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
      .setOrganizationUuid(organization.getUuid())
      .setName("Sonar Way")
      .setLanguage("js")
      .setLastUsed(1_000L)
      .setUserUpdatedAt(2_000L)
      .setRulesUpdatedAt("2017-05-31")
      .setIsBuiltIn(true);
    underTest.insert(dbSession, dto2);

    DefaultQProfileDto defaultQProfileDto = new DefaultQProfileDto()
      .setQProfileUuid(dto1.getKee())
      .setLanguage(dto1.getLanguage())
      .setOrganizationUuid(organization.getUuid());
    db.getDbClient().defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(dto1));

    return Arrays.asList(dto1, dto2);
  }
}
