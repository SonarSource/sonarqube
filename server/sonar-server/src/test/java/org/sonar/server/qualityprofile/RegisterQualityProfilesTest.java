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
package org.sonar.server.qualityprofile;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.UtcDateUtils.formatDateTime;

public class RegisterQualityProfilesTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo", "foo", "foo");
  private static final Language BAR_LANGUAGE = LanguageTesting.newLanguage("bar", "bar", "bar");
  private static final String TABLE_RULES_PROFILES = "RULES_PROFILES";
  private static final String TABLE_LOADED_TEMPLATES = "loaded_templates";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DefinedQProfileRepositoryRule definedQProfileRepositoryRule = new DefinedQProfileRepositoryRule();

  private DbClient dbClient = dbTester.getDbClient();
  private DbClient mockedDbClient = mock(DbClient.class);
  private UuidFactory mockedUuidFactory = mock(UuidFactory.class);
  private System2 mockedSystem2 = mock(System2.class);
  private ActiveRuleIndexer mockedActiveRuleIndexer = mock(ActiveRuleIndexer.class);
  private RegisterQualityProfiles underTest = new RegisterQualityProfiles(
    definedQProfileRepositoryRule,
    dbClient,
    new QProfileFactory(dbClient, mockedUuidFactory, mockedSystem2),
    new CachingRuleActivator(mockedSystem2, dbClient, null, new CachingRuleActivatorContextFactory(dbClient), null, null, userSessionRule),
    mockedActiveRuleIndexer);

  @Test
  public void start_fails_if_DefinedQProfileRepository_has_not_been_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");
    
    underTest.start();
  }

  @Test
  public void no_action_in_DB_nothing_to_index_when_there_is_no_DefinedQProfile() {
    RegisterQualityProfiles underTest = new RegisterQualityProfiles(definedQProfileRepositoryRule, mockedDbClient, null, null, mockedActiveRuleIndexer);
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(definedQProfileRepositoryRule.isInitialized()).isTrue();
    verify(mockedDbClient).openSession(false);
    verify(mockedActiveRuleIndexer).index(Collections.emptyList());
    verifyNoMoreInteractions(mockedDbClient, mockedActiveRuleIndexer);
  }

  @Test
  public void start_creates_qp_and_store_flag_in_loaded_templates_for_each_organization_id_BD() {
    OrganizationDto otherOrganization = dbTester.organizations().insert();
    String[] uuids = {"uuid 1", "uuid 2"};
    Long[] dates = {2_456_789L, 6_123_789L};
    String[] formattedDates = {formatDateTime(new Date(dates[0])), formatDateTime(new Date(dates[1]))};
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1");
    mockForQPInserts(uuids, dates);
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(definedQProfileRepositoryRule.isInitialized()).isTrue();
    Arrays.asList(dbTester.getDefaultOrganization(), otherOrganization)
      .forEach(organization -> {
        QualityProfileDto dto = getPersistedQP(organization, FOO_LANGUAGE, "foo1");
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getOrganizationUuid()).isEqualTo(organization.getUuid());
        assertThat(dto.getLanguage()).isEqualTo(FOO_LANGUAGE.getKey());
        assertThat(dto.getName()).isEqualTo("foo1");
        assertThat(dto.getKee()).isIn(uuids);
        assertThat(dto.getKey()).isEqualTo(dto.getKee());
        assertThat(dto.getParentKee()).isNull();
        assertThat(dto.getRulesUpdatedAt()).isIn(formattedDates);
        assertThat(dto.getLastUsed()).isNull();
        assertThat(dto.getUserUpdatedAt()).isNull();
        assertThat(dto.isDefault()).isFalse();

        assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(definedQProfile.getLoadedTemplateType(), organization.getUuid(), dbTester.getSession()))
          .isEqualTo(1);
      });
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(2);
  }

  @Test
  public void start_persists_default_flag_of_DefinedQualityProfile() {
    definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1", false);
    definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo2", true);
    definedQProfileRepositoryRule.add(BAR_LANGUAGE, "bar1", true);
    definedQProfileRepositoryRule.add(BAR_LANGUAGE, "bar2", false);
    mockForQPInserts(new String[]{"uuid1", "uuid2", "uuid3", "uuid4"}, new Long[]{ 1L, 2L, 3L, 4L});
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo1").isDefault()).isFalse();
    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), FOO_LANGUAGE, "foo2").isDefault()).isTrue();
    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), BAR_LANGUAGE, "bar1").isDefault()).isTrue();
    assertThat(getPersistedQP(dbTester.getDefaultOrganization(), BAR_LANGUAGE, "bar2").isDefault()).isFalse();
  }

  @Test
  public void start_does_not_create_sq_if_loaded_profile_of_organization_already_exists() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1");
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(dbTester.getDefaultOrganization().getUuid(), definedQProfile.getLoadedTemplateType()), dbTester.getSession());
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(org1.getUuid(), definedQProfile.getLoadedTemplateType()), dbTester.getSession());
    dbTester.commit();
    mockForSingleQPInsert();
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(definedQProfile.getLoadedTemplateType(), org2.getUuid(), dbTester.getSession()))
      .isEqualTo(1);
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_LOADED_TEMPLATES)).isEqualTo(3);
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(1);
  }

  @Test
  public void start_creates_different_qps_and_their_loaded_templates_if_several_profile_has_same_name_for_different_languages() {
    String uuid1 = "uuid1";
    String uuid2 = "uuid2";
    long date1 = 2_456_789L;
    long date2 = 4_231_654L;
    String name = "doh";

    DefinedQProfile definedQProfile1 = definedQProfileRepositoryRule.add(FOO_LANGUAGE, name, true);
    DefinedQProfile definedQProfile2 = definedQProfileRepositoryRule.add(BAR_LANGUAGE, name, true);
    mockForQPInserts(new String[]{uuid1, uuid2}, new Long[]{date1, date2});
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    OrganizationDto organization = dbTester.getDefaultOrganization();
    QualityProfileDto dto = getPersistedQP(organization, FOO_LANGUAGE, name);
    String uuidQP1 = dto.getKee();
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(FOO_LANGUAGE.getKey());
    assertThat(dto.getName()).isEqualTo(name);
    assertThat(uuidQP1).isIn(uuid1, uuid2);
    assertThat(dto.getKey()).isEqualTo(uuidQP1);
    assertThat(dto.getParentKee()).isNull();
    assertThat(dto.getRulesUpdatedAt()).isIn(formatDateTime(new Date(date1)), formatDateTime(new Date(date2)));
    assertThat(dto.getLastUsed()).isNull();
    assertThat(dto.getUserUpdatedAt()).isNull();
    assertThat(dto.isDefault()).isTrue();
    dto = getPersistedQP(dbTester.getDefaultOrganization(), BAR_LANGUAGE, name);
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(BAR_LANGUAGE.getKey());
    assertThat(dto.getName()).isEqualTo(name);
    String uuidQP2 = uuid1.equals(uuidQP1) ? uuid2 : uuid1;
    long dateQP2 = uuid1.equals(uuidQP1) ? date2 : date1;
    assertThat(dto.getKee()).isEqualTo(uuidQP2);
    assertThat(dto.getKey()).isEqualTo(uuidQP2);
    assertThat(dto.getParentKee()).isNull();
    assertThat(dto.getRulesUpdatedAt()).isEqualTo(formatDateTime(new Date(dateQP2)));
    assertThat(dto.getLastUsed()).isNull();
    assertThat(dto.getUserUpdatedAt()).isNull();
    assertThat(dto.isDefault()).isTrue();
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), TABLE_RULES_PROFILES)).isEqualTo(2);

    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(definedQProfile1.getLoadedTemplateType(), organization.getUuid(), dbTester.getSession()))
      .isEqualTo(1);
    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(definedQProfile2.getLoadedTemplateType(), organization.getUuid(), dbTester.getSession()))
      .isEqualTo(1);
  }

  private void mockForSingleQPInsert() {
    mockForSingleQPInsert("generated uuid", 2_456_789);
  }

  private void mockForSingleQPInsert(String uuid, long now) {
    when(mockedUuidFactory.create()).thenReturn(uuid).thenThrow(new UnsupportedOperationException("uuidFactory should be called only once"));
    when(mockedSystem2.now()).thenReturn(now).thenThrow(new UnsupportedOperationException("now should be called only once"));
  }

  private void mockForQPInserts(String[] uuids, Long[] dates) {
    when(mockedUuidFactory.create())
      .thenReturn(uuids[0], Arrays.copyOfRange(uuids, 1, uuids.length))
      .thenThrow(new UnsupportedOperationException("uuidFactory should be called only " + uuids.length + " times"));

    when(mockedSystem2.now())
      .thenReturn(dates[0], Arrays.copyOfRange(dates, 1, dates.length))
      .thenThrow(new UnsupportedOperationException("now should be called only " + dates.length + " times"));
  }

  private QualityProfileDto getPersistedQP(OrganizationDto organization, Language language, String name) {
    return dbClient.qualityProfileDao().selectByNameAndLanguage(organization, name, language.getKey(), dbTester.getSession());
  }

}
