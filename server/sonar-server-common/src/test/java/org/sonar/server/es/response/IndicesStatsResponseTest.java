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
import java.util.Collection;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IndicesStatsResponseTest {
  private static final String EXAMPLE_JSON = "{" +
    "  \"indices\": {" +
    "    \"index-1\": {" +
    "      \"primaries\": {" +
    "        \"docs\": {" +
    "          \"count\": 1234" +
    "        }," +
    "        \"store\": {" +
    "          \"size_in_bytes\": 56789" +
    "        }" +
    "      }," +
    "      \"shards\": {" +
    "        \"shard-1\": {}," +
    "        \"shard-2\": {}" +
    "      }" +
    "    }," +
    "    \"index-2\": {" +
    "      \"primaries\": {" +
    "        \"docs\": {" +
    "          \"count\": 42" +
    "        }," +
    "        \"store\": {" +
    "          \"size_in_bytes\": 123" +
    "        }" +
    "      }," +
    "      \"shards\": {" +
    "        \"shard-1\": {}," +
    "        \"shard-2\": {}" +
    "      }" +
    "    }" +
    "  }" +
    "}";

  @Test
  public void should_parse_example_json() {
    JsonObject jsonObject = getExampleAsJsonObject();
    IndicesStatsResponse indicesStatsResponse = IndicesStatsResponse.toIndicesStatsResponse(jsonObject);

    Collection<IndexStats> allIndexStats = indicesStatsResponse.getAllIndexStats();
    assertThat(allIndexStats)
      .hasSize(2)
      .extracting("name")
      .contains("index-1", "index-2");

    IndexStats indexStats = allIndexStats.stream().filter(i -> i.getName().equals("index-1")).findFirst().get();
    assertThat(indexStats.getDocCount()).isEqualTo(1234);
    assertThat(indexStats.getShardsCount()).isEqualTo(2);
    assertThat(indexStats.getStoreSizeBytes()).isEqualTo(56789);

    indexStats = allIndexStats.stream().filter(i -> i.getName().equals("index-2")).findFirst().get();
    assertThat(indexStats.getDocCount()).isEqualTo(42);
    assertThat(indexStats.getStoreSizeBytes()).isEqualTo(123);
    assertThat(indexStats.getShardsCount()).isEqualTo(2);
  }

  private static JsonObject getExampleAsJsonObject() {
    return new Gson().fromJson(EXAMPLE_JSON, JsonObject.class);
  }

}
