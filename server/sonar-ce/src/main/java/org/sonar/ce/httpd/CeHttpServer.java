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
package org.sonar.ce.httpd;

import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.picocontainer.Startable;
import org.slf4j.LoggerFactory;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

/**
 * This HTTP server exports data required for display of System Info page (and the related web service).
 * It listens on loopback address only, so it does not need to be secure (no HTTPS, no authentication).
 */
public class CeHttpServer implements Startable {

  private final Properties processProps;
  private final List<HttpAction> actions;
  private final ActionRegistryImpl actionRegistry;
  private final CeNanoHttpd nanoHttpd;

  public CeHttpServer(Properties processProps, List<HttpAction> actions) {
    this.processProps = processProps;
    this.actions = actions;
    this.actionRegistry = new ActionRegistryImpl();
    this.nanoHttpd = new CeNanoHttpd(InetAddress.getLoopbackAddress().getHostAddress(), 0, actionRegistry);
  }

  @Override
  public void start() {
    try {
      registerActions();
      nanoHttpd.start();
      registerHttpUrl();
    } catch (IOException e) {
      throw new IllegalStateException("Can not start local HTTP server for System Info monitoring", e);
    }
  }

  private void registerActions() {
    actions.forEach(action -> action.register(this.actionRegistry));
  }

  private void registerHttpUrl() {
    int processNumber = parseInt(processProps.getProperty(PROPERTY_PROCESS_INDEX));
    File shareDir = new File(processProps.getProperty(PROPERTY_SHARED_PATH));
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(shareDir, processNumber)) {
      String url = getUrl();
      commands.setHttpUrl(url);
      LoggerFactory.getLogger(getClass()).debug("System Info HTTP server listening at {}", url);
    }
  }

  @Override
  public void stop() {
    nanoHttpd.stop();
  }

  // visible for testing
  String getUrl() {
    return "http://" + nanoHttpd.getHostname() + ":" + nanoHttpd.getListeningPort();
  }

  private static class CeNanoHttpd extends NanoHTTPD {
    private final ActionRegistryImpl actionRegistry;

    CeNanoHttpd(String hostname, int port, ActionRegistryImpl actionRegistry) {
      super(hostname, port);
      this.actionRegistry = actionRegistry;
    }

    @Override
    public Response serve(IHTTPSession session) {
      return actionRegistry.getAction(session)
        .map(action -> serveFromAction(session, action))
        .orElseGet(() -> newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT, format("Error 404, '%s' not found.", session.getUri())));
    }

    private static Response serveFromAction(IHTTPSession session, HttpAction action) {
      try {
        return action.serve(session);
      } catch (Exception e) {
        return newFixedLengthResponse(INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
      }
    }
  }

  private static final class ActionRegistryImpl implements HttpAction.ActionRegistry {
    private final Map<String, HttpAction> actionsByPath = new HashMap<>();

    @Override
    public void register(String path, HttpAction action) {
      requireNonNull(path, "path can't be null");
      requireNonNull(action, "action can't be null");
      checkArgument(!path.isEmpty(), "path can't be empty");
      checkArgument(!path.startsWith("/"), "path must not start with '/'");
      String fixedPath = path.toLowerCase(Locale.ENGLISH);
      HttpAction existingAction = actionsByPath.put(fixedPath, action);
      checkState(existingAction == null, "Action '%s' already registered for path '%s'", existingAction, fixedPath);
    }

    Optional<HttpAction> getAction(NanoHTTPD.IHTTPSession session) {
      String path = session.getUri().substring(1).toLowerCase(Locale.ENGLISH);
      return Optional.ofNullable(actionsByPath.get(path));
    }
  }
}
