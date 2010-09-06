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
package org.sonar.api.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.testing.ServletTester;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpDownloaderTest {

  private ServletTester tester;
  private String baseUrl;

  @Before
  public void startServer() throws Exception {
    tester = new ServletTester();
    tester.setContextPath("/");
    tester.addServlet(RedirectServlet.class, "/redirect/");
    tester.addServlet(FakeServlet.class, "/");
    baseUrl = tester.createSocketConnector(true);
    tester.start();
  }

  @After
  public void stopServer() throws Exception {
    tester.stop();
  }

  @Test
  public void downloadBytes() throws URISyntaxException {
    byte[] bytes = new HttpDownloader().download(new URI(baseUrl));
    assertThat(bytes.length, greaterThan(10));
  }

  @Test(expected=SonarException.class)
  public void failIfServerDown() throws URISyntaxException {
    new HttpDownloader().download(new URI("http://localhost:13579/unknown"));
  }

  @Test
  public void downloadToFile() throws URISyntaxException, IOException {
    File toDir = new File("target/test-tmp/org/sonar/api/utils/DownloaderTest/");
    FileUtils.forceMkdir(toDir);
    FileUtils.cleanDirectory(toDir);
    File toFile = new File(toDir, "downloadToFile.txt");

    new HttpDownloader().download(new URI(baseUrl), toFile);
    assertThat(toFile.exists(), is(true));
    assertThat(toFile.length(), greaterThan(10l));
  }

  @Test
  public void userAgentIsSonarVersion() throws URISyntaxException, IOException {
    Server server = mock(Server.class);
    when(server.getVersion()).thenReturn("2.2");

    byte[] bytes = new HttpDownloader(server).download(new URI(baseUrl));
    Properties props = new Properties();
    props.load(IOUtils.toInputStream(new String(bytes)));
    assertThat(props.getProperty("agent"), is("Sonar 2.2"));
  }

  @Test
  public void followRedirect() throws URISyntaxException {
    byte[] bytes = new HttpDownloader().download(new URI(baseUrl + "/redirect/"));
    assertThat(new String(bytes), containsString("count"));

  }
}
