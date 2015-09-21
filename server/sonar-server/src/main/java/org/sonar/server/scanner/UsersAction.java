/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.scanner;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.BatchInput;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

public class UsersAction implements ScannerWsAction {

  private static final String PARAM_LOGINS = "logins";

  private final UserIndex userIndex;
  private final UserSession userSession;

  public UsersAction(UserIndex userIndex, UserSession userSession) {
    this.userIndex = userIndex;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("users")
      .setDescription("Return user details.")
      .setSince("5.2")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_LOGINS)
      .setRequired(true)
      .setDescription("A comma separated list of user logins")
      .setExampleValue("ada.lovelace,grace.hopper");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkGlobalPermission(GlobalPermissions.PREVIEW_EXECUTION);
    List<String> logins = request.mandatoryParamAsStrings(PARAM_LOGINS);

    response.stream().setMediaType(MimeTypes.PROTOBUF);
    BatchInput.User.Builder userBuilder = BatchInput.User.newBuilder();
    OutputStream output = response.stream().output();
    try {
      for (Iterator<UserDoc> userDocIterator = userIndex.selectUsersForBatch(logins); userDocIterator.hasNext();) {
        handleUser(userDocIterator.next(), userBuilder, output);
      }
    } finally {
      output.close();
    }
  }

  private void handleUser(UserDoc user, BatchInput.User.Builder userBuilder, OutputStream out) {
    userBuilder.setLogin(user.login())
      .setName(user.name());
    try {
      userBuilder.build().writeDelimitedTo(out);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to serialize user", e);
    }
    userBuilder.clear();
  }
}
