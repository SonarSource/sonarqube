/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.organization;

import com.sonar.orchestrator.Orchestrator;
import it.Category3Suite;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.OrganizationService;
import org.sonarqube.ws.client.organization.SearchWsRequest;
import org.sonarqube.ws.client.organization.UpdateWsRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationIt {
  private static final String DEFAULT_ORGANIZATION_KEY = "default-organization";
  private static final String NAME = "Foo Company";
  private static final String KEY = "foo-company";
  private static final String DESCRIPTION = "the description of Foo company";
  private static final String URL = "https://www.foo.fr";
  private static final String AVATAR_URL = "https://www.foo.fr/corporate_logo.png";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  private OrganizationService anonymousOrganizationService = ItUtils.newWsClient(orchestrator).organizations();
  private OrganizationService adminOrganizationService = ItUtils.newAdminWsClient(orchestrator).organizations();

  @Test
  public void create_update_delete_an_organization() {
    verifyNoExtraOrganization();

    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(NAME)
      .setKey(KEY)
      .setDescription(DESCRIPTION)
      .setUrl(URL)
      .setAvatar(AVATAR_URL)
      .build())
      .getOrganization();
    assertThat(createdOrganization.getName()).isEqualTo(NAME);
    assertThat(createdOrganization.getKey()).isEqualTo(KEY);
    assertThat(createdOrganization.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(createdOrganization.getUrl()).isEqualTo(URL);
    assertThat(createdOrganization.getAvatar()).isEqualTo(AVATAR_URL);

    verifySingleSearchResult(createdOrganization, NAME, DESCRIPTION, URL, AVATAR_URL);

    // update by id
    adminOrganizationService.update(new UpdateWsRequest.Builder()
      .setKey(createdOrganization.getKey())
      .setName("new name")
      .setDescription("new description")
      .setUrl("new url")
      .setAvatar("new avatar url")
      .build());
    verifySingleSearchResult(createdOrganization, "new name", "new description", "new url", "new avatar url");

    // update by key
    adminOrganizationService.update(new UpdateWsRequest.Builder()
      .setKey(createdOrganization.getKey())
      .setName("new name 2")
      .setDescription("new description 2")
      .setUrl("new url 2")
      .setAvatar("new avatar url 2")
      .build());
    verifySingleSearchResult(createdOrganization, "new name 2", "new description 2", "new url 2", "new avatar url 2");

    // remove optional fields
    adminOrganizationService.update(new UpdateWsRequest.Builder()
      .setKey(createdOrganization.getKey())
      .setName("new name 3")
      .build());
    verifySingleSearchResult(createdOrganization, "new name 3", null, null, null);

    // delete organization
    adminOrganizationService.delete(createdOrganization.getKey());
    verifyNoExtraOrganization();
  }

  @Test
  public void create_generates_key_from_name() {
    // create organization without key
    String name = "Foo  Company to keyize";
    Organizations.Organization createdOrganization = adminOrganizationService.create(new CreateWsRequest.Builder()
      .setName(name)
      .build())
      .getOrganization();
    assertThat(createdOrganization.getKey()).isEqualTo("foo-company-to-keyize");
    verifySingleSearchResult(createdOrganization, name, null, null, null);
  }

  private void verifyNoExtraOrganization() {
    Organizations.SearchWsResponse searchWsResponse = anonymousOrganizationService.search(new SearchWsRequest.Builder().build());
    List<Organizations.Organization> organizationsList = searchWsResponse.getOrganizationsList();
    assertThat(organizationsList).hasSize(1);
    assertThat(organizationsList.iterator().next().getKey()).isEqualTo(DEFAULT_ORGANIZATION_KEY);
  }

  private void verifySingleSearchResult(Organizations.Organization createdOrganization, String name, String description, String url,
    String avatarUrl) {
    List<Organizations.Organization> organizations = anonymousOrganizationService.search(new SearchWsRequest.Builder().build()).getOrganizationsList();
    assertThat(organizations).hasSize(2);
    Organizations.Organization searchedOrganization = organizations.stream()
      .filter(organization -> !DEFAULT_ORGANIZATION_KEY.equals(organization.getKey()))
      .findFirst()
      .get();
    assertThat(searchedOrganization.getKey()).isEqualTo(createdOrganization.getKey());
    assertThat(searchedOrganization.getName()).isEqualTo(name);
    if (description == null) {
      assertThat(searchedOrganization.hasDescription()).isFalse();
    } else {
      assertThat(searchedOrganization.getDescription()).isEqualTo(description);
    }
    if (url == null) {
      assertThat(searchedOrganization.hasUrl()).isFalse();
    } else {
      assertThat(searchedOrganization.getUrl()).isEqualTo(url);
    }
    if (avatarUrl == null) {
      assertThat(searchedOrganization.hasAvatar()).isFalse();
    } else {
      assertThat(searchedOrganization.getAvatar()).isEqualTo(avatarUrl);
    }
  }
}
