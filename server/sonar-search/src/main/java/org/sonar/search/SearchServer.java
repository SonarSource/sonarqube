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
package org.sonar.search;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.internal.InternalNode;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;

public class SearchServer implements Monitored {

  private final SearchSettings settings;
  private InternalNode node;

  public SearchServer(Props props) {
    this.settings = new SearchSettings(props);
    new MinimumViableSystem()
      .checkJavaVersion()
      .checkWritableTempDir();
  }

  @Override
  public void start() {
    node = new InternalNode(settings.build(), false);
    node.start();
  }

  @Override
  public boolean isReady() {
    return node != null && node.client().admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(TimeValue.timeValueSeconds(30L))
      .get()
      .getStatus() != ClusterHealthStatus.RED;
  }

  @Override
  public void awaitStop() {
    while (node != null && !node.isClosed()) {
      try {
        Thread.sleep(200L);
      } catch (InterruptedException e) {
        // Ignore
      }
    }
  }

  @Override
  public void stop() {
    if (node != null && !node.isClosed()) {
      node.close();
    }
  }

  public static void main(String... args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    new SearchLogging().configure();
    SearchServer searchServer = new SearchServer(entryPoint.getProps());
    entryPoint.launch(searchServer);
  }
}
