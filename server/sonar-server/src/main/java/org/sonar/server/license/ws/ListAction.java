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
package org.sonar.server.license.ws;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.api.config.License;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.setting.ws.Setting;
import org.sonar.server.setting.ws.SettingsFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsAction;
import org.sonarqube.ws.Licenses;
import org.sonarqube.ws.Licenses.ListWsResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.license.LicensesWsParameters.ACTION_LIST;

public class ListAction implements WsAction {

  private static final String ALL_SERVERS_VALUE = "*";

  private final UserSession userSession;
  private final PropertyDefinitions definitions;
  private final DbClient dbClient;
  private final SettingsFinder settingsFinder;

  public ListAction(UserSession userSession, PropertyDefinitions definitions, DbClient dbClient, SettingsFinder settingsFinder) {
    this.userSession = userSession;
    this.definitions = definitions;
    this.dbClient = dbClient;
    this.settingsFinder = settingsFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION_LIST)
      .setDescription("List licenses settings.<br>" +
        "Requires 'Administer System' permission")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setSince("6.1")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    try (DbSession dbSession = dbClient.openSession(true)) {
      writeProtobuf(doHandle(dbSession), request, response);
    }
  }

  private ListWsResponse doHandle(DbSession dbSession) {
    Map<String, PropertyDefinition> licenseDefinitionsByKeys = definitions.getAll().stream()
      .filter(definition -> LICENSE.equals(definition.type()))
      .collect(MoreCollectors.uniqueIndex(PropertyDefinition::key, Function.identity()));
    Set<String> settingsKeys = new HashSet<>(licenseDefinitionsByKeys.keySet());
    settingsKeys.add(PERMANENT_SERVER_ID);
    List<Setting> settings = settingsFinder.loadGlobalSettings(dbSession, settingsKeys);
    return new ListResponseBuilder(licenseDefinitionsByKeys, settings).build();
  }

  private static class ListResponseBuilder {
    private final Optional<String> serverId;
    private final Map<String, Setting> licenseSettingsByKey;
    private final Collection<PropertyDefinition> licenseDefinitions;

    ListResponseBuilder(Map<String, PropertyDefinition> licenseDefinitionsByKeys, List<Setting> settings) {
      this.serverId = getServerId(settings);
      this.licenseDefinitions = licenseDefinitionsByKeys.values();
      this.licenseSettingsByKey = settings.stream().collect(uniqueIndex(Setting::getKey, Function.identity()));
    }

    ListWsResponse build() {
      ListWsResponse.Builder wsResponse = ListWsResponse.newBuilder();
      licenseDefinitions.forEach(def -> wsResponse.addLicenses(buildLicense(def, licenseSettingsByKey.get(def.key()))));
      return wsResponse.build();
    }

    private Licenses.License buildLicense(PropertyDefinition definition, @Nullable Setting setting) {
      Licenses.License.Builder licenseBuilder = Licenses.License.newBuilder()
        .setKey(definition.key());
      String name = definition.name();
      if (!isNullOrEmpty(name)) {
        licenseBuilder.setName(name);
      }
      if (setting != null) {
        License license = License.readBase64(setting.getValue());
        licenseBuilder.setValue(setting.getValue());
        setProduct(licenseBuilder, license, setting);
        setOrganization(licenseBuilder, license);
        setExpiration(licenseBuilder, license);
        setServerId(licenseBuilder, license);
        setType(licenseBuilder, license);
        setAdditionalProperties(licenseBuilder, license);
      }
      return licenseBuilder.build();
    }

    private static void setProduct(Licenses.License.Builder licenseBuilder, License license, Setting setting) {
      String product = license.getProduct();
      if (product != null) {
        licenseBuilder.setProduct(product);
      }
      if (product == null || !setting.getKey().contains(product)) {
        licenseBuilder.setInvalidProduct(true);
      }
    }

    private static void setOrganization(Licenses.License.Builder licenseBuilder, License license) {
      String licenseOrganization = license.getOrganization();
      if (licenseOrganization != null) {
        licenseBuilder.setOrganization(licenseOrganization);
      }
    }

    private void setServerId(Licenses.License.Builder licenseBuilder, License license) {
      String licenseServerId = license.getServer();
      if (licenseServerId != null) {
        licenseBuilder.setServerId(licenseServerId);
      }
      boolean isValidServerId = Objects.equals(licenseServerId, ALL_SERVERS_VALUE)
        || (serverId.isPresent() && Objects.equals(licenseServerId, serverId.get()));
      if (!isValidServerId) {
        licenseBuilder.setInvalidServerId(true);
      }
    }

    private static void setExpiration(Licenses.License.Builder licenseBuilder, License license) {
      String expiration = license.getExpirationDateAsString();
      if (expiration != null) {
        licenseBuilder.setExpiration(expiration);
      }
      if (license.isExpired()) {
        licenseBuilder.setInvalidExpiration(true);
      }
    }

    private static void setType(Licenses.License.Builder licenseBuilder, License license) {
      String type = license.getType();
      if (type != null) {
        licenseBuilder.setType(type);
      }
    }

    private static void setAdditionalProperties(Licenses.License.Builder licenseBuilder, License license) {
      Map<String, String> additionalProperties = license.additionalProperties();
      if (!additionalProperties.isEmpty()) {
        licenseBuilder.getAdditionalPropertiesBuilder().putAllAdditionalProperties(additionalProperties).build();
      }
    }

    private static Optional<String> getServerId(List<Setting> settings) {
      Optional<Setting> setting = settings.stream().filter(s -> s.getKey().equals(PERMANENT_SERVER_ID)).findFirst();
      return setting.isPresent() ? Optional.of(setting.get().getValue()) : Optional.empty();
    }
  }
}
