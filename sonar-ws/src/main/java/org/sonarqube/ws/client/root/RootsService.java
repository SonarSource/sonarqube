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
package org.sonarqube.ws.client.root;

import org.sonarqube.ws.WsRoot;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

public class RootsService extends BaseService {
  public RootsService(WsConnector wsConnector) {
    super(wsConnector, "api/roots");
  }

  public WsRoot.SearchWsResponse search() {
    return call(new GetRequest(path("search")), WsRoot.SearchWsResponse.parser());
  }

  public void setRoot(String login) {
    PostRequest post = new PostRequest(path("set_root"))
      .setParam("login", login);

    call(post);
  }

  public void unsetRoot(String login) {
    PostRequest post = new PostRequest(path("unset_root"))
      .setParam("login", login);

    call(post);
  }
}
