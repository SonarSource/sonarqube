/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.HttpDownloader;

public class FindbugsDownloaderTest {

  private FindbugsDownloader downloader;

  @Before
  public void setUp() {
    Server server = mock(Server.class);
    when(server.getURL()).thenReturn("http://sonar");

    HttpDownloader httpDownloader = mock(HttpDownloader.class);
    downloader = new FindbugsDownloader(server, httpDownloader);
  }

  @Test
  public void testUrls() {
    assertThat(downloader.getUrlForAnnotationsJar(), startsWith("http://sonar/deploy/plugins/findbugs/annotations-"));
    assertThat(downloader.getUrlForJsrJar(), startsWith("http://sonar/deploy/plugins/findbugs/jsr305-"));
  }

  @Test
  public void shouldCreateTempFile() {
    File file = downloader.downloadLib("http://sonar/test.jar");
    assertThat(file.exists(), is(true));
  }
}
