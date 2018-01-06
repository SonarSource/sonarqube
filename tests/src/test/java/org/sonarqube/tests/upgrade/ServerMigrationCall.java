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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.wsclient.jsonsimple.JSONObject;

public class ServerMigrationCall extends WsCallAndWait<ServerMigrationResponse> {

  public ServerMigrationCall(Orchestrator orchestrator) {
    super(orchestrator, "/api/system/migrate_db");
  }

  @Override
  @Nonnull
  protected ServerMigrationResponse parse(JSONObject jsonObject) {
    try {
      return new ServerMigrationResponse(
          ServerMigrationResponse.Status.valueOf((String) jsonObject.get("state")),
          (String) jsonObject.get("message"),
          parseDate((String) jsonObject.get("createdAt")));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse JSON response", e);
    }
  }

  @CheckForNull
  private Date parseDate(@Nullable String createdAt) throws ParseException {
    if (createdAt == null || createdAt.isEmpty()) {
      return null;
    }
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(createdAt);
  }

  @Override
  protected boolean shouldWait(ServerMigrationResponse response) {
    return response.getStatus() == ServerMigrationResponse.Status.MIGRATION_NEEDED
        || response.getStatus() == ServerMigrationResponse.Status.MIGRATION_RUNNING;
  }

}
