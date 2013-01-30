/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.startup.GenerateBootstrapIndex;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * This servlet allows to load libraries from directory "WEB-INF/lib" in order to provide them for batch-bootstrapper.
 * Most probably this is not a best solution.
 */
public class BatchResourcesServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(BatchResourcesServlet.class);
  private static final long serialVersionUID = -2100128371794649028L;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String filename = filename(request);
    if (StringUtils.isBlank(filename)) {
      PrintWriter writer = null;
      try {
        response.setContentType("text/plain");
        writer = response.getWriter();
        writer.print(StringUtils.join(GenerateBootstrapIndex.getLibs(getServletContext()), ','));
      } catch (IOException e) {
        LOG.error("Unable to provide list of batch resources", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } finally {
        IOUtils.closeQuietly(writer);
      }
    } else {
      InputStream in = null;
      OutputStream out = null;
      try {
        in = getServletContext().getResourceAsStream("/WEB-INF/lib/" + filename);
        if (in == null) {
          // TODO
        } else {
          response.setContentType("application/java-archive");
          out = response.getOutputStream();
          IOUtils.copy(in, out);
        }
      } catch (Exception e) {
        LOG.error("Unable to load batch resource '" + filename + "'", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }
  }

  /**
   * @return part of request URL after servlet path
   */
  String filename(HttpServletRequest request) {
    String filename = null;
    if (StringUtils.endsWithIgnoreCase(request.getRequestURI(), "jar")) {
      filename = StringUtils.substringAfterLast(request.getRequestURI(), "/");
    }
    return filename;
  }

}
