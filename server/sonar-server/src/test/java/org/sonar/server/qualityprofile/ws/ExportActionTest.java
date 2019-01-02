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
package org.sonar.server.qualityprofile.ws;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileRestoreSummary;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class ExportActionTest {

  private static final String XOO_LANGUAGE = "xoo";
  private static final String JAVA_LANGUAGE = "java";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private QProfileBackuper backuper = new TestBackuper();
  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(db));

  @Test
  public void export_profile_with_key() {
    QProfileDto profile = createProfile(db.getDefaultOrganization(), false);

    WsActionTester tester = newWsActionTester(newExporter("polop"), newExporter("palap"));
    String result = tester.newRequest()
      .setParam(PARAM_KEY, profile.getKee())
      .setParam("exporterKey", "polop")
      .execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + profile.getLanguage() + "/" + profile.getName() + " exported by polop");
  }

  @Test
  public void export_profile_in_default_organization() {
    QProfileDto profile = createProfile(db.getDefaultOrganization(), false);

    WsActionTester tester = newWsActionTester(newExporter("polop"), newExporter("palap"));
    String result = tester.newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .setParam("exporterKey", "polop").execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + profile.getLanguage() + "/" + profile.getName() + " exported by polop");
  }

  @Test
  public void export_profile_in_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = createProfile(organization, false);

    WsActionTester tester = newWsActionTester(newExporter("polop"), newExporter("palap"));
    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .setParam("exporterKey", "polop").execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + profile.getLanguage() + "/" + profile.getName() + " exported by polop");
  }

  @Test
  public void export_default_profile() {
    QProfileDto nonDefaultProfile = createProfile(db.getDefaultOrganization(), false);
    QProfileDto defaultProfile = createProfile(db.getDefaultOrganization(), true);

    WsActionTester tester = newWsActionTester(newExporter("polop"), newExporter("palap"));
    String result = tester.newRequest()
      .setParam("language", XOO_LANGUAGE)
      .setParam("exporterKey", "polop")
      .execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + defaultProfile.getLanguage() + "/" + defaultProfile.getName() + " exported by polop");
  }

  @Test
  public void return_backup_when_exporter_is_not_specified() {
    OrganizationDto organization = db.getDefaultOrganization();
    QProfileDto profile = createProfile(organization, false);

    String result = newWsActionTester(newExporter("polop")).newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .execute()
      .getInput();

    assertThat(result).isEqualTo("Backup of " + profile.getLanguage() + "/" + profile.getKee());
  }

  @Test
  public void do_not_mismatch_profiles_with_other_organizations_and_languages() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    QProfileDto defaultJavaInOrg1 = db.qualityProfiles().insert(org1, p -> p.setLanguage(JAVA_LANGUAGE).setName("Sonar Way"));
    QProfileDto nonDefaultJavaInOrg1 = db.qualityProfiles().insert(org1, p -> p.setLanguage(JAVA_LANGUAGE).setName("My Way"));
    QProfileDto defaultXooInOrg1 = db.qualityProfiles().insert(org1, p -> p.setLanguage(XOO_LANGUAGE).setName("Sonar Way"));
    QProfileDto nonDefaultXooInOrg1 = db.qualityProfiles().insert(org1, p -> p.setLanguage(XOO_LANGUAGE).setName("My Way"));
    QProfileDto defaultJavaInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(JAVA_LANGUAGE).setName("Sonar Way"));
    QProfileDto nonDefaultJavaInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(JAVA_LANGUAGE).setName("My Way"));
    QProfileDto defaultXooInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(XOO_LANGUAGE).setName("Sonar Way"));
    QProfileDto nonDefaultXooInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(XOO_LANGUAGE).setName("My Way"));
    db.qualityProfiles().setAsDefault(defaultJavaInOrg1, defaultJavaInOrg2, defaultXooInOrg1, defaultXooInOrg2);

    WsActionTester tester = newWsActionTester();

    // default profile for specified organization and language
    assertThat(tester.newRequest()
      .setParam("organization", org1.getKey())
      .setParam("language", defaultJavaInOrg1.getLanguage())
      .execute()
      .getInput())
        .isEqualTo("Backup of java/" + defaultJavaInOrg1.getKee());

    // profile for specified organization, language and name --> do not mix with Xoo profile or profile with same lang/name on other
    // organization
    assertThat(tester.newRequest()
      .setParam("organization", org1.getKey())
      .setParam("language", defaultJavaInOrg1.getLanguage())
      .setParam("name", defaultJavaInOrg1.getName())
      .execute()
      .getInput())
        .isEqualTo("Backup of java/" + defaultJavaInOrg1.getKee());
  }

  @Test
  public void export_profile_in_paid_organization() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QProfileDto profile = createProfile(organization, false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addMembership(organization);

    WsActionTester tester = newWsActionTester(newExporter("polop"));
    String result = tester.newRequest()
      .setParam("organization", organization.getKey())
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .setParam("exporterKey", "polop").execute()
      .getInput();

    assertThat(result).isEqualTo("Profile " + profile.getLanguage() + "/" + profile.getName() + " exported by polop");
  }

  @Test
  public void throw_NotFoundException_if_profile_with_specified_name_does_not_exist_in_default_organization() {
    expectedException.expect(NotFoundException.class);

    newWsActionTester().newRequest()
      .setParam("language", XOO_LANGUAGE)
      .setParam("exporterKey", "polop").execute();
  }

  @Test
  public void throw_IAE_if_export_with_specified_key_does_not_exist() {
    QProfileDto profile = createProfile(db.getDefaultOrganization(), true);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'exporterKey' (unknown) must be one of: [polop, palap]");

    newWsActionTester(newExporter("polop"), newExporter("palap")).newRequest()
      .setParam("language", XOO_LANGUAGE)
      .setParam("exporterKey", "unknown").execute();
  }

  @Test
  public void throw_NotFoundException_if_specified_organization_does_not_exist() {
    WsActionTester tester = newWsActionTester(newExporter("foo"));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'does_not_exist'");

    tester.newRequest()
      .setParam("organization", "does_not_exist")
      .setParam("language", XOO_LANGUAGE)
      .setParam("name", "bar")
      .setParam("exporterKey", "foo")
      .execute();
  }

  @Test
  public void fail_if_profile_key_is_unknown() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Could not find profile with key 'PROFILE-KEY-404'");

    WsActionTester ws = newWsActionTester(newExporter("polop"), newExporter("palap"));
    ws.newRequest()
      .setParam(PARAM_KEY, "PROFILE-KEY-404")
      .setParam("exporterKey", "polop").execute()
      .getInput();
  }

  @Test
  public void fail_if_profile_key_and_language_provided() {
    QProfileDto profile = createProfile(db.getDefaultOrganization(), false);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Either 'key' or 'language' must be provided");

    WsActionTester ws = newWsActionTester(newExporter("polop"), newExporter("palap"));
    ws.newRequest()
      .setParam(PARAM_KEY, profile.getKee())
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam("exporterKey", "polop").execute()
      .getInput();
  }

  @Test
  public void fail_on_paid_organization_when_not_member() {
    WsActionTester tester = newWsActionTester(newExporter("foo"));
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO_LANGUAGE));

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(format("You're not member of organization '%s'", organization.getKey()));

    tester.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, qualityProfile.getName())
      .setParam(PARAM_LANGUAGE, XOO_LANGUAGE)
      .setParam("exporterKey", "foo")
      .execute();
  }

  @Test
  public void definition_without_exporters() {
    WebService.Action definition = newWsActionTester().getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("key", "language", "qualityProfile", "organization");

    WebService.Param organizationParam = definition.param("organization");
    assertThat(organizationParam.since()).isEqualTo("6.4");
    assertThat(organizationParam.isInternal()).isTrue();

    WebService.Param key = definition.param("key");
    assertThat(key.since()).isEqualTo("6.5");
    assertThat(key.deprecatedSince()).isEqualTo("6.6");

    WebService.Param name = definition.param("qualityProfile");
    assertThat(name.deprecatedSince()).isNullOrEmpty();
    assertThat(name.deprecatedKey()).isEqualTo("name");

    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  @Test
  public void definition_with_exporters() {
    WebService.Action definition = newWsActionTester(newExporter("polop"), newExporter("palap")).getDef();

    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting("key").containsExactlyInAnyOrder("key", "language", "qualityProfile", "organization", "exporterKey");
    WebService.Param exportersParam = definition.param("exporterKey");
    assertThat(exportersParam.possibleValues()).containsOnly("polop", "palap");
    assertThat(exportersParam.deprecatedKey()).isEqualTo("format");
    assertThat(exportersParam.deprecatedKeySince()).isEqualTo("6.3");
    assertThat(exportersParam.isInternal()).isFalse();
  }

  private QProfileDto createProfile(OrganizationDto organization, boolean isDefault) {
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO_LANGUAGE));
    if (isDefault) {
      db.qualityProfiles().setAsDefault(profile);
    }
    return profile;
  }

  private WsActionTester newWsActionTester(ProfileExporter... profileExporters) {
    QProfileExporters exporters = new QProfileExporters(dbClient, null, null, profileExporters, null);
    return new WsActionTester(new ExportAction(dbClient, backuper, exporters, LanguageTesting.newLanguages(XOO_LANGUAGE, JAVA_LANGUAGE), wsSupport));
  }

  private static ProfileExporter newExporter(String key) {
    return new ProfileExporter(key, StringUtils.capitalize(key)) {
      @Override
      public String getMimeType() {
        return "text/plain+" + key;
      }

      @Override
      public void exportProfile(RulesProfile profile, Writer writer) {
        try {
          writer.write(format("Profile %s/%s exported by %s", profile.getLanguage(), profile.getName(), key));
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    };
  }

  private static class TestBackuper implements QProfileBackuper {

    @Override
    public void backup(DbSession dbSession, QProfileDto profile, Writer backupWriter) {
      try {
        backupWriter.write(format("Backup of %s/%s", profile.getLanguage(), profile.getKee()));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile) {
      throw new UnsupportedOperationException();
    }
  }
}
