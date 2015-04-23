/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Sonar;
import org.sonar.updatecenter.common.SonarUpdate;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class UpgradesSystemWsActionTest {
  private static final String DUMMY_CONTROLLER_KEY = "dummy";
  private static final String JSON_EMPTY_UPGRADE_LIST =
    "{" +
      "  \"upgrades\":" + "[]" +
      "}";

  private UpdateCenterMatrixFactory updateCenterFactory = mock(UpdateCenterMatrixFactory.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);
  private UpgradesSystemWsAction underTest = new UpgradesSystemWsAction(updateCenterFactory);

  private Request request = mock(Request.class);
  private WsTester.TestResponse response = new WsTester.TestResponse();

  @Before
  public void wireMocks() throws Exception {
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(updateCenter);
    when(updateCenter.getDate()).thenReturn(DateUtils.parseDateTime("2015-04-24T16:08:36+0200"));
  }

  @Test
  public void action_updates_is_defined() throws Exception {
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("upgrades");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();

    assertThat(action.params()).isEmpty();
  }

  @Test
  public void empty_array_is_returned_when_there_is_no_upgrade_available() throws Exception {
    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(JSON_EMPTY_UPGRADE_LIST);
  }

  @Test
  public void verify_JSON_response_against_example() throws Exception {
    SonarUpdate sonarUpdate = createSonar_51_update();
    when(updateCenter.findSonarUpdates()).thenReturn(of(sonarUpdate));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true)
      .isSimilarTo(getClass().getResource("example-updates_plugins.json"));
  }

  private static SonarUpdate createSonar_51_update() {
    Plugin brandingPlugin = new Plugin("branding")
      .setCategory("Integration")
      .setName("Branding")
      .setDescription("Allows to add your own logo to the SonarQube UI.")
      .setHomepageUrl("http://docs.codehaus.org/display/SONAR/Branding+Plugin")
      .setLicense("GNU LGPL 3")
      .setOrganization("SonarSource")
      .setOrganizationUrl("http://www.sonarsource.com")
      .setIssueTrackerUrl("http://jira.codehaus.org/browse/SONARPLUGINS/component/14663")
      .setSourcesUrl("https://github.com/SonarCommunity/sonar-branding");
    Plugin viewsPlugin = new Plugin("views")
      .setName("Views")
      .setCategory("Governance")
      .setDescription("Create aggregation trees to group projects. Projects can for instance be grouped by applications,   applications by team, teams by department.")
      .setHomepageUrl("http://redirect.sonarsource.com/plugins/views.html")
      .setLicense("Commercial")
      .setOrganization("SonarSource")
      .setOrganizationUrl("http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf")
      .setTermsConditionsUrl("http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf")
      .setIssueTrackerUrl("http://jira.sonarsource.com/browse/VIEWS");

    SonarUpdate sonarUpdate = new SonarUpdate(
      new Release(new Sonar(), Version.create("5.1"))
        .setDate(DateUtils.parseDate("2015-04-02"))
        .setDescription("New overall layout, merge Issues Drilldown [...]")
        .setDownloadUrl("http://dist.sonar.codehaus.org/sonarqube-5.1.zip")
        .setChangelogUrl("http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=11694&version=20666")
      );

    sonarUpdate.addIncompatiblePlugin(brandingPlugin);
    sonarUpdate.addPluginToUpgrade(new Release(viewsPlugin, Version.create("2.8")));

    return sonarUpdate;
  }
}
