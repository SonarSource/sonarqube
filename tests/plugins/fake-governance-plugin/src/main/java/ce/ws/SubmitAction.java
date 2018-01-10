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
package ce.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;

public class SubmitAction implements FakeGoVWsAction {

  private static final String PARAM_TYPE = "type";

  private final CeQueue ceQueue;

  public SubmitAction(CeQueue ceQueue) {
    this.ceQueue = ceQueue;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("submit")
      .setPost(true)
      .setHandler(this);
    action.createParam(PARAM_TYPE)
      .setRequired(true)
      .setPossibleValues("OOM", "OK", "ISE");
  }

  @Override
  public void handle(Request request, Response response) {
    String type = request.mandatoryParam(PARAM_TYPE);

    CeTaskSubmit.Builder submit = ceQueue.prepareSubmit();
    submit.setType(type);

    ceQueue.submit(submit.build());
    response.noContent();
  }
}
