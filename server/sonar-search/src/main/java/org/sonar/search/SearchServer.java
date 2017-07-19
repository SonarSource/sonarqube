/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;

public class SearchServer implements Monitored {
  // VisibleForTesting
  protected static Logger LOGGER = LoggerFactory.getLogger(SearchServer.class);

  private final EsSettings settings;
  private Process p;
  private String url;

  public SearchServer(Props props) {
    this.settings = new EsSettings(props);
    new MinimumViableSystem()
      .checkWritableTempDir();
  }

  @Override
  public void start() {
    Path absolutePath = findExecutable();

    List<String> command = new ArrayList<>();
    command.add(absolutePath.toString());
    Map<String, String> settingsMap = settings.build();
    settingsMap.entrySet().stream()
      .filter(entry -> !"path.home".equals(entry.getKey()))
      .forEach(entry -> command.add("-E" + entry.getKey() + "=" + entry.getValue()));
    url = "http://"+settingsMap.get("http.host") + ":" + settingsMap.get("http.port");
    System.out.println(command.stream().collect(Collectors.joining(" ")));
    ProcessBuilder builder = new ProcessBuilder(command)
      .directory(new File(absolutePath.getParent().toString()));
    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
    builder.redirectErrorStream(true);
    try {
      p = builder.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    CountDownLatch latch = new CountDownLatch(2);

    new Thread(() -> {
      InputStream inputStream = p.getInputStream();
      InputStreamReader reader1 = new InputStreamReader(inputStream);
      BufferedReader reader = new BufferedReader(reader1);
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
          if (line.contains(" publish_address ")) {
            latch.countDown();
          }
          if (line.contains(" started")) {
            latch.countDown();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();

    try {
      latch.await();
    } catch (InterruptedException e) {
      // no action required
    }

    String urlString = url+"/_cluster/health?wait_for_status=yellow&timeout=30s";
    try {
      URL url = new URL(urlString);
      url.openConnection();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Path findExecutable() {
    String executable = getExecutable();
    Path path = Paths.get(executable);
    if (path.toFile().exists()) {
      return path.toAbsolutePath();
    }

    String assembly = "sonar-application/src/main/assembly/";
    Path idePath = Paths.get(assembly + executable);
    if (idePath.toFile().exists()) {
      return idePath.toAbsolutePath();
    }

    String parentAssembly = "../" + assembly;
    Path buildServerPath = Paths.get(parentAssembly + executable);
    if (buildServerPath.toFile().exists()) {
      return buildServerPath.toAbsolutePath();
    }

    String grandParentAssembly = "../" + parentAssembly;
    Path buildServerPath2 = Paths.get(grandParentAssembly + executable);
    if (buildServerPath2.toFile().exists()) {
      return buildServerPath2.toAbsolutePath();
    }

    throw new IllegalStateException(String.format("Cannot find elasticsearch binary %s in either %s or %s or %s or %s",
            executable,
            Paths.get(".").toAbsolutePath().getParent(),
            Paths.get(assembly).toAbsolutePath(),
            Paths.get(parentAssembly).toAbsolutePath(),
            Paths.get(grandParentAssembly).toAbsolutePath()
    ));
  }

  private static String getExecutable() {
    if (System.getProperty("os.name").startsWith("Windows")) {
      return "elasticsearch/bin/elasticsearch.bat";
    }
    return "elasticsearch/bin/elasticsearch";
  }

  @Override
  public Status getStatus() {
    Status status = null;
    try {
      status = getStatus2();
      System.out.println("ES STATUS "+status);
      return status;
    } catch (Exception e) {
      System.out.println("ES STATUS "+e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private Status getStatus2() {
    String urlString = url+"/_cluster/health";
    try {
      URL url = new URL(urlString);
      URLConnection urlConnection = url.openConnection();
      InputStream inputStream = urlConnection.getInputStream();
      String line;
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      while ((line = reader.readLine()) != null) {
        if (line.contains("\"status\"")) {
          if (line.contains("\"red\"")) {
            return Status.DOWN;
          }
          if (line.contains("\"yellow\"")) {
            return Status.OPERATIONAL;
          }
          if (line.contains("\"green\"")) {
            return Status.OPERATIONAL;
          }
        }
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException(String.format("Cannot contact local Elasticsearch instance via url '%s'", urlString), e);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Status.DOWN;
  }

  @Override
  public void awaitStop() {
    try {
      while (p != null && p.isAlive()) {
        Thread.sleep(200L);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (p != null) {
      p.destroy();
      try {
        p.waitFor();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void stop() {
    if (p != null) {
      p.destroyForcibly();
    }
    //Jmx.unregister(EsSettingsMBean.OBJECT_NAME);
  }

  public static void main(String... args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    new SearchLogging().configure(entryPoint.getProps());
    SearchServer searchServer = new SearchServer(entryPoint.getProps());
    entryPoint.launch(searchServer);
  }
}
