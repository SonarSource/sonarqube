/*
 * Copyright (C) 2009-2016 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonarsource.sonarqube.upgrade;

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
    return false;
  }
}
