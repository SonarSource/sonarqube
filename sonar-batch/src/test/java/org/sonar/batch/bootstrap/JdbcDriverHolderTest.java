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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;

public class JdbcDriverHolderTest {

  private ClassLoader initialThreadClassloader;

  @Before
  public void before() {
    initialThreadClassloader = Thread.currentThread().getContextClassLoader();
  }

  @After
  public void after() {
    Thread.currentThread().setContextClassLoader(initialThreadClassloader);
  }

  @Test
  public void should_extend_classloader_with_jdbc_driver() throws URISyntaxException {
    /* foo.jar has just one file /foo/foo.txt */
    assertThat(getClass().getClassLoader().getResource("foo/foo.txt")).isNull();

    URL url = getClass().getResource("/org/sonar/batch/bootstrap/JdbcDriverHolderTest/foo.jar");
    JdbcDriverHolder.JdbcDriverClassLoader classloader = JdbcDriverHolder.initClassloader(new File(url.toURI()));
    assertThat(classloader).isNotNull();
    assertThat(classloader.getResource("foo/foo.txt")).isNotNull();
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classloader);
  }

}
