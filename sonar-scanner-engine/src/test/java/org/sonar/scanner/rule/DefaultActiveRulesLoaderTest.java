/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.rule;

import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.BatchWsClient;
import org.sonar.scanner.rule.DefaultActiveRulesLoader;
import org.sonar.scanner.rule.LoadedActiveRule;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.junit.Before;

public class DefaultActiveRulesLoaderTest {
  private DefaultActiveRulesLoader loader;
  private BatchWsClient wsClient;

  @Before
  public void setUp() {
    wsClient = mock(BatchWsClient.class);
    loader = new DefaultActiveRulesLoader(wsClient);
  }

  @Test
  public void feed_real_response_encode_qp() throws IOException {
    InputStream response1 = loadResource("active_rule_search1.protobuf");
    InputStream response2 = loadResource("active_rule_search2.protobuf");

    String req1 = "/api/rules/search.protobuf?f=repo,name,severity,lang,internalKey,templateKey,params,actives&activation=true&qprofile=c%2B-test_c%2B-values-17445&p=1&ps=500";
    String req2 = "/api/rules/search.protobuf?f=repo,name,severity,lang,internalKey,templateKey,params,actives&activation=true&qprofile=c%2B-test_c%2B-values-17445&p=2&ps=500";
    WsTestUtil.mockStream(wsClient, req1, response1);
    WsTestUtil.mockStream(wsClient, req2, response2);

    Collection<LoadedActiveRule> activeRules = loader.load("c+-test_c+-values-17445");
    assertThat(activeRules).hasSize(226);
    assertActiveRule(activeRules);

    WsTestUtil.verifyCall(wsClient, req1);
    WsTestUtil.verifyCall(wsClient, req2);

    verifyNoMoreInteractions(wsClient);
  }

  private static void assertActiveRule(Collection<LoadedActiveRule> activeRules) {
    RuleKey key = RuleKey.of("squid", "S3008");
    for (LoadedActiveRule r : activeRules) {
      if (!r.getRuleKey().equals(key)) {
        continue;
      }

      assertThat(r.getParams().get("format")).isEqualTo("^[a-z][a-zA-Z0-9]*$");
      assertThat(r.getSeverity()).isEqualTo("MINOR");
    }
  }

  private InputStream loadResource(String name) throws IOException {
    return Resources.asByteSource(this.getClass().getResource("DefaultActiveRulesLoaderTest/" + name))
      .openBufferedStream();
  }

}
