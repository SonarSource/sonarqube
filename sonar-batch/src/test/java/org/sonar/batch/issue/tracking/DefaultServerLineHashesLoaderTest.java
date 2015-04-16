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
package org.sonar.batch.issue.tracking;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.api.utils.HttpDownloader;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultServerLineHashesLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
  }

  @Test
  public void should_download_source_from_ws_if_preview_mode() {
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString())).thenReturn("ae12\n\n43fb");

    ServerLineHashesLoader lastSnapshots = new DefaultServerLineHashesLoader(server);

    String[] hashes = lastSnapshots.getLineHashes("myproject:org/foo/Bar.c");
    assertThat(hashes).containsOnly("ae12", "", "43fb");
    verify(server).request("/api/sources/hash?key=myproject%3Aorg%2Ffoo%2FBar.c");
  }

  @Test
  public void should_download_source_with_space_from_ws_if_preview_mode() {
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString())).thenReturn("ae12\n\n43fb");

    ServerLineHashesLoader lastSnapshots = new DefaultServerLineHashesLoader(server);

    String[] hashes = lastSnapshots.getLineHashes("myproject:org/foo/Foo Bar.c");
    assertThat(hashes).containsOnly("ae12", "", "43fb");
    verify(server).request("/api/sources/hash?key=myproject%3Aorg%2Ffoo%2FFoo+Bar.c");
  }

  @Test
  public void should_fail_to_download_source_from_ws() throws URISyntaxException {
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString())).thenThrow(new HttpDownloader.HttpException(new URI(""), 500));

    ServerLineHashesLoader lastSnapshots = new DefaultServerLineHashesLoader(server);

    thrown.expect(HttpDownloader.HttpException.class);
    lastSnapshots.getLineHashes("foo");
  }

}
