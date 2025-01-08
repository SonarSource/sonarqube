/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import org.elasticsearch.ElasticsearchException;
import org.sonar.api.server.ServerSide;
import org.slf4j.LoggerFactory;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.IndexStats;
import org.sonar.server.es.response.IndicesStatsResponse;

import static org.sonar.core.util.FileUtils.humanReadableByteCountSI;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class EsIndexesSection implements SystemInfoSection, Global {

  private final EsClient esClient;

  public EsIndexesSection(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Search Indexes");
    try {
      completeIndexAttributes(protobuf);
    } catch (Exception es) {
      LoggerFactory.getLogger(EsIndexesSection.class).warn("Failed to retrieve ES attributes. There will be only a single \"Error\" attribute.", es);
      setAttribute(protobuf, "Error", es.getCause() instanceof ElasticsearchException ? es.getCause().getMessage() : es.getMessage());
    }
    return protobuf.build();
  }

  private void completeIndexAttributes(ProtobufSystemInfo.Section.Builder protobuf) {
    IndicesStatsResponse indicesStats = esClient.indicesStats();
    for (IndexStats indexStats : indicesStats.getAllIndexStats()) {
      String prefix = "Index " + indexStats.getName() + " - ";
      setAttribute(protobuf, prefix + "Docs", indexStats.getDocCount());
      setAttribute(protobuf, prefix + "Shards", indexStats.getShardsCount());
      setAttribute(protobuf, prefix + "Store Size", humanReadableByteCountSI(indexStats.getStoreSizeBytes()));
    }
  }
}
