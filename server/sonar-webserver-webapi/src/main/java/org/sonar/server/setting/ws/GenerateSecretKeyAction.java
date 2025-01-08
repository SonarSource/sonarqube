/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Settings.GenerateSecretKeyWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GenerateSecretKeyAction implements SettingsWsAction {
  private final DbClient dbClient;
  private final Settings settings;
  private final UserSession userSession;
  private final AuditPersister auditPersister;

  public GenerateSecretKeyAction(DbClient dbClient, Settings settings, UserSession userSession, AuditPersister auditPersister) {
    this.dbClient = dbClient;
    this.settings = settings;
    this.userSession = userSession;
    this.auditPersister = auditPersister;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("generate_secret_key")
      .setDescription("Generate a secret key.<br>" +
        "Requires the 'Administer System' permission")
      .setSince("6.1")
      .setInternal(true)
      .setResponseExample(getClass().getResource("generate_secret_key-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    writeProtobuf(GenerateSecretKeyWsResponse.newBuilder().setSecretKey(settings.getEncryption().generateRandomSecretKey()).build(), request, response);

    try (DbSession dbSession = dbClient.openSession(false)) {
      auditPersister.generateSecretKey(dbSession);
      dbSession.commit();
    }
  }
}
