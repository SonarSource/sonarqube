/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.rule;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.Rules.ListResponse.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultRulesLoaderTest {
  @org.junit.Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testParseServerResponse() throws IOException {
    ScannerWsClient wsClient = mock(ScannerWsClient.class);
    InputStream is = Resources.asByteSource(this.getClass().getResource("DefaultRulesLoaderTest/response.protobuf")).openBufferedStream();
    WsTestUtil.mockStream(wsClient, is);
    DefaultRulesLoader loader = new DefaultRulesLoader(wsClient);
    List<Rule> ruleList = loader.load();
    assertThat(ruleList).hasSize(318);
  }

  @Test
  public void testError() throws IOException {
    ScannerWsClient wsClient = mock(ScannerWsClient.class);
    InputStream is = ByteSource.wrap(new String("trash").getBytes()).openBufferedStream();
    WsTestUtil.mockStream(wsClient, is);
    DefaultRulesLoader loader = new DefaultRulesLoader(wsClient);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unable to get rules");

    loader.load();
  }
}
