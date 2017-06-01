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
package it.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import it.Category6Suite;
import java.util.function.Predicate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.qualityprofile.ActivateRuleWsRequest;
import org.sonarqube.ws.client.qualityprofile.CopyRequest;
import org.sonarqube.ws.client.qualityprofile.DeleteRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import org.sonarqube.ws.client.qualityprofile.SetDefaultRequest;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static org.apache.commons.lang.RandomStringUtils.randomAscii;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.fail;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newOrganizationKey;

public class QualityProfilesBuiltInTest {

  private static final String ORGANIZATION = newOrganizationKey();
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    enableOrganizationsSupport();
    adminWsClient = newAdminWsClient(orchestrator);
    adminWsClient.organizations().create(new CreateWsRequest.Builder()
      .setKey(ORGANIZATION)
      .setName(ORGANIZATION).build());
  }

  @AfterClass
  public static void tearDown() {
    adminWsClient.organizations().delete(ORGANIZATION);
  }

  @Test
  public void built_in_profiles_provided_copied_to_new_organization() {
    SearchWsResponse result = adminWsClient.qualityProfiles().search(new SearchWsRequest().setOrganizationKey(ORGANIZATION));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("Basic", "xoo", true, true),
        tuple("empty", "xoo", true, false),
        tuple("Basic", "xoo2", true, true));
  }

  @Test
  public void built_in_profiles_provided_for_default_organization() {
    SearchWsResponse result = adminWsClient.qualityProfiles().search(new SearchWsRequest().setOrganizationKey("default-organization"));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getOrganization, QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("default-organization", "Basic", "xoo", true, true),
        tuple("default-organization", "empty", "xoo", true, false),
        tuple("default-organization", "Basic", "xoo2", true, true));
  }

  @Test
  public void cannot_delete_built_in_profile_even_when_non_default() {
    QualityProfile defaultBuiltInProfile = getProfile(p -> p.getIsBuiltIn() && p.getIsDefault() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));

    QualityProfiles.CopyWsResponse copiedProfile = adminWsClient.qualityProfiles().copy(new CopyRequest(defaultBuiltInProfile.getKey(), randomAscii(20)));
    adminWsClient.qualityProfiles().setDefault(new SetDefaultRequest(copiedProfile.getKey()));

    try {
      adminWsClient.qualityProfiles().delete(new DeleteRequest(defaultBuiltInProfile.getKey()));
      fail();
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(400);
      assertThat(e.content()).contains("Operation forbidden for built-in Quality Profile 'Basic' with language 'xoo'");
    } finally {
      adminWsClient.qualityProfiles().setDefault(new SetDefaultRequest(defaultBuiltInProfile.getKey()));
      adminWsClient.qualityProfiles().delete(new DeleteRequest(copiedProfile.getKey()));
    }
  }

  @Test
  public void fail_to_modify_built_in_quality_profile() {
    QualityProfile profile = getProfile(p -> p.getIsBuiltIn() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));
    assertThat(profile.getIsBuiltIn()).isTrue();

    try {
      adminWsClient.qualityProfiles().activateRule(ActivateRuleWsRequest.builder()
        .setOrganization(ORGANIZATION)
        .setProfileKey(profile.getKey())
        .setRuleKey(RULE_ONE_BUG_PER_LINE)
        .build());
      fail();
    } catch (HttpException e) {
      assertThat(e.code()).isEqualTo(HTTP_BAD_REQUEST);
      assertThat(e.content()).contains("Operation forbidden for built-in Quality Profile 'Basic' with language 'xoo'");
    }
  }

  private QualityProfile getProfile(Predicate<QualityProfile> filter) {
    return adminWsClient.qualityProfiles().search(new SearchWsRequest()
      .setOrganizationKey(ORGANIZATION)).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}
