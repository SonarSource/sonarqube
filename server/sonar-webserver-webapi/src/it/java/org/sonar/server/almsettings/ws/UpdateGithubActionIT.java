/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.DevOpsPlatformSettingNewValue;
import org.sonar.db.audit.model.SecretNewValue;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.alm.setting.ALM.GITHUB;

@RunWith(DataProviderRunner.class)
public class UpdateGithubActionIT {

  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(auditPersister);

  private final Encryption encryption = mock(Encryption.class);

  private final WsActionTester ws = new WsActionTester(new UpdateGithubAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), mock(ResourceTypes.class)),
      mock(MultipleAlmFeature.class))));

  private AlmSettingDto almSettingDto;

  @Before
  public void setUp() {
    almSettingDto = db.almSettings().insertGitHubAlmSetting();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
  }

  @Test
  public void update() {
    buildTestRequest().execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://github.enterprise-unicorn.com", "54321", "10987654321", "client_1234", "client_so_secret"));
  }

  private TestRequest buildTestRequest() {
    return ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("privateKey", "10987654321")
      .setParam("clientId", "client_1234")
      .setParam("clientSecret", "client_so_secret");
  }

  @Test
  public void update_url_with_trailing_slash() {
    buildTestRequest().setParam("url", "https://github.enterprise-unicorn.com/").execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://github.enterprise-unicorn.com", "54321", "10987654321", "client_1234", "client_so_secret"));
  }

  @Test
  public void update_with_new_key() {
    buildTestRequest().setParam("newKey", "GitHub Server - Infra Team").execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple("GitHub Server - Infra Team", "https://github.enterprise-unicorn.com", "54321", "10987654321", "client_1234", "client_so_secret"));
  }

  @Test
  public void update_without_client_secret() {
    buildTestRequestWithoutSecrets().execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId,
        s -> s.getDecryptedPrivateKey(encryption), AlmSettingDto::getClientId, s -> s.getDecryptedClientSecret(encryption))
      .containsOnly(tuple(almSettingDto.getKey(), "https://github.enterprise-unicorn.com", "54321",
        "10987654321", "client_1234", almSettingDto.getDecryptedClientSecret(encryption)));
  }


  private TestRequest buildTestRequestWithoutSecrets() {
    return ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com/")
      .setParam("appId", "54321")
      .setParam("clientId", "client_1234")
      .setParam("privateKey", "10987654321");
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    TestRequest request = buildTestRequest()
      .setParam("key", "unknown")
      .setParam("newKey", "GitHub Server - Infra Team");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("DevOps Platform setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_new_key_matches_existing_alm_setting() {
    AlmSettingDto almSetting2 = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = buildTestRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("newKey", almSetting2.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("An DevOps Platform setting with key '%s' already exists", almSetting2.getKey()));
  }

  @Test
  public void update_without_url_changes_does_not_need_private_key() {
    TestRequest request = ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", almSettingDto.getUrl())
      .setParam("appId", "54321")
      .setParam("clientId", "client_1234");

    request.execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(AlmSettingDto::getKey, AlmSettingDto::getUrl, AlmSettingDto::getAppId, AlmSettingDto::getClientId)
      .containsOnly(tuple(almSettingDto.getKey(), almSettingDto.getUrl(), "54321", "client_1234"));
  }

  @Test
  public void fail_when_url_updated_without_private_key() {
    TestRequest request = ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .setParam("url", "https://github.enterprise-unicorn.com")
      .setParam("appId", "54321")
      .setParam("clientId", "client_1234");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Please provide the Private Key to update the URL.");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = buildTestRequest();

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("key", true),
        tuple("newKey", false),
        tuple("url", true),
        tuple("appId", true),
        tuple("privateKey", false),
        tuple("clientId", true),
        tuple("clientSecret", false),
        tuple("webhookSecret", false));
  }

  @Test
  public void update_withWebhookSecret() {
    buildTestRequest().setParam("webhookSecret", "webhook_secret").execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(almSettings -> almSettings.getDecryptedWebhookSecret(encryption))
      .containsOnly("webhook_secret");
  }

  @Test
  public void update_withoutWebhookSecret_shouldNotOverrideExistingValue() {
    buildTestRequest().setParam("webhookSecret", "webhook_secret").execute();

    buildTestRequest().execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(almSettings -> almSettings.getDecryptedWebhookSecret(encryption))
      .containsOnly("webhook_secret");
  }

  @Test
  public void update_withEmptyValue_shouldResetWebhookSecret() {
    buildTestRequest().setParam("webhookSecret", "webhook_secret").execute();

    buildTestRequest().setParam("webhookSecret", "").execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession()))
      .extracting(almSettings -> almSettings.getDecryptedWebhookSecret(encryption))
      .containsOnly((String) null);
  }

  @Test
  public void definition_shouldHaveChangeLog() {
    assertThat(ws.getDef().changelog()).extracting(Change::getVersion, Change::getDescription).containsExactly(
      new Tuple("9.7", "Optional parameter 'webhookSecret' was added"),
      new Tuple("8.7", "Parameter 'privateKey' is no longer required"),
      new Tuple("8.7", "Parameter 'clientSecret' is no longer required")
    );
  }

  @Test
  @UseDataProvider("secretParams")
  public void update_withSecretChange_shouldAuditDevOpsPlatformSecret(String secretParam) {
    buildTestRequestWithoutSecrets().setParam(secretParam, randomAlphanumeric(10)).execute();
    SecretNewValue expected = new SecretNewValue("DevOpsPlatform", GITHUB.getId());
    ArgumentCaptor<SecretNewValue> captor = ArgumentCaptor.forClass(SecretNewValue.class);

    verify(auditPersister).updateDevOpsPlatformSecret(any(DbSession.class), captor.capture());
    assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expected);
  }

  @DataProvider
  public static Object[][] secretParams() {
    return new Object[][] {
      {"webhookSecret"},
      {"clientSecret"}
    };
  }

  @Test
  public void update_withNoSecretChanges_shouldAuditDevOpsPlatformSettings() {
    buildTestRequestWithoutSecrets().execute();
    DevOpsPlatformSettingNewValue expected = new DevOpsPlatformSettingNewValue(almSettingDto.getUuid(), almSettingDto.getKey());
    ArgumentCaptor<DevOpsPlatformSettingNewValue> captor = ArgumentCaptor.forClass(DevOpsPlatformSettingNewValue.class);

    verify(auditPersister).updateDevOpsPlatformSetting(any(DbSession.class), captor.capture());
    assertThat(captor.getValue()).usingRecursiveComparison().comparingOnlyFields("devOpsPlatformSettingUuid", "key").isEqualTo(expected);
  }

}
