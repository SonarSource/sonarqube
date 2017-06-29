/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.search;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.lucene.util.StringHelper;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;

public class SearchServer implements Monitored {
  // VisibleForTesting
  protected static Logger LOGGER = LoggerFactory.getLogger(SearchServer.class);

  private static final String MIMINUM_MASTER_NODES = "discovery.zen.minimum_master_nodes";
  private static final String INITIAL_STATE_TIMEOUT = "discovery.initial_state_timeout";
  private final EsSettings settings;
  Process p;

  public SearchServer(Props props) {
    this.settings = new EsSettings(props);
    new MinimumViableSystem()
      .checkWritableTempDir();
  }

  @Override
  public void start() {
    //Jmx.register(EsSettingsMBean.OBJECT_NAME, settings);
    initBootstrap();
    Settings esSettings = settings.build();
    if (esSettings.getAsInt(MIMINUM_MASTER_NODES, 1) >= 2) {
      LOGGER.info("Elasticsearch is waiting {} for {} node(s) to be up to start.",
        esSettings.get(INITIAL_STATE_TIMEOUT),
        esSettings.get(MIMINUM_MASTER_NODES));
    }

    try {
      ProcessBuilder builder = new ProcessBuilder("/Users/danielschwarz/SonarSource/batches/elasticsearch/elasticsearch-5.0.0/bin/elasticsearch")
        .directory(new File("/Users/danielschwarz/SonarSource/batches/elasticsearch/elasticsearch-5.0.0/bin/"));
      builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
      builder.redirectErrorStream(true);
      Process p = builder.start();
      p.destroy();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to start ES", e);
    }
    configureIndexDefaultSettings(settings);
  }

  private void configureIndexDefaultSettings(EsSettings settings) {
    Settings.Builder indexSettings = Settings.builder();
    settings.configureIndexDefaults(indexSettings);
//    node.client().admin().indices().putTemplate(new PutIndexTemplateRequest().settings(indexSettings));
  }

  // copied from https://github.com/elastic/elasticsearch/blob/v2.3.3/core/src/main/java/org/elasticsearch/bootstrap/Bootstrap.java
  private static void initBootstrap() {
    // init lucene random seed. it will use /dev/urandom where available:
    StringHelper.randomId();
  }

  @Override
  public Status getStatus() {
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try (TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)) {
      client
        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

      boolean esStatus = client.admin().cluster().prepareHealth()
        .setWaitForYellowStatus()
        .setTimeout(TimeValue.timeValueSeconds(30L))
        .get()
        .getStatus() != ClusterHealthStatus.RED;
      if (esStatus) {
        return Status.OPERATIONAL;
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
      // no action required
    }
    return Status.DOWN;
  }

  @Override
  public void awaitStop() {
    if (p != null) {
      p.destroy();
      try {
        p.waitFor();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  @Override
  public void stop() {
    if (p != null) {
      p.destroyForcibly();
    }

    //Jmx.unregister(EsSettingsMBean.OBJECT_NAME);
  }

  public static void main(String... args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    new SearchLogging().configure(entryPoint.getProps());
    SearchServer searchServer = new SearchServer(entryPoint.getProps());
    entryPoint.launch(searchServer);
  }
}
