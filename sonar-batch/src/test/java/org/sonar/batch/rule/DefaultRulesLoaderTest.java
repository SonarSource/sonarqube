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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.rules.ExpectedException;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonarqube.ws.Rules.ListResponse.Rule;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class DefaultRulesLoaderTest {
  @org.junit.Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testParseServerResponse() throws IOException {
    WSLoader wsLoader = mock(WSLoader.class);
    InputStream is = Resources.asByteSource(this.getClass().getResource("DefaultRulesLoader/response.protobuf")).openBufferedStream();
    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<>(is, true));
    DefaultRulesLoader loader = new DefaultRulesLoader(wsLoader);
    List<Rule> ruleList = loader.load(null);
    assertThat(ruleList).hasSize(318);
  }

  @Test
  public void testLoadedFromCache() throws IOException {
    WSLoader wsLoader = mock(WSLoader.class);
    InputStream is = Resources.asByteSource(this.getClass().getResource("DefaultRulesLoader/response.protobuf")).openBufferedStream();
    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<>(is, true));
    DefaultRulesLoader loader = new DefaultRulesLoader(wsLoader);
    MutableBoolean fromCache = new MutableBoolean();
    loader.load(fromCache);

    assertThat(fromCache.booleanValue()).isTrue();
  }

  @Test
  public void testError() throws IOException {
    WSLoader wsLoader = mock(WSLoader.class);
    InputStream is = ByteSource.wrap(new String("trash").getBytes()).openBufferedStream();
    when(wsLoader.loadStream(anyString())).thenReturn(new WSLoaderResult<>(is, true));
    DefaultRulesLoader loader = new DefaultRulesLoader(wsLoader);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unable to get rules");

    loader.load(null);
  }

}
