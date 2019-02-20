/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.es;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.es.newindex.FakeIndexDefinition;
import org.sonar.server.es.request.ProxyClusterHealthRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStateRequestBuilder;
import org.sonar.server.es.request.ProxyClusterStatsRequestBuilder;
import org.sonar.server.es.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.es.request.ProxyDeleteRequestBuilder;
import org.sonar.server.es.request.ProxyGetRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyNodesStatsRequestBuilder;
import org.sonar.server.es.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.es.request.ProxyRefreshRequestBuilder;
import org.sonar.server.es.request.ProxySearchRequestBuilder;
import org.sonar.server.es.request.ProxySearchScrollRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class EsClientTest {

  @Rule
  public EsTester es = EsTester.createCustom(new FakeIndexDefinition());

  @Test
  public void proxify_requests() {
    Index fakesIndex = Index.simple("fakes");
    IndexType.IndexMainType fakeMainType = IndexType.main(fakesIndex, "fake");

    EsClient underTest = es.client();
    assertThat(underTest.nativeClient()).isNotNull();
    assertThat(underTest.prepareClusterStats()).isInstanceOf(ProxyClusterStatsRequestBuilder.class);
    assertThat(underTest.prepareCreate(fakesIndex)).isInstanceOf(ProxyCreateIndexRequestBuilder.class);
    assertThat(underTest.prepareDelete(fakeMainType, "my_id")).isInstanceOf(ProxyDeleteRequestBuilder.class);
    assertThat(underTest.prepareIndicesExist(fakesIndex)).isInstanceOf(ProxyIndicesExistsRequestBuilder.class);
    assertThat(underTest.prepareGet(fakeMainType, "1")).isInstanceOf(ProxyGetRequestBuilder.class);
    assertThat(underTest.prepareHealth()).isInstanceOf(ProxyClusterHealthRequestBuilder.class);
    assertThat(underTest.prepareNodesStats()).isInstanceOf(ProxyNodesStatsRequestBuilder.class);
    assertThat(underTest.preparePutMapping(fakesIndex)).isInstanceOf(ProxyPutMappingRequestBuilder.class);
    assertThat(underTest.prepareRefresh(fakesIndex)).isInstanceOf(ProxyRefreshRequestBuilder.class);
    assertThat(underTest.prepareSearch(fakesIndex)).isInstanceOf(ProxySearchRequestBuilder.class);
    assertThat(underTest.prepareSearchScroll("1234")).isInstanceOf(ProxySearchScrollRequestBuilder.class);
    assertThat(underTest.prepareState()).isInstanceOf(ProxyClusterStateRequestBuilder.class);
    assertThat(underTest.prepareStats(fakesIndex)).isInstanceOf(ProxyIndicesStatsRequestBuilder.class);

    underTest.close();
  }
}
