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
package org.sonar.server.es;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.search.request.ProxyBulkRequestBuilder;
import org.sonar.server.search.request.ProxyClusterHealthRequestBuilder;
import org.sonar.server.search.request.ProxyClusterStateRequestBuilder;
import org.sonar.server.search.request.ProxyClusterStatsRequestBuilder;
import org.sonar.server.search.request.ProxyCountRequestBuilder;
import org.sonar.server.search.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.search.request.ProxyDeleteByQueryRequestBuilder;
import org.sonar.server.search.request.ProxyDeleteRequestBuilder;
import org.sonar.server.search.request.ProxyFlushRequestBuilder;
import org.sonar.server.search.request.ProxyGetRequestBuilder;
import org.sonar.server.search.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.search.request.ProxyIndicesStatsRequestBuilder;
import org.sonar.server.search.request.ProxyMultiGetRequestBuilder;
import org.sonar.server.search.request.ProxyNodesStatsRequestBuilder;
import org.sonar.server.search.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.search.request.ProxyRefreshRequestBuilder;
import org.sonar.server.search.request.ProxySearchRequestBuilder;
import org.sonar.server.search.request.ProxySearchScrollRequestBuilder;

import static org.fest.assertions.Assertions.assertThat;

@Ignore
public class EsClientTest {

  @Rule
  public EsTester es = new EsTester();

  @Test
  public void proxify_requests() throws Exception {
    EsClient client = es.client();
    client.start();
    assertThat(client.nativeClient()).isNotNull();
    assertThat(client.getClusterHealth().isClusterAvailable()).isTrue();
    assertThat(client.prepareBulk()).isInstanceOf(ProxyBulkRequestBuilder.class);
    assertThat(client.prepareClusterStats()).isInstanceOf(ProxyClusterStatsRequestBuilder.class);
    assertThat(client.prepareCount()).isInstanceOf(ProxyCountRequestBuilder.class);
    assertThat(client.prepareCreate("fakes")).isInstanceOf(ProxyCreateIndexRequestBuilder.class);
    assertThat(client.prepareDeleteByQuery()).isInstanceOf(ProxyDeleteByQueryRequestBuilder.class);
    assertThat(client.prepareDelete("fakes", "fake", "my_id")).isInstanceOf(ProxyDeleteRequestBuilder.class);
    assertThat(client.prepareExists()).isInstanceOf(ProxyIndicesExistsRequestBuilder.class);
    assertThat(client.prepareFlush()).isInstanceOf(ProxyFlushRequestBuilder.class);
    assertThat(client.prepareGet()).isInstanceOf(ProxyGetRequestBuilder.class);
    assertThat(client.prepareGet("fakes", "fake", "1")).isInstanceOf(ProxyGetRequestBuilder.class);
    assertThat(client.prepareHealth()).isInstanceOf(ProxyClusterHealthRequestBuilder.class);
    assertThat(client.prepareMultiGet()).isInstanceOf(ProxyMultiGetRequestBuilder.class);
    assertThat(client.prepareNodesStats()).isInstanceOf(ProxyNodesStatsRequestBuilder.class);
    assertThat(client.preparePutMapping()).isInstanceOf(ProxyPutMappingRequestBuilder.class);
    assertThat(client.prepareRefresh()).isInstanceOf(ProxyRefreshRequestBuilder.class);
    assertThat(client.prepareSearch()).isInstanceOf(ProxySearchRequestBuilder.class);
    assertThat(client.prepareSearchScroll("1234")).isInstanceOf(ProxySearchScrollRequestBuilder.class);
    assertThat(client.prepareState()).isInstanceOf(ProxyClusterStateRequestBuilder.class);
    assertThat(client.prepareStats()).isInstanceOf(ProxyIndicesStatsRequestBuilder.class);

    client.stop();
  }
}
