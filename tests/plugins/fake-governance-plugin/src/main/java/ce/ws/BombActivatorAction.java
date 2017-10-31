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
package ce.ws;

import ce.BombConfig;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

public class BombActivatorAction implements FakeGoVWsAction {

  private static final String PARAM_BOMB_TYPE = "type";

  private final BombConfig bombConfig;

  public BombActivatorAction(BombConfig bombConfig) {
    this.bombConfig = bombConfig;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("activate_bomb")
      .setPost(true)
      .setHandler(this);
    action.createParam(PARAM_BOMB_TYPE)
      .setRequired(true)
      .setPossibleValues("OOM", "ISE", "NONE");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    BombType bombType = BombType.valueOf(request.mandatoryParam(PARAM_BOMB_TYPE));

    bombConfig.setIseStopBomb(false);
    bombConfig.setOomStopBomb(false);
    switch (bombType) {
      case ISE:
        bombConfig.setIseStopBomb(true);
        break;
      case OOM:
        bombConfig.setOomStopBomb(true);
        break;
      case NONE:
        break;
      default:
        throw new IllegalArgumentException("Unsupported bomb type " + bombType);
    }

    response.noContent();
  }

  enum BombType {
    NONE, OOM, ISE

  }

}
