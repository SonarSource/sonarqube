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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.es.request.ProxyBulkRequestBuilder;
import org.sonar.server.es.request.ProxyClusterHealthRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStateRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStatsRequestBuilder;
import org.sonar.server.es.request.ProxyCountRequestBuilder;
import org.sonar.server.es.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.es.request.ProxyDeleteByQueryRequestBuilder;
import org.sonar.server.es.request.ProxyDeleteRequestBuilder;
import org.sonar.server.es.request.ProxyFlushRequestBuilder;
import org.sonar.server.es.request.ProxyGetRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyMultiGetRequestBuilder;
import org.sonar.server.es.request.ProxyNodesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.es.request.ProxyRefreshRequestBuilder;
import org.sonar.server.es.request.ProxySearchRequestBuilder;
import org.sonar.server.es.request.ProxySearchScrollRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class EsClientTest {

  @Rule
  public EsTester es = new EsTester();

  @Test
  public void proxify_requests() {
    EsClient client = es.client();
    client.start();
    assertThat(client.nativeClient()).isNotNull();
    assertThat(client.prepareBulk()).isInstanceOf(ProxyBulkRequestBuilder.class);
    assertThat(client.prepareClusterStats()).isInstanceOf(ProxyClusterStatsRequestBuilder.class);
    assertThat(client.prepareCount()).isInstanceOf(ProxyCountRequestBuilder.class);
    assertThat(client.prepareCreate("fakes")).isInstanceOf(ProxyCreateIndexRequestBuilder.class);
    assertThat(client.prepareDeleteByQuery()).isInstanceOf(ProxyDeleteByQueryRequestBuilder.class);
    assertThat(client.prepareDelete("fakes", "fake", "my_id")).isInstanceOf(ProxyDeleteRequestBuilder.class);
    assertThat(client.prepareIndicesExist()).isInstanceOf(ProxyIndicesExistsRequestBuilder.class);
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
