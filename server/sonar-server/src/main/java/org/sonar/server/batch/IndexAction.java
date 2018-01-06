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

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

public class IndexAction implements BatchWsAction {

  private final BatchIndex batchIndex;

  public IndexAction(BatchIndex batchIndex) {
    this.batchIndex = batchIndex;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("index")
      .setInternal(true)
      .setSince("4.4")
      .setDescription("List the JAR files to be downloaded by scanners")
      .setHandler(this)
      .setResponseExample(getClass().getResource("index-example.txt"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try {
      response.stream().setMediaType("text/plain");
      String index = batchIndex.getIndex();
      checkState(index != null, "No available files");
      IOUtils.write(index, response.stream().output(), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
