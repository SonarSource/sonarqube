/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.ServerMetadata;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class ArtifactDownloaderTest {

  @Test
  public void shouldDownloadJdbcDriver() throws IOException, URISyntaxException {
    ServerMetadata server = mock(ServerMetadata.class);
    when(server.getURL()).thenReturn("http://sonar:8000");

    HttpDownloader httpDownloader = mock(HttpDownloader.class);
    TempDirectories workingDirectories = new TempDirectories();

    ArtifactDownloader downloader = new ArtifactDownloader(httpDownloader, workingDirectories, server);
    File jdbcDriver = downloader.downloadJdbcDriver();

    assertNotNull(jdbcDriver);
    verify(httpDownloader).download(new URI("http://sonar:8000/deploy/jdbc-driver.jar"), jdbcDriver);
  }

  @Test
  public void shouldDownloadExtension() throws IOException, URISyntaxException {
    ServerMetadata server = mock(ServerMetadata.class);
    when(server.getURL()).thenReturn("http://sonar:8000");

    HttpDownloader httpDownloader = mock(HttpDownloader.class);
    TempDirectories workingDirectories = new TempDirectories();

    ArtifactDownloader downloader = new ArtifactDownloader(httpDownloader, workingDirectories, server);
    JpaPluginFile extension = new JpaPluginFile(new JpaPlugin("findbugs"), "bcel.jar");
    File bcel = downloader.downloadExtension(extension);

    assertNotNull(bcel);
    verify(httpDownloader).download(new URI("http://sonar:8000/deploy/plugins/findbugs/bcel.jar"), bcel);
  }
}
