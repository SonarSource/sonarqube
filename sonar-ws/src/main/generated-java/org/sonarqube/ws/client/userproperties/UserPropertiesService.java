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
package org.sonarqube.ws.client.userproperties;

import com.google.common.base.Joiner;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/*
 * THIS FILE HAS BEEN AUTOMATICALLY GENERATED
 */
/**
 * Removed since 6.3, please use api/favorites and api/notifications instead
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_properties">Further information about this web service online</a>
 */
public class UserPropertiesService extends BaseService {

  public UserPropertiesService(WsConnector wsConnector) {
    super(wsConnector, "api/user_properties");
  }

  /**
   * This web service is removed
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_properties/index">Further information about this action online (including a response example)</a>
   * @since 2.6
   * @deprecated since 6.3
   */
  @Deprecated
  public String index() {
    return call(
      new GetRequest(path("index"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
