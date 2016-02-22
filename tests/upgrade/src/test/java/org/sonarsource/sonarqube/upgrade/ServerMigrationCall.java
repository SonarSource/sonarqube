/*
 * Copyright (C) 2009-2016 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonarsource.sonarqube.upgrade;

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
