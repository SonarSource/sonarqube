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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RegisterQualityProfilesTest {
  private static final Language FOO_LANGUAGE = LanguageTesting.newLanguage("foo", "foo", "foo");
  private static final Language BAR_LANGUAGE = LanguageTesting.newLanguage("bar", "bar", "bar");

  @Rule
  public DbTester dbTester = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DefinedQProfileRepositoryRule definedQProfileRepositoryRule = new DefinedQProfileRepositoryRule();

  private DbClient dbClient = dbTester.getDbClient();
  private DbClient mockedDbClient = mock(DbClient.class);
  private ActiveRuleIndexer mockedActiveRuleIndexer = mock(ActiveRuleIndexer.class);
  private DummyDefinedQProfileCreation definedQProfileCreation = new DummyDefinedQProfileCreation();
  private RegisterQualityProfiles underTest = new RegisterQualityProfiles(
    definedQProfileRepositoryRule,
    dbClient,
    definedQProfileCreation,
    mockedActiveRuleIndexer);

  @Test
  public void start_fails_if_DefinedQProfileRepository_has_not_been_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("initialize must be called first");

    underTest.start();
  }

  @Test
  public void no_action_in_DB_nothing_to_index_when_there_is_no_DefinedQProfile() {
    RegisterQualityProfiles underTest = new RegisterQualityProfiles(definedQProfileRepositoryRule, mockedDbClient, null, mockedActiveRuleIndexer);
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(definedQProfileCreation.getCallLogs()).isEmpty();
    verify(mockedDbClient).openSession(false);
    verify(mockedActiveRuleIndexer).index(Collections.emptyList());
    verifyNoMoreInteractions(mockedDbClient, mockedActiveRuleIndexer);
  }

  @Test
  public void start_creates_qps_for_every_organization_in_DB_when_LoadedTemplate_table_is_empty() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1");
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(definedQProfileCreation.getCallLogs())
      .containsExactly(
        callLog(definedQProfile, dbTester.getDefaultOrganization()),
        callLog(definedQProfile, organization1),
        callLog(definedQProfile, organization2));
  }

  @Test
  public void start_creates_qps_only_for_organizations_in_DB_without_loaded_template() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    DefinedQProfile definedQProfile = definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1");
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(dbTester.getDefaultOrganization().getUuid(), definedQProfile.getLoadedTemplateType()), dbTester.getSession());
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(org1.getUuid(), definedQProfile.getLoadedTemplateType()), dbTester.getSession());
    dbTester.commit();
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(definedQProfileCreation.getCallLogs())
      .containsExactly(callLog(definedQProfile, org2));
  }

  @Test
  public void start_creates_different_qps_and_their_loaded_templates_if_several_profile_has_same_name_for_different_languages() {
    String name = "doh";

    DefinedQProfile definedQProfile1 = definedQProfileRepositoryRule.add(FOO_LANGUAGE, name, true);
    DefinedQProfile definedQProfile2 = definedQProfileRepositoryRule.add(BAR_LANGUAGE, name, true);
    definedQProfileRepositoryRule.initialize();

    underTest.start();

    assertThat(definedQProfileCreation.getCallLogs())
      .containsExactly(callLog(definedQProfile2, dbTester.getDefaultOrganization()), callLog(definedQProfile1, dbTester.getDefaultOrganization()));
  }

  @Test
  public void start_indexes_ActiveRuleChanges_in_order() {
    dbTester.organizations().insert();
    dbTester.organizations().insert();
    dbTester.organizations().insert();
    definedQProfileRepositoryRule.add(FOO_LANGUAGE, "foo1", false);
    definedQProfileRepositoryRule.initialize();
    ActiveRuleChange ruleChange1 = newActiveRuleChange("1");
    ActiveRuleChange ruleChange2 = newActiveRuleChange("2");
    ActiveRuleChange ruleChange3 = newActiveRuleChange("3");
    ActiveRuleChange ruleChange4 = newActiveRuleChange("4");
    definedQProfileCreation.addChangesPerCall(ruleChange1, ruleChange3);
    // no change for second org
    definedQProfileCreation.addChangesPerCall();
    definedQProfileCreation.addChangesPerCall(ruleChange2);
    definedQProfileCreation.addChangesPerCall(ruleChange4);
    ArgumentCaptor<List<ActiveRuleChange>> indexedChangesCaptor = ArgumentCaptor.forClass((Class<List<ActiveRuleChange>>) (Object) List.class);
    doNothing().when(mockedActiveRuleIndexer).index(indexedChangesCaptor.capture());

    underTest.start();

    assertThat(indexedChangesCaptor.getValue())
      .containsExactly(ruleChange1, ruleChange3, ruleChange2, ruleChange4);
  }

  private static ActiveRuleChange newActiveRuleChange(String id) {
    return ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(id, RuleKey.of(id + "1", id + "2")));
  }

  private class DummyDefinedQProfileCreation implements DefinedQProfileCreation {
    private List<List<ActiveRuleChange>> changesPerCall;
    private Iterator<List<ActiveRuleChange>> changesPerCallIterator;
    private final List<CallLog> callLogs = new ArrayList<>();

    @Override
    public void create(DbSession session, DefinedQProfile qualityProfile, OrganizationDto organization, List<ActiveRuleChange> changes) {
      callLogs.add(callLog(qualityProfile, organization));

      // RegisterQualityProfiles relies on the fact that DefinedQProfileCreation populates table LOADED_TEMPLATE each time create is called
      // to not loop infinitely
      dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(organization.getUuid(), qualityProfile.getLoadedTemplateType()), session);

      if (changesPerCall != null) {
        if (changesPerCallIterator == null) {
          this.changesPerCallIterator = changesPerCall.iterator();
        }
        changes.addAll(changesPerCallIterator.next());
      }
    }

    void addChangesPerCall(ActiveRuleChange... changes) {
      if (changesPerCall == null) {
        this.changesPerCall = new ArrayList<>();
      }
      changesPerCall.add(Arrays.asList(changes));
    }

    List<CallLog> getCallLogs() {
      return callLogs;
    }
  }

  private static final class CallLog {
    private final DefinedQProfile definedQProfile;
    private final OrganizationDto organization;

    private CallLog(DefinedQProfile definedQProfile, OrganizationDto organization) {
      this.definedQProfile = definedQProfile;
      this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CallLog callLog = (CallLog) o;
      return definedQProfile == callLog.definedQProfile &&
        organization.getUuid().equals(callLog.organization.getUuid());
    }

    @Override
    public int hashCode() {
      return Objects.hash(definedQProfile, organization);
    }

    @Override
    public String toString() {
      return "CallLog{" +
        "qp=" + definedQProfile.getLanguage() + '-' + definedQProfile.getName() + '-' + definedQProfile.isDefault() +
        ", org=" + organization.getKey() +
        '}';
    }
  }

  private static CallLog callLog(DefinedQProfile definedQProfile, OrganizationDto organizationDto) {
    return new CallLog(definedQProfile, organizationDto);
  }
}
