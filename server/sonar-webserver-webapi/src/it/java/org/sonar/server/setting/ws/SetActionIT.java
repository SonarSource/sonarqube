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
package org.sonar.server.setting.ws;

import com.google.gson.Gson;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.process.ProcessProperties;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.setting.SettingsChangeNotifier;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.auth.github.GitHubSettings.GITHUB_API_URL;
import static org.sonar.auth.github.GitHubSettings.GITHUB_WEB_URL;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.user.UserTesting.newUserDto;

@RunWith(DataProviderRunner.class)
public class SetActionIT {

  private static final Gson GSON = GsonHelper.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final PropertyDbTester propertyDb = new PropertyDbTester(db);
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final I18nRule i18n = new I18nRule();
  private final PropertyDefinitions definitions = new PropertyDefinitions(System2.INSTANCE);
  private final FakeSettingsNotifier settingsChangeNotifier = new FakeSettingsNotifier(dbClient);
  private final SettingsUpdater settingsUpdater = new SettingsUpdater(dbClient, definitions);
  private final SettingValidations validations = new SettingValidations(definitions, dbClient, i18n);
  private final SetAction underTest = new SetAction(definitions, dbClient, userSession, settingsUpdater, settingsChangeNotifier, validations);

  private final WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    // by default test doesn't care about permissions
    userSession.logIn().setSystemAdministrator();
  }

  @DataProvider
  public static Object[][] securityJsonProperties() {
    return new Object[][] {
      {"sonar.security.config.javasecurity"},
      {"sonar.security.config.phpsecurity"},
      {"sonar.security.config.pythonsecurity"},
      {"sonar.security.config.roslyn.sonaranalyzer.security.cs"}
    };
  }

  @Test
  public void empty_204_response() {
    TestResponse result = ws.newRequest()
      .setParam("key", "my.key")
      .setParam("value", "my value")
      .execute();

    assertThat(result.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
  }

  @Test
  public void persist_new_global_setting() {
    callForGlobalSetting("my.key", "my,value");

    assertGlobalSetting("my.key", "my,value");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void update_existing_global_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my value"), null, null, null, null);
    assertGlobalSetting("my.key", "my value");

    callForGlobalSetting("my.key", "my new value");

    assertGlobalSetting("my.key", "my new value");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void persist_new_project_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"), null, null, null, null);
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "my project value", project.getKey());

    assertGlobalSetting("my.key", "my global value");
    assertComponentSetting("my.key", "my project value", project.getUuid());
    assertThat(settingsChangeNotifier.wasCalled).isFalse();
  }

  @Test
  public void persist_new_subportfolio_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"), null, null, null, null);
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto subportfolio = db.components().insertSubportfolio(portfolio);
    logInAsPortfolioAdministrator(db.components().getPortfolioDto(portfolio));

    callForProjectSettingByKey("my.key", "my project value", subportfolio.getKey());

    assertGlobalSetting("my.key", "my global value");
    assertComponentSetting("my.key", "my project value", subportfolio.uuid());
    assertThat(settingsChangeNotifier.wasCalled).isFalse();
  }

  @Test
  public void persist_project_property_with_project_admin_permission() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "my value", project.getKey());

    assertComponentSetting("my.key", "my value", project.getUuid());
  }

  @Test
  public void update_existing_project_setting() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"), null, null,
      null, null);
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    propertyDb.insertProperty(newComponentPropertyDto("my.key", "my project value", project), project.getKey(),
      project.getName(), null, null);
    assertComponentSetting("my.key", "my project value", project.getUuid());
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "my new project value", project.getKey());

    assertComponentSetting("my.key", "my new project value", project.getUuid());
  }

  @Test
  public void persist_several_multi_value_setting() {
    callForMultiValueGlobalSetting("my.key", List.of("first,Value", "second,Value", "third,Value"));

    String expectedValue = "first%2CValue,second%2CValue,third%2CValue";
    assertGlobalSetting("my.key", expectedValue);
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void persist_one_multi_value_setting() {
    callForMultiValueGlobalSetting("my.key", List.of("first,Value"));

    assertGlobalSetting("my.key", "first%2CValue");
  }

  @Test
  public void persist_property_set_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    callForGlobalPropertySet("my.key", List.of(
      GSON.toJson(Map.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(Map.of("firstField", "anotherFirstValue", "secondField", "anotherSecondValue")),
      GSON.toJson(Map.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));

    assertThat(dbClient.propertiesDao().selectGlobalProperties(dbSession)).hasSize(7);
    assertGlobalSetting("my.key", "1,2,3");
    assertGlobalSetting("my.key.1.firstField", "firstValue");
    assertGlobalSetting("my.key.1.secondField", "secondValue");
    assertGlobalSetting("my.key.2.firstField", "anotherFirstValue");
    assertGlobalSetting("my.key.2.secondField", "anotherSecondValue");
    assertGlobalSetting("my.key.3.firstField", "yetFirstValue");
    assertGlobalSetting("my.key.3.secondField", "yetSecondValue");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void update_property_set_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());
    propertyDb.insertProperties(null, null, null, null,
      newGlobalPropertyDto("my.key", "1,2,3,4"),
      newGlobalPropertyDto("my.key.1.firstField", "oldFirstValue"),
      newGlobalPropertyDto("my.key.1.secondField", "oldSecondValue"),
      newGlobalPropertyDto("my.key.2.firstField", "anotherOldFirstValue"),
      newGlobalPropertyDto("my.key.2.secondField", "anotherOldSecondValue"),
      newGlobalPropertyDto("my.key.3.firstField", "oldFirstValue"),
      newGlobalPropertyDto("my.key.3.secondField", "oldSecondValue"),
      newGlobalPropertyDto("my.key.4.firstField", "anotherOldFirstValue"),
      newGlobalPropertyDto("my.key.4.secondField", "anotherOldSecondValue"));

    callForGlobalPropertySet("my.key", List.of(
      GSON.toJson(Map.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(Map.of("firstField", "anotherFirstValue", "secondField", "anotherSecondValue")),
      GSON.toJson(Map.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));

    assertThat(dbClient.propertiesDao().selectGlobalProperties(dbSession)).hasSize(7);
    assertGlobalSetting("my.key", "1,2,3");
    assertGlobalSetting("my.key.1.firstField", "firstValue");
    assertGlobalSetting("my.key.1.secondField", "secondValue");
    assertGlobalSetting("my.key.2.firstField", "anotherFirstValue");
    assertGlobalSetting("my.key.2.secondField", "anotherSecondValue");
    assertGlobalSetting("my.key.3.firstField", "yetFirstValue");
    assertGlobalSetting("my.key.3.secondField", "yetSecondValue");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void update_property_set_on_component_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .onQualifiers(Qualifiers.PROJECT)
      .fields(List.of(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    propertyDb.insertProperties(null, null, null, null,
      newGlobalPropertyDto("my.key", "1"),
      newGlobalPropertyDto("my.key.1.firstField", "oldFirstValue"),
      newGlobalPropertyDto("my.key.1.secondField", "oldSecondValue"));
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto("my.key", "1", project),
      newComponentPropertyDto("my.key.1.firstField", "componentFirstValue", project),
      newComponentPropertyDto("my.key.1.firstField", "componentSecondValue", project));
    logInAsProjectAdministrator(project);

    callForComponentPropertySet("my.key", List.of(
        GSON.toJson(Map.of("firstField", "firstValue", "secondField", "secondValue")),
        GSON.toJson(Map.of("firstField", "anotherFirstValue", "secondField", "anotherSecondValue"))),
      project.getKey());

    assertThat(dbClient.propertiesDao().selectGlobalProperties(dbSession)).hasSize(3);
    assertThat(dbClient.propertiesDao().selectEntityProperties(dbSession, project.getUuid())).hasSize(5);
    assertGlobalSetting("my.key", "1");
    assertGlobalSetting("my.key.1.firstField", "oldFirstValue");
    assertGlobalSetting("my.key.1.secondField", "oldSecondValue");
    String projectUuid = project.getUuid();
    assertComponentSetting("my.key", "1,2", projectUuid);
    assertComponentSetting("my.key.1.firstField", "firstValue", projectUuid);
    assertComponentSetting("my.key.1.secondField", "secondValue", projectUuid);
    assertComponentSetting("my.key.2.firstField", "anotherFirstValue", projectUuid);
    assertComponentSetting("my.key.2.secondField", "anotherSecondValue", projectUuid);
    assertThat(settingsChangeNotifier.wasCalled).isFalse();
  }

  @Test
  public void persist_multi_value_with_type_logIn() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.USER_LOGIN)
      .defaultValue("default")
      .multiValues(true)
      .build());
    db.users().insertUser(newUserDto().setLogin("login.1"));
    db.users().insertUser(newUserDto().setLogin("login.2"));

    callForMultiValueGlobalSetting("my.key", List.of("login.1", "login.2"));

    assertGlobalSetting("my.key", "login.1,login.2");
  }

  @Test
  public void user_setting_is_not_updated() {
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my user value").setUserUuid("42"), null, null,
      null, "user_login");
    propertyDb.insertProperty(newGlobalPropertyDto("my.key", "my global value"), null, null, null, null);

    callForGlobalSetting("my.key", "my new global value");

    assertGlobalSetting("my.key", "my new global value");
    assertUserSetting("my.key", "my user value", "42");
  }

  @Test
  public void persist_global_property_with_deprecated_key() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .deprecatedKey("my.old.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.STRING)
      .defaultValue("default")
      .build());

    callForGlobalSetting("my.old.key", "My Value");

    assertGlobalSetting("my.key", "My Value");
  }

  @Test
  public void persist_JSON_property() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    callForGlobalSetting("my.key", "{\"test\":\"value\"}");

    assertGlobalSetting("my.key", "{\"test\":\"value\"}");
  }

  @Test
  public void fail_if_JSON_invalid_for_JSON_property() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "{\"test\":\"value\""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided JSON is invalid");

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "{\"test\":\"value\",}"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided JSON is invalid");

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "{\"test\":[\"value\",]}"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided JSON is invalid");
  }

  @Test
  @UseDataProvider("securityJsonProperties")
  public void successfully_validate_json_schema(String securityPropertyKey) {
    String security_custom_config = """
      {
        "S3649": {
          "sources": [
            {
              "methodId": "My\\\\Namespace\\\\ClassName\\\\ServerRequest::getQuery"
            }
          ],
          "sanitizers": [
            {
              "methodId": "str_replace",        "args": [
                 0
               ]
            }
          ],
          "validators": [
            {
              "methodId": "is_valid",        "args": [
                 1
               ]
            }
          ],
          "sinks": [
            {
              "methodId": "mysql_query",
              "args": [1]
            }
          ]
        }
      }""";
    definitions.addComponent(PropertyDefinition
      .builder(securityPropertyKey)
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    callForGlobalSetting(securityPropertyKey, security_custom_config);

    assertGlobalSetting(securityPropertyKey, security_custom_config);
  }

  @Test
  @UseDataProvider("securityJsonProperties")
  public void fail_json_schema_validation_when_property_has_incorrect_type(String securityPropertyKey) {
    String security_custom_config = """
      {
        "S3649": {
          "sources": [
            {
              "methodId": "My\\\\Namespace\\\\ClassName\\\\ServerRequest::getQuery"
            }
          ],
          "sinks": [
            {
              "methodId": 12345,
              "args": [1]
            }
          ]
        }
      }""";
    definitions.addComponent(PropertyDefinition
      .builder(securityPropertyKey)
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting(securityPropertyKey, security_custom_config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("expected type: string, actual: integer at line 10, character 21, pointer: #/S3649/sinks/0/methodId");
  }

  @Test
  @UseDataProvider("securityJsonProperties")
  public void fail_json_schema_validation_when_sanitizers_have_no_args(String securityPropertyKey) {
    String security_custom_config = """
      {
        "S3649": {
          "sources": [
            {
              "methodId": "My\\\\Namespace\\\\ClassName\\\\ServerRequest::getQuery"
            }
          ],
          "sanitizers": [
             {
               "methodId": "SomeSanitizer"
             }
          ]
        }
      }""";
    definitions.addComponent(PropertyDefinition
      .builder(securityPropertyKey)
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting(securityPropertyKey, security_custom_config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("required properties are missing: args at line 9, character 8, pointer: #/S3649/sanitizers/0");
  }

  @Test
  @UseDataProvider("securityJsonProperties")
  public void fail_json_schema_validation_when_validators_have_empty_args_array(String securityPropertyKey) {
    String security_custom_config = """
      {
        "S3649": {
          "sources": [
            {
              "methodId": "My\\\\Namespace\\\\ClassName\\\\ServerRequest::getQuery"
            }
          ],
          "validators": [
             {
               "methodId": "SomeValidator",
               "args": []
             }
          ]
        }
      }""";
    definitions.addComponent(PropertyDefinition
      .builder(securityPropertyKey)
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting(securityPropertyKey, security_custom_config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("expected minimum items: 1, found only 0 at line 11, character 18, pointer: #/S3649/validators/0/args");
  }

  @Test
  @UseDataProvider("securityJsonProperties")
  public void fail_json_schema_validation_when_property_has_unknown_attribute(String securityPropertyKey) {
    String security_custom_config = """
      {
        "S3649": {
          "sources": [
            {
              "methodId": "My\\\\Namespace\\\\ClassName\\\\ServerRequest::getQuery"
            }
          ],
          "unknown": [
            {
              "methodId": 12345,
              "args": [1]
            }
          ]
        }
      }""";
    definitions.addComponent(PropertyDefinition
      .builder(securityPropertyKey)
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.JSON)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting(securityPropertyKey, security_custom_config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("false schema always fails at line 8, character 16, pointer: #/S3649/unknown");
  }

  @Test
  public void persist_global_setting_with_non_ascii_characters() {
    callForGlobalSetting("my.key", "ﬁ±∞…");

    assertGlobalSetting("my.key", "ﬁ±∞…");
    assertThat(settingsChangeNotifier.wasCalled).isTrue();
  }

  @Test
  public void fail_when_no_key() {
    assertThatThrownBy(() -> callForGlobalSetting(null, "my value"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_empty_key_value() {
    assertThatThrownBy(() -> callForGlobalSetting("  ", "my value"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'key' parameter is missing");
  }

  @Test
  public void fail_when_no_value() {
    assertThatThrownBy(() -> callForGlobalSetting("my.key", null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Either 'value', 'values' or 'fieldValues' must be provided");
  }

  @Test
  public void fail_when_empty_value() {
    assertThatThrownBy(() -> callForGlobalSetting("my.key", ""))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A non empty value must be provided");
  }

  @Test
  public void fail_when_one_empty_value_on_multi_value() {
    List<String> values = List.of("oneValue", "  ", "anotherValue");
    assertThatThrownBy(() -> callForMultiValueGlobalSetting("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A non empty value must be provided");
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "my value"))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_when_data_and_type_do_not_match() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.INTEGER)
      .defaultValue("default")
      .build());
    i18n.put("property.error.notInteger", "Not an integer error message");

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "My Value"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Not an integer error message");
  }

  @Test
  public void fail_when_data_and_login_type_with_invalid_logIn() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.USER_LOGIN)
      .defaultValue("default")
      .multiValues(true)
      .build());
    db.users().insertUser(newUserDto().setLogin("login.1"));
    db.users().insertUser(newUserDto().setLogin("login.2").setActive(false));

    List<String> values = List.of("login.1", "login.2");
    assertThatThrownBy(() -> callForMultiValueGlobalSetting("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Error when validating login setting with key 'my.key' and values [login.1, login.2]. A value is not a valid login.");
  }

  @Test
  public void fail_when_data_and_type_do_not_match_with_unknown_error_key() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.INTEGER)
      .defaultValue("default")
      .build());

    List<String> values = List.of("My Value", "My Other Value");
    assertThatThrownBy(() -> callForMultiValueGlobalSetting("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Error when validating setting with key 'my.key' and value [My Value, My Other Value]");
  }

  @Test
  public void fail_when_global_with_property_only_on_projects() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.INTEGER)
      .defaultValue("default")
      .onlyOnQualifiers(Qualifiers.PROJECT)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "42"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'my.key' cannot be global");
  }

  @Test
  public void fail_when_view_property_when_on_projects_only() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.STRING)
      .defaultValue("default")
      .onQualifiers(Qualifiers.PROJECT)
      .build());
    ComponentDto view = db.components().insertPublicPortfolio();
    i18n.put("qualifier." + Qualifiers.VIEW, "View");

    assertThatThrownBy(() -> {
      logInAsPortfolioAdministrator(db.components().getPortfolioDto(view));
      callForProjectSettingByKey("my.key", "My Value", view.getKey());
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'my.key' cannot be set on a View");
  }

  @Test
  public void fail_when_property_with_definition_when_component_qualifier_does_not_match() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.STRING)
      .defaultValue("default")
      .onQualifiers(Qualifiers.PROJECT)
      .build());
    i18n.put("qualifier." + portfolio.getQualifier(), "CptLabel");
    logInAsPortfolioAdministrator(portfolio);

    assertThatThrownBy(() -> callForProjectSettingByKey("my.key", "My Value", portfolio.getKey()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'my.key' cannot be set on a CptLabel");
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_project_component() {
    ProjectDto project = randomPublicOrPrivateProject().getProjectDto();
    succeedForPropertyWithoutDefinitionAndValidComponent(project);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_directory_component() {
    ProjectData projectData = randomPublicOrPrivateProject();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(projectData.getMainBranchComponent(), "A/B"));
    failForPropertyWithoutDefinitionOnUnsupportedComponent(projectData.getProjectDto(), directory);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_file_component() {
    ProjectData projectData = randomPublicOrPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(projectData.getMainBranchComponent()));
    failForPropertyWithoutDefinitionOnUnsupportedComponent(projectData.getProjectDto(), file);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_view_component() {
    PortfolioDto view = db.components().insertPrivatePortfolioDto();
    succeedForPropertyWithoutDefinitionAndValidComponent(view);
  }

  @Test
  public void succeed_for_property_without_definition_when_set_on_subview_component() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubPortfolio(view));
    failForPropertyWithoutDefinitionOnUnsupportedComponent(db.components().getPortfolioDto(view), subview);
  }

  @Test
  public void fail_for_property_without_definition_when_set_on_projectCopy_component() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto projectCopy = db.components().insertComponent(ComponentTesting.newProjectCopy("a", db.components().insertPrivateProject().getMainBranchComponent(), view));

    failForPropertyWithoutDefinitionOnUnsupportedComponent(db.components().getPortfolioDto(view), projectCopy);
  }

  private void succeedForPropertyWithoutDefinitionAndValidComponent(ProjectDto project) {
    logInAsProjectAdministrator(project);

    callForProjectSettingByKey("my.key", "My Value", project.getKey());

    assertComponentSetting("my.key", "My Value", project.getUuid());
  }

  private void succeedForPropertyWithoutDefinitionAndValidComponent(PortfolioDto portfolioDto) {
    logInAsPortfolioAdministrator(portfolioDto);

    callForProjectSettingByKey("my.key", "My Value", portfolioDto.getKey());

    assertComponentSetting("my.key", "My Value", portfolioDto.getUuid());
  }

  private void failForPropertyWithoutDefinitionOnUnsupportedComponent(ProjectDto project, ComponentDto component) {
    i18n.put("qualifier." + component.qualifier(), "QualifierLabel");
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> callForProjectSettingByKey("my.key", "My Value", component.getKey()))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component key '%s' not found", component.getKey()));
  }

  private void failForPropertyWithoutDefinitionOnUnsupportedComponent(PortfolioDto portfolio, ComponentDto component) {
    i18n.put("qualifier." + component.qualifier(), "QualifierLabel");
    logInAsPortfolioAdministrator(portfolio);

    assertThatThrownBy(() -> callForProjectSettingByKey("my.key", "My Value", component.getKey()))
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component key '%s' not found", component.getKey()));
  }

  @Test
  public void fail_when_single_and_multi_value_provided() {
    List<String> value = List.of("Another Value");
    assertThatThrownBy(() -> call("my.key", "My Value", value, null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Either 'value', 'values' or 'fieldValues' must be provided");
  }

  @Test
  public void fail_when_multi_definition_and_single_value_provided() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .type(PropertyType.STRING)
      .multiValues(true)
      .build());

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "My Value"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Parameter 'value' must be used for single value setting. Parameter 'values' must be used for multi value setting.");
  }

  @Test
  public void fail_when_single_definition_and_multi_value_provided() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .type(PropertyType.STRING)
      .multiValues(false)
      .build());

    assertThatThrownBy(() -> callForMultiValueGlobalSetting("my.key", List.of("My Value")))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Parameter 'value' must be used for single value setting. Parameter 'values' must be used for multi value setting.");
  }

  @Test
  public void fail_when_empty_values_on_one_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", List.of(
      GSON.toJson(Map.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(Map.of("firstField", "", "secondField", "")),
      GSON.toJson(Map.of("firstField", "yetFirstValue", "secondField", "yetSecondValue")))))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A non empty value must be provided");
  }

  @Test
  public void do_not_fail_when_only_one_empty_value_on_one_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("firstField")
          .name("First Field")
          .type(PropertyType.STRING)
          .build(),
        PropertyFieldDefinition.build("secondField")
          .name("Second Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    callForGlobalPropertySet("my.key", List.of(
      GSON.toJson(Map.of("firstField", "firstValue", "secondField", "secondValue")),
      GSON.toJson(Map.of("firstField", "anotherFirstValue", "secondField", "")),
      GSON.toJson(Map.of("firstField", "yetFirstValue", "secondField", "yetSecondValue"))));

    assertGlobalSetting("my.key", "1,2,3");
  }

  @Test
  public void fail_when_property_set_setting_is_not_defined() {
    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", singletonList("{\"field\":\"value\"}")))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'my.key' is undefined");
  }

  @Test
  public void fail_when_property_set_with_unknown_field() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    List<String> values = List.of(GSON.toJson(Map.of("field", "value", "unknownField", "anotherValue")));
    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Unknown field key 'unknownField' for setting 'my.key'");
  }

  @Test
  public void fail_when_property_set_has_field_with_incorrect_type() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.INTEGER)
          .build()))
      .build());

    List<String> values = List.of(GSON.toJson(Map.of("field", "notAnInt")));
    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Error when validating setting with key 'my.key'. Field 'field' has incorrect value 'notAnInt'.");
  }

  @Test
  public void fail_when_property_set_has_a_null_field_value() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    List<String> values = List.of("{\"field\": null}");
    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("A non empty value must be provided");
  }

  @Test
  public void fail_when_property_set_with_invalid_json() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    List<String> values = List.of("incorrectJson:incorrectJson");
    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("JSON 'incorrectJson:incorrectJson' does not respect expected format for setting 'my.key'. " +
        "Ex: {\"field1\":\"value1\", \"field2\":\"value2\"}");
  }

  @Test
  public void fail_when_property_set_with_json_of_the_wrong_format() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(
        PropertyFieldDefinition.build("field")
          .name("Field")
          .type(PropertyType.STRING)
          .build()))
      .build());

    List<String> values = List.of("[{\"field\":\"v1\"}, {\"field\":\"v2\"}]");
    assertThatThrownBy(() -> callForGlobalPropertySet("my.key", values))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("JSON '[{\"field\":\"v1\"}, {\"field\":\"v2\"}]' does not respect expected format for setting 'my.key'. " +
        "Ex: {\"field1\":\"value1\", \"field2\":\"value2\"}");
  }

  @Test
  public void fail_when_property_set_on_component_of_global_setting() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .category("cat")
      .subCategory("subCat")
      .type(PropertyType.PROPERTY_SET)
      .defaultValue("default")
      .fields(List.of(PropertyFieldDefinition.build("firstField").name("First Field").type(PropertyType.STRING).build()))
      .build());
    i18n.put("qualifier." + Qualifiers.PROJECT, "Project");
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);

    List<String> values = List.of(GSON.toJson(Map.of("firstField", "firstValue")));
    assertThatThrownBy(() -> callForComponentPropertySet("my.key", values, project.getKey()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Setting 'my.key' cannot be set on a Project");
  }

  @Test
  public void fail_when_component_not_found() {
    TestRequest testRequest = ws.newRequest()
      .setParam("key", "foo")
      .setParam("value", "2")
      .setParam("component", "unknown");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown' not found");
  }

  @Test
  public void fail_when_setting_key_is_defined_in_sonar_properties() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    logInAsProjectAdministrator(project);
    String settingKey = ProcessProperties.Property.JDBC_URL.getKey();

    TestRequest testRequest = ws.newRequest()
      .setParam("key", settingKey)
      .setParam("value", "any value")
      .setParam("component", project.getKey());
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Setting '%s' can only be used in sonar.properties", settingKey));
  }

  @DataProvider
  public static Object[][] forbiddenProperties() {
    return new Object[][] {
      {GITLAB_AUTH_URL},
      {GITHUB_API_URL},
      {GITHUB_WEB_URL},
    };
  }

  @Test
  @UseDataProvider("forbiddenProperties")
  public void fail_when_setting_key_is_forbidden(String property) {
    TestRequest testRequest = ws.newRequest()
      .setParam("key", property)
      .setParam("value", "value");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("For security reasons, the key '%s' cannot be updated using this webservice. Please use the API v2", property);
  }

  @Test
  public void fail_when_setting_key_is_forbidden() {
    TestRequest testRequest = ws.newRequest()
      .setParam("key", "sonar.auth.gitlab.url")
      .setParam("value", "http://malicious.url");
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("For security reasons, the key 'sonar.auth.gitlab.url' cannot be updated using this webservice. Please use the API v2");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("set");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.since()).isEqualTo("6.1");
    assertThat(definition.params()).extracting(Param::key)
      .containsOnly("key", "value", "values", "fieldValues", "component");
  }

  @Test
  public void call_whenEmailPropertyValid_shouldSucceed() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .type(PropertyType.EMAIL)
      .build());

    callForGlobalSetting("my.key", "test@sonarsource.com");
    assertGlobalSetting("my.key", "test@sonarsource.com");
  }

  @Test
  public void call_whenEmailPropertyInvalid_shouldFail() {
    definitions.addComponent(PropertyDefinition
      .builder("my.key")
      .name("foo")
      .description("desc")
      .type(PropertyType.EMAIL)
      .build());
    i18n.put("property.error.notEmail", "Not a valid email address");

    assertThatThrownBy(() -> callForGlobalSetting("my.key", "test1@sonarsource.com,test2@sonarsource.com"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Not a valid email address");
  }

  private void assertGlobalSetting(String key, String value) {
    PropertyDto result = dbClient.propertiesDao().selectGlobalProperty(key);

    assertThat(result)
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getEntityUuid)
      .containsExactly(key, value, null);
  }

  private void assertUserSetting(String key, String value, String userUuid) {
    List<PropertyDto> result = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setKey(key).setUserUuid(userUuid).build(), dbSession);

    assertThat(result).hasSize(1)
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getUserUuid)
      .containsExactly(tuple(key, value, userUuid));
  }

  private void assertComponentSetting(String key, String value, String entityUuid) {
    PropertyDto result = dbClient.propertiesDao().selectProjectProperty(db.getSession(), entityUuid, key);

    assertThat(result)
      .isNotNull()
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getEntityUuid)
      .containsExactly(key, value, entityUuid);
  }

  private void callForGlobalSetting(@Nullable String key, @Nullable String value) {
    call(key, value, null, null, null);
  }

  private void callForMultiValueGlobalSetting(@Nullable String key, @Nullable List<String> values) {
    call(key, null, values, null, null);
  }

  private void callForGlobalPropertySet(@Nullable String key, @Nullable List<String> fieldValues) {
    call(key, null, null, fieldValues, null);
  }

  private void callForComponentPropertySet(@Nullable String key, @Nullable List<String> fieldValues, @Nullable String componentKey) {
    call(key, null, null, fieldValues, componentKey);
  }

  private void callForProjectSettingByKey(@Nullable String key, @Nullable String value, @Nullable String componentKey) {
    call(key, value, null, null, componentKey);
  }

  private void call(@Nullable String key, @Nullable String value, @Nullable List<String> values, @Nullable List<String> fieldValues, @Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam("key", key);
    }
    if (value != null) {
      request.setParam("value", value);
    }
    if (values != null) {
      request.setMultiParam("values", values);
    }
    if (fieldValues != null) {
      request.setMultiParam("fieldValues", fieldValues);
    }
    if (componentKey != null) {
      request.setParam("component", componentKey);
    }
    request.execute();
  }

  private static class FakeSettingsNotifier extends SettingsChangeNotifier {

    private final DbClient dbClient;
    private boolean wasCalled = false;

    private FakeSettingsNotifier(DbClient dbClient) {
      this.dbClient = dbClient;
    }

    @Override
    public void onGlobalPropertyChange(String key, @Nullable String value) {
      wasCalled = true;
      PropertyDto property = dbClient.propertiesDao().selectGlobalProperty(key);

      assertThat(property.getValue()).isEqualTo(value);
    }
  }

  private void logInAsPortfolioAdministrator(PortfolioDto portfolio) {
    userSession.logIn().addPortfolioPermission(UserRole.ADMIN, portfolio);
  }

  private void logInAsProjectAdministrator(ProjectDto project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
  }

  private ProjectData randomPublicOrPrivateProject() {
    return ThreadLocalRandom.current().nextBoolean() ? db.components().insertPrivateProject() : db.components().insertPublicProject();
  }
}
