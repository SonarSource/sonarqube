/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterStatsResponseTest {
  private static final String EXAMPLE_JSON = "{" +
    "  \"status\": \"yellow\"," +
    "  \"nodes\": {" +
    "    \"count\": {" +
    "      \"total\": 3" +
    "    }" +
    "  }" +
    "}";

  @Test
  public void should_parse_example_json() {
    JsonObject jsonObject = getExampleAsJsonObject();
    ClusterStatsResponse clusterStatsResponse = ClusterStatsResponse.toClusterStatsResponse(jsonObject);

    assertThat(clusterStatsResponse.getHealthStatus()).isEqualTo(ClusterHealthStatus.YELLOW);
    assertThat(clusterStatsResponse.getNodeCount()).isEqualTo(3);
  }

  private static JsonObject getExampleAsJsonObject() {
    return new Gson().fromJson(EXAMPLE_JSON, JsonObject.class);
  }

}
