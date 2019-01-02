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
package org.sonar.server.plugins;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.server.platform.Platform;
import org.sonarqube.ws.MediaTypes;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

public class StaticResourcesServlet extends HttpServlet {

  private static final Logger LOG = Loggers.get(StaticResourcesServlet.class);
  private static final long serialVersionUID = -2577454614650178426L;

  private final System system;

  @VisibleForTesting
  StaticResourcesServlet(System system) {
    this.system = system;
  }

  public StaticResourcesServlet() {
    this.system = new System();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    String pluginKey = getPluginKey(request);
    String resource = getResourcePath(request);
    InputStream in = null;
    OutputStream out = null;
    try {
      CoreExtensionRepository coreExtensionRepository = system.getCoreExtensionRepository();
      PluginRepository pluginRepository = system.getPluginRepository();

      boolean coreExtension = coreExtensionRepository.isInstalled(pluginKey);
      if (!coreExtension && !pluginRepository.hasPlugin(pluginKey)) {
        silentlySendError(response, SC_NOT_FOUND);
        return;
      }

      in = coreExtension ? system.openCoreExtensionResourceStream(resource) : system.openPluginResourceStream(pluginKey, resource, pluginRepository);
      if (in == null) {
        silentlySendError(response, SC_NOT_FOUND);
        return;
      }

      // mime type must be set before writing response body
      completeContentType(response, resource);
      out = response.getOutputStream();
      IOUtils.copy(in, out);
    } catch (ClientAbortException e) {
      LOG.trace("Client canceled loading resource [{}] from plugin [{}]: {}", resource, pluginKey, e);
    } catch (Exception e) {
      LOG.error(format("Unable to load resource [%s] from plugin [%s]", resource, pluginKey), e);
      silentlySendError(response, SC_INTERNAL_SERVER_ERROR);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  private void silentlySendError(HttpServletResponse response, int error) {
    if (system.isCommitted(response)) {
      LOG.trace("Response is committed. Cannot send error response code {}", error);
      return;
    }
    try {
      system.sendError(response, error);
    } catch (IOException e) {
      LOG.trace("Failed to send error code {}: {}", error, e);
    }
  }

  /**
   * @return part of request URL after servlet path
   */
  private static String getPluginKeyAndResourcePath(HttpServletRequest request) {
    return StringUtils.substringAfter(request.getRequestURI(), request.getContextPath() + request.getServletPath() + "/");
  }

  private static String getPluginKey(HttpServletRequest request) {
    return StringUtils.substringBefore(getPluginKeyAndResourcePath(request), "/");
  }

  /**
   * Note that returned value should not have a leading "/" - see {@link Class#resolveName(String)}.
   */
  private static String getResourcePath(HttpServletRequest request) {
    return "static/" + StringUtils.substringAfter(getPluginKeyAndResourcePath(request), "/");
  }

  private static void completeContentType(HttpServletResponse response, String filename) {
    response.setContentType(MediaTypes.getByFilename(filename));
  }

  static class System {
    PluginRepository getPluginRepository() {
      return Platform.getInstance().getContainer().getComponentByType(PluginRepository.class);
    }

    CoreExtensionRepository getCoreExtensionRepository() {
      return Platform.getInstance().getContainer().getComponentByType(CoreExtensionRepository.class);
    }

    @CheckForNull
    InputStream openPluginResourceStream(String pluginKey, String resource, PluginRepository pluginRepository) throws Exception {
      ClassLoader pluginClassLoader = pluginRepository.getPluginInstance(pluginKey).getClass().getClassLoader();
      return pluginClassLoader.getResourceAsStream(resource);
    }

    @CheckForNull
    InputStream openCoreExtensionResourceStream(String resource) throws Exception {
      return getClass().getClassLoader().getResourceAsStream(resource);
    }

    boolean isCommitted(HttpServletResponse response) {
      return response.isCommitted();
    }

    void sendError(HttpServletResponse response, int error) throws IOException {
      response.sendError(error);
    }
  }
}
