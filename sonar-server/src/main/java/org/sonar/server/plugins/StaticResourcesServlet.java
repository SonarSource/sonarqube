package org.sonar.server.plugins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.platform.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StaticResourcesServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(StaticResourcesServlet.class);

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String path = StringUtils.substringAfter(request.getRequestURI(), request.getContextPath() + request.getServletPath() + "/");
    String pluginKey = StringUtils.substringBefore(path, "/");
    String resource = "/static/" + StringUtils.substringAfter(path, "/");

    PluginClassLoaders pluginClassLoaders = Platform.getInstance().getContainer().getComponent(PluginClassLoaders.class);

    ClassLoader classLoader = pluginClassLoaders.getClassLoader(pluginKey);
    if (classLoader == null) {
      return;
    }

    InputStream in = null;
    OutputStream out = null;
    try {
      in = classLoader.getResourceAsStream(resource);
      if (in != null) {
        out = response.getOutputStream();
        IOUtils.copy(in, out);
      }
    } catch (Exception e) {
      LOG.error("Unable to load static resource '" + resource + "' from plugin '" + pluginKey + "'", e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }
}
