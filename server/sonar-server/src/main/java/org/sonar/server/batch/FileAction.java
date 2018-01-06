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
package org.sonar.server.batch;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

public class FileAction implements BatchWsAction {

  private final BatchIndex batchIndex;

  public FileAction(BatchIndex batchIndex) {
    this.batchIndex = batchIndex;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("file")
      .setInternal(true)
      .setSince("4.4")
      .setDescription("Download a JAR file listed in the index (see batch/index)")
      .setResponseExample(getClass().getResource("batch-file-example.txt"))
      .setHandler(this);
    action
      .createParam("name")
      .setDescription("File name")
      .setExampleValue("batch-library-2.3.jar");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String filename = request.mandatoryParam("name");
    try {
      response.stream().setMediaType("application/java-archive");
      File file = batchIndex.getFile(filename);
      FileUtils.copyFile(file, response.stream().output());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
