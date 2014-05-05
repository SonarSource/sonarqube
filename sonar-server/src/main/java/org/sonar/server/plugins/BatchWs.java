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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.home.cache.FileHashes;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * JAR files to be downloaded by sonar-runner.
 */
public class BatchWs implements WebService, Startable {

  private final Server server;
  private String index;
  private File batchDir;

  public BatchWs(Server server) {
    this.server = server;
  }

  @Override
  public void start() {
    StringBuilder sb = new StringBuilder();
    batchDir = new File(server.getRootDir(), "lib/batch");
    if (batchDir.exists()) {
      Collection<File> files = FileUtils.listFiles(batchDir, HiddenFileFilter.VISIBLE, FileFilterUtils.directoryFileFilter());
      for (File file : files) {
        String filename = file.getName();
        if (StringUtils.endsWith(filename, ".jar")) {
          sb.append(filename).append('|').append(new FileHashes().of(file)).append(CharUtils.LF);
        }
      }
    }
    this.index = sb.toString();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("batch");
    controller.createAction("index")
      .setInternal(true)
      .setDescription("List the JAR files to be downloaded by source analyzer")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          index(response);
        }
      }).setResponseExample(getClass().getResource("example-batch-index.txt"));
    controller.createAction("file")
      .setInternal(true)
      .setDescription("Download a JAR file required by source analyzer")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          file(request, response);
        }
      }).createParam("name")
        .setDescription("File name")
        .setExampleValue("batch-library-2.3.jar");

    controller.done();
  }

  private void index(Response response) {
    try {
      response.stream().setMediaType("text/plain");
      IOUtils.write(index, response.stream().output());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to send batch index", e);
    }
  }

  private void file(Request request, Response response) {
    String filename = request.mandatoryParam("name");
    try {
      File input = new File(batchDir, filename);
      if (!input.exists() || !FileUtils.directoryContains(batchDir, input)) {
        throw new IllegalArgumentException("Bad filename: " + filename);
      }
      response.stream().setMediaType("application/java-archive");
      FileUtils.copyFile(input, response.stream().output());

    } catch (IOException e) {
      throw new IllegalStateException("Fail to send batch file " + filename, e);
    }
  }
}
