/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.application;

import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class StartServerTest {

  Env env;
  StartServer starter;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void prepare_app() throws IOException {
    File confFile = new File("src/test/fake-app/conf/sonar.properties");
    if (!confFile.exists()) {
      confFile = new File("sonar-application/src/test/fake-app/conf/sonar.properties");
    }

    File rootDir = temp.newFolder();
    FileUtils.copyDirectory(confFile.getParentFile().getParentFile(), rootDir);
    env = new Env(new File(rootDir, "conf/sonar.properties"));
    starter = new StartServer(env);
  }

  @Test
  public void start_server() throws Exception {
    BackgroundThread background = new BackgroundThread(starter);
    int port = 0;
    try {
      background.start();
      boolean started = false;
      for (int i = 0; i < 400; i++) {
        // Waiting for server to be started.
        // A random and open port is used (see conf/sonar.properties)
        Thread.sleep(300L);
        if (verifyUp() && verifyLogs()) {
          port = starter.port();
          started = true;
          break;
        }
      }
      assertThat(started).isTrue();
    } finally {
      starter.stop();
    }

    // Server is down
    try {
      assertThat(HttpRequest.get("http://localhost:" + port).ok()).isFalse();
    } catch (HttpRequest.HttpRequestException e) {
      // ok
    }
  }

  private boolean verifyUp() {
    if (starter.port() > 0) {
      String url = "http://localhost:" + starter.port() + "/index.html";
      HttpRequest request = HttpRequest.get(url);
      if (request.ok() && "Hello World".equals(request.body(HttpRequest.CHARSET_UTF8))) {
        return true;
      }
    }
    return false;
  }

  private boolean verifyLogs() {
    File logFile = env.file("logs/access.log");
    return logFile.isFile() && logFile.exists() && logFile.length()>0;
  }

  @Test
  public void fail_if_started_twice() throws Exception {
    BackgroundThread background = new BackgroundThread(starter);
    try {
      background.start();
      boolean started = false;
      for (int i = 0; i < 100; i++) {
        // Waiting for server to be started.
        // A random and open port is used (see conf/sonar.properties)
        Thread.sleep(500L);
        if (starter.port() > 0) {
          try {
            starter.start();
            fail();
          } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Server is already started");
            started = true;
            break;
          }
        }
      }
      assertThat(started).isTrue();

    } finally {
      starter.stop();
    }
  }

  @Test
  public void ignore_stop_if_not_running() throws Exception {
    starter.stop();
    starter.stop();
  }

  static class BackgroundThread extends Thread {
    private StartServer server;

    BackgroundThread(StartServer server) {
      this.server = server;
    }

    @Override
    public void run() {
      try {
        server.start();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
