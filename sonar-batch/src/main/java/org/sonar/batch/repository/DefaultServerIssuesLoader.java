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
package org.sonar.batch.repository;

import com.google.common.base.Function;
import com.google.common.io.InputSupplier;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.api.utils.HttpDownloader;

import java.io.IOException;
import java.io.InputStream;

public class DefaultServerIssuesLoader implements ServerIssuesLoader {

  private final ServerClient serverClient;

  public DefaultServerIssuesLoader(ServerClient serverClient) {
    this.serverClient = serverClient;
  }

  @Override
  public void load(String componentKey, Function<ServerIssue, Void> consumer, boolean incremental) {
    InputSupplier<InputStream> request = serverClient.doRequest("/batch/issues?key=" + ServerClient.encodeForUrl(componentKey), "GET", null);
    try (InputStream is = request.getInput()) {
      ServerIssue previousIssue = ServerIssue.parseDelimitedFrom(is);
      while (previousIssue != null) {
        consumer.apply(previousIssue);
        previousIssue = ServerIssue.parseDelimitedFrom(is);
      }
    } catch (HttpDownloader.HttpException e) {
      throw serverClient.handleHttpException(e);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get previous issues", e);
    }
  }

}
