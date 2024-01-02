/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.application.es;

import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EsConnectorImplTest {

  private static final String JSON_SUCCESS_RESPONSE = "{" +
    "  \"cluster_name\" : \"testcluster\"," +
    "  \"status\" : \"yellow\"," +
    "  \"timed_out\" : false," +
    "  \"number_of_nodes\" : 1," +
    "  \"number_of_data_nodes\" : 1," +
    "  \"active_primary_shards\" : 1," +
    "  \"active_shards\" : 1," +
    "  \"relocating_shards\" : 0," +
    "  \"initializing_shards\" : 0," +
    "  \"unassigned_shards\" : 1," +
    "  \"delayed_unassigned_shards\": 0," +
    "  \"number_of_pending_tasks\" : 0," +
    "  \"number_of_in_flight_fetch\": 0," +
    "  \"task_max_waiting_in_queue_millis\": 0," +
    "  \"active_shards_percent_as_number\": 50.0" +
    "}";

  private static final String JSON_ERROR_RESPONSE = "{" +
    "  \"error\" : \"i-have-a-bad-feelings-about-this\"" +
    "}";

  private static final String ES_INFO_RESPONSE = "{"
    + "  \"name\" : \"sonarqube\","
    + "  \"cluster_name\" : \"sonarqube\","
    + "  \"cluster_uuid\" : \"6Oj9lFIyQVa_d5HgQWqQpA\","
    + "  \"version\" : {"
    + "    \"number\" : \"7.14.1\","
    + "    \"build_flavor\" : \"default\","
    + "    \"build_type\" : \"tar\","
    + "    \"build_hash\" : \"66b55ebfa59c92c15db3f69a335d500018b3331e\","
    + "    \"build_date\" : \"2021-08-26T09:01:05.390870785Z\","
    + "    \"build_snapshot\" : false,"
    + "    \"lucene_version\" : \"8.9.0\","
    + "    \"minimum_wire_compatibility_version\" : \"6.8.0\","
    + "    \"minimum_index_compatibility_version\" : \"6.0.0-beta1\""
    + "  },"
    + "  \"tagline\" : \"You Know, for Search\""
    + "}";

  @Rule
  public MockWebServer mockWebServer = new MockWebServer();

  EsConnectorImpl underTest = new EsConnectorImpl(Sets.newHashSet(HostAndPort.fromParts(mockWebServer.getHostName(), mockWebServer.getPort())), null);

  @After
  public void after() {
    underTest.stop();
  }

  @Test
  public void should_rethrow_if_es_exception() {
    mockServerResponse(500, JSON_ERROR_RESPONSE);

    assertThatThrownBy(() -> underTest.getClusterHealthStatus())
      .isInstanceOf(ElasticsearchStatusException.class);
  }

  @Test
  public void should_return_status() {
    mockServerResponse(200, JSON_SUCCESS_RESPONSE);

    assertThat(underTest.getClusterHealthStatus())
      .hasValue(ClusterHealthStatus.YELLOW);
  }

  @Test
  public void should_add_authentication_header() throws InterruptedException {
    mockServerResponse(200, JSON_SUCCESS_RESPONSE);
    String password = "test-password";

    EsConnectorImpl underTest = new EsConnectorImpl(Sets.newHashSet(HostAndPort.fromParts(mockWebServer.getHostName(), mockWebServer.getPort())), password);

    assertThat(underTest.getClusterHealthStatus())
      .hasValue(ClusterHealthStatus.YELLOW);
    assertThat(mockWebServer.takeRequest().getHeader("Authorization")).isEqualTo("Basic ZWxhc3RpYzp0ZXN0LXBhc3N3b3Jk");
  }

  private void mockServerResponse(int httpCode, String jsonResponse) {
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
      .setBody(ES_INFO_RESPONSE)
      .setHeader("Content-Type", "application/json")
      .setHeader("X-elastic-product", "Elasticsearch"));
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(httpCode)
      .setBody(jsonResponse)
      .setHeader("Content-Type", "application/json")
      .setHeader("X-elastic-product", "Elasticsearch"));
  }

}
