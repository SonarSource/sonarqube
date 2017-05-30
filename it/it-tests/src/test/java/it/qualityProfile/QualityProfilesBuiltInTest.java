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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static it.Category6Suite.enableOrganizationsSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newOrganizationKey;

public class QualityProfilesBuiltInTest {

  private static final String ANOTHER_ORGANIZATION = newOrganizationKey();
  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    enableOrganizationsSupport();
    adminWsClient = newAdminWsClient(orchestrator);
  }

  @Test
  public void xoo_profiles_provided() {
    SearchWsResponse result = adminWsClient.qualityProfiles().search(new SearchWsRequest());

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getOrganization, QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("default-organization", "Basic", "xoo", true, true),
        tuple("default-organization", "empty", "xoo", true, false),
        tuple("default-organization", "Basic", "xoo2", true, true));
  }

  @Test
  public void xoo_profiles_provided_copied_to_new_organization() {
    adminWsClient.organizations().create(new CreateWsRequest.Builder()
      .setKey(ANOTHER_ORGANIZATION)
      .setName(ANOTHER_ORGANIZATION).build());
    SearchWsResponse result = adminWsClient.qualityProfiles().search(new SearchWsRequest()
      .setOrganizationKey(ANOTHER_ORGANIZATION));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getOrganization, QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple(ANOTHER_ORGANIZATION, "Basic", "xoo", true, true),
        tuple(ANOTHER_ORGANIZATION, "empty", "xoo", true, false),
        tuple(ANOTHER_ORGANIZATION, "Basic", "xoo2", true, true));
  }
}
