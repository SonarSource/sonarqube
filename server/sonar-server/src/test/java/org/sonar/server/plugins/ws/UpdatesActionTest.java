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
package org.sonar.server.plugins.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.INCOMPATIBLE;

public class UpdatesActionTest extends AbstractUpdateCenterBasedPluginsWsActionTest {
  private static final Plugin JAVA_PLUGIN = Plugin.factory("java")
    .setName("Java")
    .setDescription("SonarQube rule engine.");
  private static final Plugin ABAP_PLUGIN = Plugin.factory("abap")
    .setName("ABAP")
    .setCategory("Languages")
    .setDescription("Enable analysis and reporting on ABAP projects")
    .setLicense("Commercial")
    .setOrganization("SonarSource")
    .setOrganizationUrl("http://www.sonarsource.com")
    .setTermsConditionsUrl("http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf");
  private static final Release ABAP_31 = release(ABAP_PLUGIN, "3.1")
    .setDate(DateUtils.parseDate("2014-12-21"))
    .setDescription("New rules, several improvements")
    .setDownloadUrl("http://dist.sonarsource.com/abap/download/sonar-abap-plugin-3.1.jar")
    .setChangelogUrl("http://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10054&version=10552");
  private static final Release ABAP_32 = release(ABAP_PLUGIN, "3.2")
    .setDate(DateUtils.parseDate("2015-03-10"))
    .setDescription("14 new rules, most of them designed to detect potential performance hotspots.")
    .setDownloadUrl("http://dist.sonarsource.com/abap/download/sonar-abap-plugin-3.2.jar")
    .setChangelogUrl("http://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10054&version=10575");
  private static final Plugin ANDROID_PLUGIN = Plugin.factory("android")
    .setName("Android")
    .setCategory("Languages")
    .setDescription("Import Android Lint reports.")
    .setLicense("GNU LGPL 3")
    .setOrganization("SonarSource and Jerome Van Der Linden, Stephane Nicolas, Florian Roncari, Thomas Bores")
    .setOrganizationUrl("http://www.sonarsource.com");
  private static final Release ANDROID_10 = release(ANDROID_PLUGIN, "1.0")
    .setDate(DateUtils.parseDate("2014-03-31"))
    .setDescription("Makes the plugin compatible with multi-language analysis introduced in SonarQube 4.2 and adds support of Emma 2.0 reports")
    .setDownloadUrl("http://repository.codehaus.org/org/codehaus/sonar-plugins/android/sonar-android-plugin/1.0/sonar-android-plugin-1.0.jar")
    .setChangelogUrl("http://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=13235&version=20187")

    .addOutgoingDependency(release(JAVA_PLUGIN, "1.0"));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpdatesAction underTest = new UpdatesAction(userSession, updateCenterFactory,
    new PluginWSCommons(), new PluginUpdateAggregator());

  @Test
  public void action_updatable_is_defined() {
    logInAsSystemAdministrator();

    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("updates");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() throws Exception {
    expectedException.expect(ForbiddenException.class);

    underTest.handle(request, response);
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() throws Exception {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    underTest.handle(request, response);
  }

  @Test
  public void empty_array_is_returned_when_there_is_no_plugin_available() throws Exception {
    logInAsSystemAdministrator();

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void verify_response_against_example() throws Exception {
    logInAsSystemAdministrator();
    when(updateCenter.findPluginUpdates()).thenReturn(of(
      pluginUpdate(ABAP_32, COMPATIBLE),
      pluginUpdate(ABAP_31, INCOMPATIBLE),
      pluginUpdate(ANDROID_10, COMPATIBLE)));

    underTest.handle(request, response);

    assertJson(response.outputAsString())
      .isSimilarTo(new WsActionTester(underTest).getDef().responseExampleAsString());
  }

  @Test
  public void status_COMPATIBLE_is_displayed_COMPATIBLE_in_JSON() throws Exception {
    logInAsSystemAdministrator();
    when(updateCenter.findPluginUpdates()).thenReturn(of(
      pluginUpdate(release(PLUGIN_1, "1.0.0"), COMPATIBLE)));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"plugins\": [" +
        "   {" +
        "      \"updates\": [" +
        "        {" +
        "          \"status\": \"COMPATIBLE\"" +
        "        }" +
        "      ]" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void plugins_are_sorted_by_name_and_made_unique() throws Exception {
    logInAsSystemAdministrator();
    when(updateCenter.findPluginUpdates()).thenReturn(of(
      pluginUpdate("key2", "name2"),
      pluginUpdate("key2", "name2"),
      pluginUpdate("key0", "name0"),
      pluginUpdate("key1", "name1")));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"plugins\": [" +
        "    {" +
        "      \"key\": \"key0\"," +
        "      \"name\": \"name0\"" +
        "    }," +
        "    {" +
        "      \"key\": \"key1\"," +
        "      \"name\": \"name1\"" +
        "    }," +
        "    {" +
        "      \"key\": \"key2\"," +
        "      \"name\": \"name2\"" +
        "    }," +
        "  ]" +
        "}");
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
