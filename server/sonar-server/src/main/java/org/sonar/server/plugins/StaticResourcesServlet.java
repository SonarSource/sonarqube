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
package org.sonar.server.plugins;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.Platform;
import org.sonarqube.ws.MediaTypes;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

public class StaticResourcesServlet extends HttpServlet {

  private static final Logger LOG = Loggers.get(StaticResourcesServlet.class);
  private static final long serialVersionUID = -2577454614650178426L;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    String pluginKey = getPluginKey(request);
    String resource = getResourcePath(request);
    InputStream in = null;
    OutputStream out = null;
    try {
      PluginRepository pluginRepository = getContainer().getComponentByType(PluginRepository.class);
      if (!pluginRepository.hasPlugin(pluginKey)) {
        silentlySendError(response, SC_NOT_FOUND);
        return;
      }

      in = pluginRepository.getPluginInstance(pluginKey).getClass().getClassLoader().getResourceAsStream(resource);
      if (in != null) {
        // mime type must be set before writing response body
        completeContentType(response, resource);
        out = response.getOutputStream();
        IOUtils.copy(in, out);
      } else {
        silentlySendError(response, SC_NOT_FOUND);
      }
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

  @VisibleForTesting
  protected ComponentContainer getContainer() {
    return Platform.getInstance().getContainer();
  }

  private static void silentlySendError(HttpServletResponse response, int error) {
    if (response.isCommitted()) {
      LOG.trace("Response is committed. Cannot send error response code {}", error);
      return;
    }
    try {
      response.sendError(error);
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

  @VisibleForTesting
  protected String getPluginKey(HttpServletRequest request) {
    return StringUtils.substringBefore(getPluginKeyAndResourcePath(request), "/");
  }

  /**
   * Note that returned value should not have a leading "/" - see {@link Class#resolveName(String)}.
   */
  protected String getResourcePath(HttpServletRequest request) {
    return "static/" + StringUtils.substringAfter(getPluginKeyAndResourcePath(request), "/");
  }

  @VisibleForTesting
  void completeContentType(HttpServletResponse response, String filename) {
    response.setContentType(MediaTypes.getByFilename(filename));
  }
}
