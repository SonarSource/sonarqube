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
package org.sonar.batch.bootstrap;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.home.cache.FileCache;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JdbcDriverHolderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ClassLoader initialThreadClassloader;
  private AnalysisMode mode;

  @Before
  public void before() {
    initialThreadClassloader = Thread.currentThread().getContextClassLoader();
    mode = mock(AnalysisMode.class);
  }

  @After
  public void after() {
    Thread.currentThread().setContextClassLoader(initialThreadClassloader);
  }

  @Test
  public void should_extend_classloader_with_jdbc_driver() throws Exception {
    FileCache cache = mock(FileCache.class);

    File fakeDriver = new File(getClass().getResource("/org/sonar/batch/bootstrap/JdbcDriverHolderTest/jdbc-driver.jar").toURI());
    when(cache.get(eq("ojdbc14.jar"), eq("fakemd5"), any(FileCache.Downloader.class))).thenReturn(fakeDriver);

    /* jdbc-driver.jar has just one file /foo/foo.txt */
    assertThat(Thread.currentThread().getContextClassLoader().getResource("foo/foo.txt")).isNull();

    ServerClient server = mock(ServerClient.class);
    when(server.request("/deploy/jdbc-driver.txt")).thenReturn("ojdbc14.jar|fakemd5");
    when(server.request("/deploy/ojdbc14.jar")).thenReturn("fakecontent");

    JdbcDriverHolder holder = new JdbcDriverHolder(cache, mode, server);
    holder.start();

    assertThat(holder.getClassLoader().getResource("foo/foo.txt")).isNotNull();
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(holder.getClassLoader());
    assertThat(holder.getClassLoader().getParent()).isSameAs(getClass().getClassLoader());

    holder.stop();
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(getClass().getClassLoader());
    assertThat(holder.getClassLoader()).isNull();
  }

  @Test
  public void should_be_disabled_if_preview() {
    FileCache cache = mock(FileCache.class);
    when(mode.isPreview()).thenReturn(true);
    ServerClient server = mock(ServerClient.class);
    JdbcDriverHolder holder = new JdbcDriverHolder(cache, mode, server);

    holder.start();

    assertThat(holder.getClassLoader()).isNull();
    verifyZeroInteractions(server);

    // no error during stop
    holder.stop();
  }
}
