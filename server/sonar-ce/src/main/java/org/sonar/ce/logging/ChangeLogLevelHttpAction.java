/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.logging;

import fi.iki.elonen.NanoHTTPD;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.httpd.HttpAction;
import org.sonar.server.log.ServerLogging;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.Response.Status.BAD_REQUEST;
import static fi.iki.elonen.NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;
import static java.lang.String.format;

public class ChangeLogLevelHttpAction implements HttpAction {

  private static final String PATH = "changeLogLevel";
  private static final String PARAM_LEVEL = "level";

  private final ServerLogging logging;

  public ChangeLogLevelHttpAction(ServerLogging logging) {
    this.logging = logging;
  }

  @Override
  public void register(ActionRegistry registry) {
    registry.register(PATH, this);
  }

  @Override
  public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
    if (session.getMethod() != NanoHTTPD.Method.POST) {
      return newFixedLengthResponse(METHOD_NOT_ALLOWED, MIME_PLAINTEXT, null);
    }

    String levelStr = session.getParms().get(PARAM_LEVEL);
    if (levelStr == null || levelStr.isEmpty()) {
      return newFixedLengthResponse(BAD_REQUEST, MIME_PLAINTEXT, format("Parameter '%s' is missing", PARAM_LEVEL));
    }
    try {
      LoggerLevel level = LoggerLevel.valueOf(levelStr);
      logging.changeLevel(level);
      return newFixedLengthResponse(OK, MIME_PLAINTEXT, null);
    } catch (IllegalArgumentException e) {
      Loggers.get(ChangeLogLevelHttpAction.class).debug("Value '{}' for parameter '" + PARAM_LEVEL + "' is invalid: {}", levelStr, e);
      return newFixedLengthResponse(BAD_REQUEST, MIME_PLAINTEXT, format("Value '%s' for parameter '%s' is invalid", levelStr, PARAM_LEVEL));
    }
  }
}
