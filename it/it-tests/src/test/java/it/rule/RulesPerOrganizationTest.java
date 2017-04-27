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
package it.rule;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.http.HttpMethod;
import it.Category6Suite;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import util.ItUtils;

import static it.Category6Suite.enableOrganizationsSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.deleteOrganizationsIfExists;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newWsClient;

public class RulesPerOrganizationTest {

  private static WsClient adminWsClient;
  private static final String ORGANIZATION_FOO = "foo-org";
  private static final String ORGANIZATION_BAR = "bar-org";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() {
    adminWsClient = newAdminWsClient(orchestrator);
    enableOrganizationsSupport();
    createOrganization(ORGANIZATION_FOO);
    createOrganization(ORGANIZATION_BAR);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    deleteOrganizationsIfExists(orchestrator, ORGANIZATION_FOO);
    deleteOrganizationsIfExists(orchestrator, ORGANIZATION_BAR);
  }

  private static void createOrganization(String organization) {
    adminWsClient.organizations().create(new CreateWsRequest.Builder().setKey(organization).setName(organization).build());
  }

  @Test
  public void should_not_show_tags_of_other_org() {
    updateTag("foo-tag", ORGANIZATION_FOO);
    updateTag("bar-tag", ORGANIZATION_BAR);
    assertThat(showRuleTags(ORGANIZATION_FOO)).containsExactly("foo-tag");
    assertThat(showRuleTags(ORGANIZATION_BAR)).containsExactly("bar-tag");
  }

  @Test
  public void should_not_list_tags_of_other_org() {
    updateTag("foo-tag", ORGANIZATION_FOO);
    updateTag("bar-tag", ORGANIZATION_BAR);
    assertThat(listTags(ORGANIZATION_FOO))
      .contains("foo-tag")
      .doesNotContain("bar-tag");
  }

  @Test
  public void should_not_show_removed_tags() {
    updateTag("foo-tag", ORGANIZATION_FOO);
    assertThat(showRuleTags(ORGANIZATION_FOO)).contains("foo-tag");

    updateTag("", ORGANIZATION_FOO);
    assertThat(showRuleTags(ORGANIZATION_FOO)).isEmpty();
  }

  @Test
  public void should_not_list_removed_tags() {
    updateTag("foo-tag", ORGANIZATION_FOO);
    assertThat(listTags(ORGANIZATION_FOO)).contains("foo-tag");

    updateTag("", ORGANIZATION_FOO);
    assertThat(listTags(ORGANIZATION_FOO)).doesNotContain("foo-tag");
  }

  private List<String> listTags(String organization) {
    String json = orchestrator.getServer().newHttpCall("/api/rules/tags")
      .setParam("organization", organization)
      .execute()
      .getBodyAsString();
    return (List<String>) ItUtils.jsonToMap(json).get("tags");
  }

  private List<String> showRuleTags(String organization) {
    return newWsClient(orchestrator).rules().show(organization, "xoo:OneIssuePerFile")
      .getRule().getTags().getTagsList();
  }

  private void updateTag(String tag, String organization) {
    orchestrator.getServer().newHttpCall("/api/rules/update")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .setParam("organization", organization)
      .setParam("key", "xoo:OneIssuePerFile")
      .setParam("tags", tag)
      .execute();
  }
}
