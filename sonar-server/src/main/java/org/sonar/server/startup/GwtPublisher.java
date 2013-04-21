/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.web.GwtExtension;
import org.sonar.server.platform.ServerSettings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipEntry;

public class GwtPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(GwtPublisher.class);

  private Settings settings;
  private GwtExtension[] extensions = null;
  private File outputDir = null;

  public GwtPublisher(GwtExtension[] extensions, Settings settings) {
    this.extensions = extensions;
    this.settings = settings;
  }

  GwtPublisher(GwtExtension[] extensions, File outputDir) {
    this.extensions = extensions;
    this.outputDir = outputDir;
  }

  GwtPublisher() {
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Deploy GWT plugins");
    try {
      cleanDirectory();
      this.outputDir = new File(settings.getString(ServerSettings.DEPLOY_DIR), "gwt");
      LoggerFactory.getLogger(GwtPublisher.class).debug("Deploy {} GWT extensions to {}", extensions.length, outputDir);
      publish();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to deploy GWT extensions", e);
    }
    profiler.stop();
  }

  protected void cleanDirectory() {
    try {
      if (outputDir != null && outputDir.exists()) {
        File[] files = outputDir.listFiles();
        if (files != null) {
          for (File file : files) {
            // avoid issues with SCM hidden dirs
            if (!file.isHidden()) {
              if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
                FileUtils.deleteDirectory(file);
              } else {
                file.delete();
              }
            }
          }
        }
      }

    } catch (IOException e) {
      LOG.warn("can not clean the directory " + outputDir, e);
    }
  }

  protected void publish() throws IOException, URISyntaxException {
    for (final GwtExtension module : extensions) {
      URL sourceDir = module.getClass().getResource("/" + module.getGwtId() + "/");
      if (sourceDir == null) {
        throw new SonarException("Can not find the directory " + module.getGwtId() + " defined by the GWT module " + module.getClass().getName());
      }
      Logs.INFO.info("Deploy {} to {}", module.getGwtId(), outputDir);
      if (sourceDir.toString().startsWith("jar:file")) {
        // unzip the JAR
        String path = StringUtils.substringBetween(sourceDir.toString(), "jar:file:", "!");
        File gwtJar = new File(getCleanPath(path));
        ZipUtils.unzip(gwtJar, outputDir, new ZipUtils.ZipEntryFilter() {
          public boolean accept(ZipEntry entry) {
            return entry.getName().startsWith(module.getGwtId());
          }
        });
      } else {
        // just copy the files
        File source = new File(sourceDir.toURI());
        FileUtils.copyDirectory(source, new File(outputDir, module.getGwtId()));
      }
    }
  }

  protected String getCleanPath(String path) throws URISyntaxException {
    return new URI(path).getPath();
  }
}
