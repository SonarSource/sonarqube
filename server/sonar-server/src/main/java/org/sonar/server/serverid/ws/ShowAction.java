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
package org.sonar.server.serverid.ws;

import com.google.common.collect.ImmutableSet;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.user.UserSession;

import static java.util.stream.Collectors.toList;
import static org.sonar.api.CoreProperties.ORGANISATION;
import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.api.CoreProperties.SERVER_ID_IP_ADDRESS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.ServerId.ShowWsResponse;

public class ShowAction implements ServerIdWsAction {

  private static final Set<String> SETTINGS_KEYS = ImmutableSet.of(PERMANENT_SERVER_ID, ORGANISATION, SERVER_ID_IP_ADDRESS);

  private final UserSession userSession;
  private final ServerIdGenerator serverIdGenerator;
  private final DbClient dbClient;

  public ShowAction(UserSession userSession, ServerIdGenerator serverIdGenerator, DbClient dbClient) {
    this.userSession = userSession;
    this.serverIdGenerator = serverIdGenerator;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("show")
      .setDescription("Get server id configuration.<br/>" +
        "Requires 'System Administer' permissions")
      .setSince("6.1")
      .setInternal(true)
      .setResponseExample(getClass().getResource("show-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    try (DbSession dbSession = dbClient.openSession(true)) {
      Map<String, PropertyDto> properties = dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, SETTINGS_KEYS).stream()
        .collect(MoreCollectors.uniqueIndex(PropertyDto::getKey, Function.identity()));
      writeProtobuf(doHandle(properties), request, response);
    }
  }

  private ShowWsResponse doHandle(Map<String, PropertyDto> properties) {
    ShowWsResponse.Builder responseBuilder = ShowWsResponse.newBuilder();
    List<String> validIpAddresses = serverIdGenerator.getAvailableAddresses().stream().map(InetAddress::getHostAddress).collect(toList());
    if (!validIpAddresses.isEmpty()) {
      responseBuilder.addAllValidIpAddresses(validIpAddresses);
    }

    Optional<String> serverId = getSettingValue(properties.get(PERMANENT_SERVER_ID));
    if (serverId.isPresent()) {
      responseBuilder.setServerId(serverId.get());
      Optional<String> organization = getSettingValue(properties.get(ORGANISATION));
      organization.ifPresent(responseBuilder::setOrganization);
      Optional<String> ip = getSettingValue(properties.get(SERVER_ID_IP_ADDRESS));
      ip.ifPresent(responseBuilder::setIp);
      boolean isValidServId = isValidServerId(serverId.get(), organization, ip);
      if (!isValidServId) {
        responseBuilder.setInvalidServerId(true);
      }
    }
    return responseBuilder.build();
  }

  private static Optional<String> getSettingValue(@Nullable PropertyDto propertyDto) {
    return propertyDto != null ? Optional.of(propertyDto.getValue()) : Optional.empty();
  }

  private boolean isValidServerId(String serverId, Optional<String> organization, Optional<String> ip) {
    return organization.isPresent()
      && ip.isPresent()
      && serverIdGenerator.validate(organization.get(), ip.get(), serverId);
  }
}
