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

package org.sonar.server.serverid.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ServerId.GenerateWsResponse;
import org.sonarqube.ws.client.serverid.GenerateRequest;

import static org.sonar.api.CoreProperties.ORGANISATION;
import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.api.CoreProperties.SERVER_ID_IP_ADDRESS;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GenerateAction implements ServerIdWsAction {
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_IP = "ip";

  private static final Logger LOG = Loggers.get(GenerateAction.class);

  private final UserSession userSession;
  private final ServerIdGenerator generator;
  private final DbClient dbClient;

  public GenerateAction(UserSession userSession, ServerIdGenerator generator, DbClient dbClient) {
    this.userSession = userSession;
    this.generator = generator;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("generate")
      .setDescription("Generate a server id.<br/>" +
        "Requires 'System Administer' permissions")
      .setSince("6.1")
      .setInternal(true)
      .setPost(true)
      .setResponseExample(getClass().getResource("generate-example.json"))
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization name")
      .setExampleValue("SonarSource")
      .setRequired(true);

    action.createParam(PARAM_IP)
      .setDescription("IP address")
      .setExampleValue("10.142.20.56")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkPermission(SYSTEM_ADMIN);

    DbSession dbSession = dbClient.openSession(true);
    try {
      writeProtobuf(doHandle(dbSession, toGenerateRequest(request)), request, response);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private GenerateWsResponse doHandle(DbSession dbSession, GenerateRequest request) {
    String serverId = generator.generate(request.getOrganization(), request.getIp());
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(PERMANENT_SERVER_ID).setValue(serverId));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(ORGANISATION).setValue(request.getOrganization()));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(SERVER_ID_IP_ADDRESS).setValue(request.getIp()));
    dbSession.commit();
    LOG.info("Generated new server ID={}", serverId);

    return GenerateWsResponse.newBuilder().setServerId(serverId).build();
  }

  private static GenerateRequest toGenerateRequest(Request request) {
    return GenerateRequest.builder()
      .setOrganization(request.mandatoryParam(PARAM_ORGANIZATION))
      .setIp(request.mandatoryParam(PARAM_IP))
      .build();
  }

}
