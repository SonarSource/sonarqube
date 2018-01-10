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

import ce.BombConfig;
import java.util.Arrays;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static java.util.stream.Collectors.toList;

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
      .setPossibleValues(Arrays.stream(BombType.values()).map(Enum::toString).collect(toList()));
  }

  @Override
  public void handle(Request request, Response response) {
    BombType bombType = BombType.valueOf(request.mandatoryParam(PARAM_BOMB_TYPE));

    bombConfig.reset();
    switch (bombType) {
      case ISE_START:
        bombConfig.setIseStartBomb(true);
        break;
      case OOM_START:
        bombConfig.setOomStartBomb(true);
        break;
      case ISE_STOP:
        bombConfig.setIseStopBomb(true);
        break;
      case OOM_STOP:
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
    NONE, OOM_START, ISE_START, OOM_STOP, ISE_STOP

  }

}
