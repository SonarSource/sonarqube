/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.almsettings.ws;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.almsettings.ws.GithubManifestStateStore.PendingManifest;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateGithubFromManifestActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);
  private final Server server = mock(Server.class);
  private final GithubManifestStateStore stateStore = new GithubManifestStateStore(System2.INSTANCE);
  private final AlmSettingsSupport almSettingsSupport = new AlmSettingsSupport(db.getDbClient(), userSession,
    new ComponentFinder(db.getDbClient(), mock(ComponentTypes.class)), multipleAlmFeature);
  private final GithubAppManifestGenerator manifestGenerator = new GithubAppManifestGenerator(server);

  private final WsActionTester ws = new WsActionTester(
    new CreateGithubFromManifestAction(db.getDbClient(), userSession, almSettingsSupport, manifestGenerator, stateStore));

  @Before
  public void setUp() {
    when(multipleAlmFeature.isAvailable()).thenReturn(true);
    when(server.getPublicRootUrl()).thenReturn("https://sonarqube.example.com");
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("create_github_from_manifest");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("key", "organization", "name", "devops", "auth");
  }

  @Test
  public void handle_whenNotSystemAdministrator_throwsForbidden() {
    userSession.logIn(db.users().insertUser());

    TestRequest request = ws.newRequest().setParam("devops", "true").setParam("key", "my-key");

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_whenNeitherDevopsNorAuth_throwsBadRequest() {
    TestRequest request = ws.newRequest().setParam("devops", "false").setParam("auth", "false");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("At least one of");
  }

  @Test
  public void handle_whenDevopsWithoutKey_throwsBadRequest() {
    TestRequest request = ws.newRequest().setParam("devops", "true");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("is required to create the DevOps Platform integration");
  }

  @Test
  public void handle_whenKeyAlreadyExists_throws() {
    db.almSettings().insertGitHubAlmSetting(a -> a.setKey("existing-key"));

    TestRequest request = ws.newRequest().setParam("devops", "true").setParam("key", "existing-key");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("already exists");
  }

  @Test
  public void handle_whenBaseUrlNotConfigured_throwsBadRequest() {
    when(server.getPublicRootUrl()).thenReturn("");

    TestRequest request = ws.newRequest().setParam("devops", "true").setParam("key", "my-key");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("server base URL must be configured");
  }

  @Test
  public void handle_devops_returnsManifestAndStoresState() {
    String response = ws.newRequest()
      .setParam("devops", "true")
      .setParam("key", "my-key")
      .setParam("organization", "my-org")
      .execute().getInput();

    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
    assertThat(json.get("githubAppUrl").getAsString()).isEqualTo("https://github.com/organizations/my-org/settings/apps/new");
    assertThat(json.get("manifest").getAsString()).contains("\"name\":\"SonarQube\"");
    String state = json.get("state").getAsString();
    assertThat(state).isNotBlank();

    Optional<PendingManifest> pending = stateStore.consume(state);
    assertThat(pending).isPresent();
    assertThat(pending.get().settingKey()).isEqualTo("my-key");
    assertThat(pending.get().organization()).isEqualTo("my-org");
    assertThat(pending.get().setupDevops()).isTrue();
    assertThat(pending.get().setupAuth()).isFalse();
  }

  @Test
  public void handle_authOnly_doesNotRequireKey_andUsesAuthSetupPath() {
    String response = ws.newRequest()
      .setParam("devops", "false")
      .setParam("auth", "true")
      .execute().getInput();

    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
    assertThat(json.get("githubAppUrl").getAsString()).isEqualTo("https://github.com/settings/apps/new");
    JsonObject manifest = JsonParser.parseString(json.get("manifest").getAsString()).getAsJsonObject();
    assertThat(manifest.get("setup_url").getAsString()).endsWith("/admin/settings?category=authentication&tab=github");
    assertThat(json.get("state").getAsString()).isNotBlank();
  }
}
