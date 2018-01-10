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
package org.sonar.server.ws;

import com.google.common.io.Resources;
import java.net.URL;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.server.exceptions.ServerException;

import static java.net.HttpURLConnection.HTTP_GONE;

/**
 * Used to declare web services that are removed
 */
public enum RemovedWebServiceHandler implements RequestHandler {

  INSTANCE;

  @Override
  public void handle(Request request, Response response) {
    throw new ServerException(HTTP_GONE, String.format("The web service '%s' doesn't exist anymore, please read its documentation to use alternatives", request.getPath()));
  }

  public URL getResponseExample() {
    return Resources.getResource(RemovedWebServiceHandler.class, "removed-ws-example.json");
  }
}
