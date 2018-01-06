/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.rule;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Rules.ShowResponse;
import org.sonarqube.ws.client.rules.ShowRequest;
import org.sonarqube.ws.client.rules.UpdateRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesMarkdownTest {

  private static final String RULE_HAS_TAG = "xoo:HasTag";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void use_markdown_in_description() {
    Organization organization = tester.organizations().generate();

    tester.wsClient().rules().update(createUpdateRequest(organization, "*my custom note*"));

    ShowResponse showResponse = tester.wsClient().rules().show(createShowRequest(organization));
    assertThat(showResponse.getRule().getMdNote()).isEqualTo("*my custom note*");
    assertThat(showResponse.getRule().getHtmlNote()).isEqualTo("<strong>my custom note</strong>");
  }

  @Test
  public void use_markdown_for_short_string() {
    Organization organization = tester.organizations().generate();
    String markdownNote = "*A*";
    String expected = "<strong>A</strong>";

    tester.wsClient().rules().update(createUpdateRequest(organization, markdownNote));

    ShowResponse showResponse = tester.wsClient().rules().show(createShowRequest(organization));
    assertThat(showResponse.getRule().getMdNote()).isEqualTo(markdownNote);
    assertThat(showResponse.getRule().getHtmlNote()).isEqualTo(expected);
  }

  private static ShowRequest createShowRequest(Organization organization) {
    return new ShowRequest().setKey(RULE_HAS_TAG).setOrganization(organization.getKey());
  }

  private static UpdateRequest createUpdateRequest(Organization organization, String markdownNote) {
    return new UpdateRequest().setKey(RULE_HAS_TAG).setOrganization(organization.getKey()).setMarkdownNote(markdownNote);
  }

}
