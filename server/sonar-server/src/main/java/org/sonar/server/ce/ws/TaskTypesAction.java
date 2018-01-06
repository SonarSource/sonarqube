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
package org.sonar.server.ce.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.taskprocessor.CeTaskProcessor;
import org.sonarqube.ws.Ce;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class TaskTypesAction implements CeWsAction {
  private final Set<String> taskTypes;

  public TaskTypesAction(CeTaskProcessor[] taskProcessors) {
    ImmutableSet.Builder<String> taskTypesBuilder = ImmutableSet.builder();
    for (CeTaskProcessor taskProcessor : taskProcessors) {
      taskTypesBuilder.addAll(taskProcessor.getHandledCeTaskTypes());
    }
    this.taskTypes = taskTypesBuilder.build();
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("task_types")
      .setDescription("List available task types")
      .setResponseExample(getClass().getResource("task_types-example.json"))
      .setSince("5.5")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Ce.TaskTypesWsResponse taskTypesWsResponse = Ce.TaskTypesWsResponse.newBuilder()
      .addAllTaskTypes(taskTypes)
      .build();

    writeProtobuf(taskTypesWsResponse, request, response);
  }
}
