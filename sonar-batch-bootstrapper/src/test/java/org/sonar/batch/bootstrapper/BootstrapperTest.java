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
package org.sonar.batch.bootstrapper;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class BootstrapperTest {

  @Test
  public void shouldRemoveLastUrlSlash() {
    Bootstrapper bootstrapper = new Bootstrapper("", "http://test/", new File("target"));
    assertThat(bootstrapper.getServerUrl(), is("http://test"));
  }

  @Test(expected = Exception.class)
  public void shouldFailIfCanNotConnectServer() {
    Bootstrapper bootstrapper = new Bootstrapper("", "http://unknown.foo", new File("target"));
    bootstrapper.getServerVersion();
  }

  @Test
  public void shouldReturnUserAgent() {
    Bootstrapper bootstrapper = new Bootstrapper("test/0.1", "http://unknown.foo", new File("target"));
    String userAgent = bootstrapper.getUserAgent();
    assertThat(userAgent.length(), greaterThan(0));
    assertThat(userAgent, allOf(startsWith("sonar-bootstrapper/"), endsWith(" test/0.1")));
  }

  @Test
  public void shouldReturnValidVersion() {
    Bootstrapper bootstrapper = new Bootstrapper("", "http://test", new File("target")) {
      @Override
      String remoteContent(String path) throws IOException {
        return "2.6";
      }
    };
    assertThat(bootstrapper.getServerVersion(), is("2.6"));
  }

}
