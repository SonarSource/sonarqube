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
package org.sonar.batch.rule;

import org.sonar.api.rule.RuleKey;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.Before;

public class DefaultActiveRulesLoaderTest {
  private DefaultActiveRulesLoader loader;
  private WSLoader ws;

  @Before
  public void setUp() {
    ws = mock(WSLoader.class);
    loader = new DefaultActiveRulesLoader(ws);
  }

  @Test
  public void feed_real_response_encode_qp() throws IOException {
    InputStream response1 = loadResource("active_rule_search1.protobuf");
    InputStream response2 = loadResource("active_rule_search2.protobuf");

    String req1 = "/api/rules/search.protobuf?f=repo,name,severity,lang,internalKey,templateKey,params,actives&activation=true&qprofile=c%2B-test_c%2B-values-17445&p=1&ps=500";
    String req2 = "/api/rules/search.protobuf?f=repo,name,severity,lang,internalKey,templateKey,params,actives&activation=true&qprofile=c%2B-test_c%2B-values-17445&p=2&ps=500";
    when(ws.loadStream(req1)).thenReturn(new WSLoaderResult<InputStream>(response1, false));
    when(ws.loadStream(req2)).thenReturn(new WSLoaderResult<InputStream>(response2, false));

    Collection<LoadedActiveRule> activeRules = loader.load("c+-test_c+-values-17445", null);
    assertThat(activeRules).hasSize(226);
    assertActiveRule(activeRules);
    
    verify(ws).loadStream(req1);
    verify(ws).loadStream(req2);
    verifyNoMoreInteractions(ws);
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
