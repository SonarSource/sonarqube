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
package org.sonar.server.plugins;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.Plugin;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.Platform;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StaticResourcesServlet extends HttpServlet {

  private static final Logger LOG = Loggers.get(StaticResourcesServlet.class);
  private static final long serialVersionUID = -2577454614650178426L;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pluginKey = getPluginKey(request);
    String resource = getResourcePath(request);

    PluginRepository pluginRepository = Platform.getInstance().getContainer().getComponentByType(PluginRepository.class);
    if (!pluginRepository.hasPlugin(pluginKey)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    InputStream in = null;
    OutputStream out = null;
    try {
      in = pluginRepository.getPluginInstance(pluginKey).getClass().getClassLoader().getResourceAsStream(resource);
      if (in != null) {
        // mime type must be set before writing response body
        completeContentType(response, resource);
        out = response.getOutputStream();
        IOUtils.copy(in, out);
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (Exception e) {
      LOG.error(String.format("Unable to load resource [%s] from plugin [%s]", resource, pluginKey), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * @return part of request URL after servlet path
   */
  protected String getPluginKeyAndResourcePath(HttpServletRequest request) {
    return StringUtils.substringAfter(request.getRequestURI(), request.getContextPath() + request.getServletPath() + "/");
  }

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
    response.setContentType(MimeTypes.getByFilename(filename));
  }
}
