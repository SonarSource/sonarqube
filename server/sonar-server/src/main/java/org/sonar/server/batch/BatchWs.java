/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

public class BatchWs implements WebService {

  public static final String API_ENDPOINT = "batch";

  private final BatchIndex batchIndex;
  private final BatchWsAction[] actions;

  public BatchWs(BatchIndex batchIndex, BatchWsAction... actions) {
    this.batchIndex = batchIndex;
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(API_ENDPOINT)
      .setSince("4.4")
      .setDescription("Get JAR files and referentials for batch");

    defineIndexAction(controller);
    defineFileAction(controller);
    for (BatchWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }

  private void defineIndexAction(NewController controller) {
    controller.createAction("index")
      .setInternal(true)
      .setSince("4.4")
      .setDescription("List the JAR files to be downloaded by source analyzer")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          try {
            response.stream().setMediaType("text/plain");
            IOUtils.write(batchIndex.getIndex(), response.stream().output());
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }
      })
      .setResponseExample(getClass().getResource("batch-index-example.txt"));
  }

  private void defineFileAction(NewController controller) {
    NewAction action = controller.createAction("file")
      .setInternal(true)
      .setSince("4.4")
      .setDescription("Download a JAR file required by source analyzer")
      .setResponseExample(getClass().getResource("batch-file-example.txt"))
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          String filename = request.mandatoryParam("name");
          try {
            response.stream().setMediaType("application/java-archive");
            FileUtils.copyFile(batchIndex.getFile(filename), response.stream().output());
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }
      });
    action
      .createParam("name")
      .setDescription("File name")
      .setExampleValue("batch-library-2.3.jar");
  }
}
