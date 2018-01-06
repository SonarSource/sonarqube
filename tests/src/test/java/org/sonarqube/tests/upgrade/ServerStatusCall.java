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
package org.sonarqube.tests.upgrade;

import com.sonar.orchestrator.Orchestrator;
import javax.annotation.Nonnull;
import org.sonar.wsclient.jsonsimple.JSONObject;

public class ServerStatusCall extends WsCallAndWait<ServerStatusResponse> {
  protected ServerStatusCall(Orchestrator orchestrator) {
    super(orchestrator, "/api/system/status");
  }

  @Nonnull
  @Override
  protected ServerStatusResponse parse(JSONObject jsonObject) {
    return new ServerStatusResponse(
        (String) jsonObject.get("id"),
        (String) jsonObject.get("version"),
        ServerStatusResponse.Status.valueOf((String) jsonObject.get("status"))
    );
  }

  @Override
  protected boolean shouldWait(ServerStatusResponse serverStatusResponse) {
    ServerStatusResponse.Status status = serverStatusResponse.getStatus();
    return status == ServerStatusResponse.Status.STARTING || status == ServerStatusResponse.Status.DB_MIGRATION_RUNNING;
  }
}
