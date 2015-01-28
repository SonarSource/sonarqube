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

import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.home.cache.FileCache;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class JdbcDriverHolderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ClassLoader initialThreadClassloader;
  private DefaultAnalysisMode mode;

  @Before
  public void before() {
    initialThreadClassloader = Thread.currentThread().getContextClassLoader();
    mode = mock(DefaultAnalysisMode.class);
  }

  @After
  public void after() {
    Thread.currentThread().setContextClassLoader(initialThreadClassloader);
  }

  @Test
  public void extend_classloader_with_jdbc_driver() throws Exception {
    FileCache cache = mock(FileCache.class);

    File fakeDriver = new File(Resources.getResource(JdbcDriverHolderTest.class, "JdbcDriverHolderTest/jdbc-driver.jar").getFile());
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
  public void do_nothing_when_jdbc_driver_file_is_empty() throws Exception {
    FileCache cache = mock(FileCache.class);

    ServerClient server = mock(ServerClient.class);
    when(server.request("/deploy/jdbc-driver.txt")).thenReturn("");

    JdbcDriverHolder holder = new JdbcDriverHolder(cache, mode, server);
    holder.start();

    verify(server, never()).download(anyString(), any(File.class));
    verifyZeroInteractions(cache);
    assertThat(holder.getClassLoader()).isNull();
  }

  @Test
  public void be_disabled_if_preview() {
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
