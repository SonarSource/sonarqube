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
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.Plugin;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Ruby on Rails requires the files to be on filesystem but not in Java classpath (JAR). This component extracts
 * all the needed files from plugins and copy them to $SONAR_HOME/temp
 *
 * @since 3.0
 */
public class RailsAppsDeployer implements Startable {

  private static final Logger LOG = Loggers.get(RailsAppsDeployer.class);
  private static final String ROR_PATH = "org/sonar/ror/";

  private final ServerFileSystem fs;
  private final PluginRepository pluginRepository;

  public RailsAppsDeployer(ServerFileSystem fs, PluginRepository pluginRepository) {
    this.fs = fs;
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void start() {
    LOG.info("Deploying Ruby on Rails applications");
    File appsDir = prepareRailsDirectory();

    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      String pluginKey = pluginInfo.getKey();
      Plugin plugin = pluginRepository.getPluginInstance(pluginKey);
      try {
        deployRailsApp(appsDir, pluginKey, plugin.getClass().getClassLoader());
      } catch (Exception e) {
        throw new IllegalStateException(String.format("Fail to deploy Ruby on Rails application of plugin [%s]", pluginKey), e);
      }
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  @VisibleForTesting
  File prepareRailsDirectory() {
    File appsDir = new File(fs.getTempDir(), "ror");
    prepareDir(appsDir);
    return appsDir;
  }

  @VisibleForTesting
  static void deployRailsApp(File appsDir, final String pluginKey, ClassLoader appClassLoader) {
    if (hasRailsApp(pluginKey, appClassLoader)) {
      LOG.info("Deploying app: " + pluginKey);
      File appDir = new File(appsDir, pluginKey);
      ClassLoaderUtils.copyResources(appClassLoader, pathToRubyInitFile(pluginKey), appDir, new Function<String, String>() {
        @Override
        public String apply(@Nullable String relativePath) {
          // Relocate the deployed files :
          // relativePath format is: org/sonar/ror/sqale/app/controllers/foo_controller.rb
          // app path is: org/sonar/ror/sqale
          // -> deployed file is app/controllers/foo_controller.rb
          return StringUtils.substringAfter(relativePath, pluginKey + "/");
        }
      });
    }
  }

  private static String pathToRubyInitFile(String pluginKey) {
    return ROR_PATH + pluginKey + "/init.rb";
  }

  @VisibleForTesting
  static boolean hasRailsApp(String pluginKey, ClassLoader classLoader) {
    return classLoader.getResource(pathToRubyInitFile(pluginKey)) != null;
  }

  private void prepareDir(File appsDir) {
    if (appsDir.exists() && appsDir.isDirectory()) {
      try {
        FileUtils.deleteDirectory(appsDir);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to delete temp directory: " + appsDir, e);
      }
    }
    try {
      FileUtils.forceMkdir(appsDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp directory: " + appsDir, e);
    }
  }
}
