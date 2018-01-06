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
package org.sonar.ce.httpd;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * a Http action of the CE's HTTP server handles a request for a specified path.
 *
 * <p>
 * Method {@link #register(ActionRegistry)} of the action will be called right before the HTTP server is started (server
 * is started by the Pico Container). It's the action's responsibility to call the method
 * {@link ActionRegistry#register(ActionRegistry)} to register itself for a given path.
 * </p>
 * <p>
 * Method {@link #serve(IHTTPSession)} will be called each time a request matching the path the action registered itself
 * for.
 * </p>
 */
public interface HttpAction {
  void register(ActionRegistry registry);

  Response serve(IHTTPSession session);

  interface ActionRegistry {
    /**
     * @throws NullPointerException if {@code path} of {@code action} is {@code null}
     * @throws IllegalArgumentException if {@code path} is empty or starts with {@code /}
     * @throws IllegalStateException if an action is already registered for the specified path (case is ignored)
     */
    void register(String path, HttpAction action);
  }
}
