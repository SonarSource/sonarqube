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
package org.sonar.server.ce.ws;

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.configuration.WorkerCountProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsCe.WorkerCountResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class WorkerCountAction implements CeWsAction {

  public static final String ACTION = "worker_count";

  private final UserSession userSession;
  private final WorkerCountProvider workerCountProvider;

  public WorkerCountAction(UserSession userSession, @Nullable WorkerCountProvider workerCountProvider) {
    this.userSession = userSession;
    this.workerCountProvider = workerCountProvider;
  }

  public WorkerCountAction(UserSession userSession) {
    this(userSession, null);
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction(ACTION)
      .setDescription("Return number of Compute Engine workers.<br/>" +
        "Requires the system administration permission")
      .setResponseExample(getClass().getResource("worker_count-example.json"))
      .setSince("6.5")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkIsSystemAdministrator();
    writeProtobuf(createResponse(), wsRequest, wsResponse);
  }

  private WorkerCountResponse createResponse(){
    WorkerCountResponse.Builder builder = WorkerCountResponse.newBuilder();
    if (workerCountProvider == null) {
      return builder
        .setValue(1)
        .setCanSetWorkerCount(false)
        .build();
    }
    return builder
      .setValue(workerCountProvider.get())
      .setCanSetWorkerCount(true)
      .build();
  }

}
