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
package org.sonar.scanner.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerIssuesLoaderTest {
  private DefaultServerIssuesLoader loader;
  private ScannerWsClient wsClient;

  @Before
  public void prepare() {
    wsClient = mock(ScannerWsClient.class);
    loader = new DefaultServerIssuesLoader(wsClient);
  }

  @Test
  public void loadFromWs() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    ServerIssue.newBuilder().setKey("ab1").build()
      .writeDelimitedTo(bos);
    ServerIssue.newBuilder().setKey("ab2").build()
      .writeDelimitedTo(bos);

    InputStream is = new ByteArrayInputStream(bos.toByteArray());
    WsTestUtil.mockStream(wsClient, "/batch/issues.protobuf?key=foo", is);

    final List<ServerIssue> result = new ArrayList<>();
    loader.load("foo", issue -> {
      result.add(issue);
    });

    assertThat(result).extracting("key").containsExactly("ab1", "ab2");
  }

  @Test(expected = IllegalStateException.class)
  public void testError() throws IOException {
    InputStream is = mock(InputStream.class);
    when(is.read()).thenThrow(IOException.class);
    WsTestUtil.mockStream(wsClient, "/batch/issues.protobuf?key=foo", is);
    loader.load("foo", mock(Consumer.class));
  }
}
