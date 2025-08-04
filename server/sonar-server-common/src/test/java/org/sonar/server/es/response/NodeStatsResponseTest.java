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
package org.sonar.server.es.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeStatsResponseTest {
  private static final String EXAMPLE_JSON = "{" +
    "  \"nodes\": {" +
    "    \"YnKPZcbGRamRQGxjErLWoQ\": {" +
    "      \"name\": \"sonarqube\"," +
    "      \"host\": \"127.0.0.1\"," +
    "      \"indices\": {" +
    "        \"docs\": {" +
    "          \"count\": 13557" +
    "        }," +
    "        \"store\": {" +
    "          \"size_in_bytes\": 8670970" +
    "        }," +
    "        \"query_cache\": {" +
    "          \"memory_size_in_bytes\": 0" +
    "        }," +
    "        \"fielddata\": {" +
    "          \"memory_size_in_bytes\": 4880" +
    "        }," +
    "        \"translog\": {" +
    "          \"size_in_bytes\": 8274137" +
    "        }," +
    "        \"request_cache\": {" +
    "          \"memory_size_in_bytes\": 0" +
    "        }" +
    "      }," +
    "      \"process\": {" +
    "        \"open_file_descriptors\": 296," +
    "        \"max_file_descriptors\": 10240," +
    "        \"cpu\": {" +
    "          \"percent\": 7" +
    "        }" +
    "      }," +
    "      \"jvm\": {" +
    "        \"mem\": {" +
    "          \"heap_used_in_bytes\": 158487160," +
    "          \"heap_used_percent\": 30," +
    "          \"heap_max_in_bytes\": 518979584," +
    "          \"non_heap_used_in_bytes\": 109066592" +
    "        }," +
    "        \"threads\": {" +
    "          \"count\": 70" +
    "        }" +
    "      }," +
    "      \"fs\": {" +
    "        \"total\": {" +
    "          \"total_in_bytes\": 250685575168," +
    "          \"free_in_bytes\": 142843138048," +
    "          \"available_in_bytes\": 136144027648" +
    "        }" +
    "      }," +
    "      \"breakers\": {" +
    "        \"request\": {" +
    "          \"limit_size_in_bytes\": 311387750," +
    "          \"estimated_size_in_bytes\": 1" +
    "        }," +
    "        \"fielddata\": {" +
    "          \"limit_size_in_bytes\": 207591833," +
    "          \"estimated_size_in_bytes\": 4880" +
    "        }" +
    "      }" +
    "    }" +
    "  }" +
    "}";

  @Test
  public void should_parse_example_json() {
    JsonObject jsonObject = getExampleAsJsonObject();
    NodeStatsResponse nodeStatsResponse = NodeStatsResponse.toNodeStatsResponse(jsonObject);

    assertThat(nodeStatsResponse.getNodeStats()).hasSize(1);

    NodeStats nodeStats = nodeStatsResponse.getNodeStats().get(0);
    assertThat(nodeStats.getName()).isEqualTo("sonarqube");
    assertThat(nodeStats.getHost()).isEqualTo("127.0.0.1");
    assertThat(nodeStats.getCpuUsage()).isEqualTo(7);

    assertThat(nodeStats.getOpenFileDescriptors()).isEqualTo(296);
    assertThat(nodeStats.getMaxFileDescriptors()).isEqualTo(10240);
    assertThat(nodeStats.getDiskAvailableBytes()).isEqualTo(136144027648L);
    assertThat(nodeStats.getDiskTotalBytes()).isEqualTo(250685575168L);

    assertThat(nodeStats.getFieldDataCircuitBreakerLimit()).isEqualTo(207591833);
    assertThat(nodeStats.getFieldDataCircuitBreakerEstimation()).isEqualTo(4880);
    assertThat(nodeStats.getRequestCircuitBreakerLimit()).isEqualTo(311387750L);
    assertThat(nodeStats.getRequestCircuitBreakerEstimation()).isOne();

    JvmStats jvmStats = nodeStats.getJvmStats();
    assertThat(jvmStats).isNotNull();
    assertThat(jvmStats.getHeapUsedPercent()).isEqualTo(30);
    assertThat(jvmStats.getHeapUsedInBytes()).isEqualTo(158487160);
    assertThat(jvmStats.getHeapMaxInBytes()).isEqualTo(518979584);
    assertThat(jvmStats.getNonHeapUsedInBytes()).isEqualTo(109066592);
    assertThat(jvmStats.getThreadCount()).isEqualTo(70);

    IndicesStats indicesStats = nodeStats.getIndicesStats();
    assertThat(indicesStats).isNotNull();
    assertThat(indicesStats.getStoreSizeInBytes()).isEqualTo(8670970);
    assertThat(indicesStats.getTranslogSizeInBytes()).isEqualTo(8274137);
    assertThat(indicesStats.getRequestCacheMemorySizeInBytes()).isZero();
    assertThat(indicesStats.getFieldDataMemorySizeInBytes()).isEqualTo(4880);
    assertThat(indicesStats.getQueryCacheMemorySizeInBytes()).isZero();

  }

  private static JsonObject getExampleAsJsonObject() {
    return new Gson().fromJson(EXAMPLE_JSON, JsonObject.class);
  }
}
