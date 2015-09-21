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

import org.sonarqube.ws.Rules.Rule;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.mockito.Mockito.verify;

import static org.mockito.Matchers.anyString;
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
  public void feed_real_response() throws IOException {
    InputStream response = loadResource("active_rule_search.protobuf");
    when(ws.loadStream(anyString())).thenReturn(new WSLoaderResult<InputStream>(response, false));
    Collection<Rule> activeRules = loader.load("java-sonar-way-26368", null);
    assertThat(activeRules).hasSize(100);

    verify(ws).loadStream("/api/rules/search?f=repo,name,severity,lang,internalKey,templateKey&qprofile=java-sonar-way-26368");
    verifyNoMoreInteractions(ws);

  }

  private InputStream loadResource(String name) throws IOException {
    return Resources.asByteSource(this.getClass().getResource("DefaultActiveRulesLoaderTest/" + name))
      .openBufferedStream();
  }

}
