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
package org.sonar.ce.taskprocessor;

import fi.iki.elonen.NanoHTTPD;
import org.sonar.ce.httpd.HttpAction;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;
import static fi.iki.elonen.NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;

public class RefreshWorkerCountAction implements HttpAction {
  private static final String PATH = "refreshWorkerCount";

  private final EnabledCeWorkerController enabledCeWorkerController;

  public RefreshWorkerCountAction(EnabledCeWorkerController enabledCeWorkerController) {
    this.enabledCeWorkerController = enabledCeWorkerController;
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

    enabledCeWorkerController.refresh();

    return newFixedLengthResponse(OK, MIME_PLAINTEXT, null);
  }
}
