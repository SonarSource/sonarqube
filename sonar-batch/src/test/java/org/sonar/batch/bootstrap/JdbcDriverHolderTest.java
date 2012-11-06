/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JdbcDriverHolderTest {

  ClassLoader initialThreadClassloader;

  @Before
  public void before() {
    initialThreadClassloader = Thread.currentThread().getContextClassLoader();
  }

  @After
  public void after() {
    Thread.currentThread().setContextClassLoader(initialThreadClassloader);
  }

  @Test
  public void should_extend_classloader_with_jdbc_driver() throws Exception {
    /* jdbc-driver.jar has just one file /foo/foo.txt */
    assertThat(Thread.currentThread().getContextClassLoader().getResource("foo/foo.txt")).isNull();

    File fakeDriver = new File(getClass().getResource("/org/sonar/batch/bootstrap/JdbcDriverHolderTest/jdbc-driver.jar").toURI());
    TempDirectories tempDirectories = mock(TempDirectories.class);
    when(tempDirectories.getRoot()).thenReturn(fakeDriver.getParentFile());
    ServerClient server = mock(ServerClient.class);

    JdbcDriverHolder holder = new JdbcDriverHolder(new Settings(), tempDirectories, server);
    holder.start();

    verify(server).download("/deploy/jdbc-driver.jar", fakeDriver);
    assertThat(holder.getClassLoader().getResource("foo/foo.txt")).isNotNull();
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(holder.getClassLoader());
    assertThat(holder.getClassLoader().getParent()).isSameAs(getClass().getClassLoader());

    holder.stop();
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(getClass().getClassLoader());
    assertThat(holder.getClassLoader()).isNull();
  }

  @Test
  public void should_be_disabled_if_dry_run() {
    Settings settings = new Settings().setProperty(CoreProperties.DRY_RUN, true);
    ServerClient server = mock(ServerClient.class);
    JdbcDriverHolder holder = new JdbcDriverHolder(settings, mock(TempDirectories.class), server);

    holder.start();

    assertThat(holder.getClassLoader()).isNull();
    verifyZeroInteractions(server);

    // no error during stop
    holder.stop();
  }
}
