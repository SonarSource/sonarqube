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
package org.sonar.scanner.issue.tracking;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.ScannerWsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultServerLineHashesLoaderTest {
  private ScannerWsClient wsClient;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    wsClient = mock(ScannerWsClient.class);
  }

  @Test
  public void should_download_source_from_ws_if_preview_mode() {
    WsTestUtil.mockReader(wsClient, new StringReader("ae12\n\n43fb"));
    ServerLineHashesLoader lastSnapshots = new DefaultServerLineHashesLoader(wsClient);

    String[] hashes = lastSnapshots.getLineHashes("myproject:org/foo/Bar.c");
    assertThat(hashes).containsOnly("ae12", "", "43fb");
    WsTestUtil.verifyCall(wsClient, "/api/sources/hash?key=myproject%3Aorg%2Ffoo%2FBar.c");
  }

  @Test
  public void should_download_source_with_space_from_ws_if_preview_mode() {
    WsTestUtil.mockReader(wsClient, new StringReader("ae12\n\n43fb"));
    ServerLineHashesLoader lastSnapshots = new DefaultServerLineHashesLoader(wsClient);

    String[] hashes = lastSnapshots.getLineHashes("myproject:org/foo/Foo Bar.c");
    assertThat(hashes).containsOnly("ae12", "", "43fb");
    WsTestUtil.verifyCall(wsClient, "/api/sources/hash?key=myproject%3Aorg%2Ffoo%2FFoo+Bar.c");
  }

  @Test
  public void should_fail_to_download_source_from_ws() throws URISyntaxException {
    WsTestUtil.mockException(wsClient, new HttpDownloader.HttpException(new URI(""), 500));
    ServerLineHashesLoader lastSnapshots = new DefaultServerLineHashesLoader(wsClient);

    thrown.expect(HttpDownloader.HttpException.class);
    lastSnapshots.getLineHashes("foo");
  }

}
