/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.application.es;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.sonar.application.command.JavaCommand;
import org.sonar.application.command.JvmOptions;
import org.sonar.process.ProcessId;

import static java.util.Objects.requireNonNull;

public class EsKeyStoreCli {
  public static final String BOOTSTRAP_PASSWORD_PROPERTY_KEY = "bootstrap.password";
  public static final String KEYSTORE_PASSWORD_PROPERTY_KEY = "xpack.security.transport.ssl.keystore.secure_password";
  public static final String TRUSTSTORE_PASSWORD_PROPERTY_KEY = "xpack.security.transport.ssl.truststore.secure_password";
  public static final String HTTP_KEYSTORE_PASSWORD_PROPERTY_KEY = "xpack.security.http.ssl.keystore.secure_password";

  private static final String MAIN_CLASS_NAME = "org.elasticsearch.launcher.CliToolLauncher";

  private final Map<String, String> properties = new LinkedHashMap<>();
  private final JavaCommand<EsKeyStoreJvmOptions> command;

  private EsKeyStoreCli(EsInstallation esInstallation) {
    String esHomeAbsolutePath = esInstallation.getHomeDirectory().getAbsolutePath();
    command = new JavaCommand<EsKeyStoreJvmOptions>(ProcessId.ELASTICSEARCH, esInstallation.getHomeDirectory())
      .setClassName(MAIN_CLASS_NAME)
      .setJvmOptions(new EsKeyStoreJvmOptions(esInstallation))
      .addClasspath(Paths.get(esHomeAbsolutePath, "lib", "").toAbsolutePath() + File.separator + "*")
      .addClasspath(Paths.get(esHomeAbsolutePath, "lib", "cli-launcher", "").toAbsolutePath() + File.separator + "*")
      .addParameter("add")
      .addParameter("-x")
      .addParameter("-f");
  }

  public static EsKeyStoreCli getInstance(EsInstallation esInstallation) {
    return new EsKeyStoreCli(esInstallation);
  }

  public EsKeyStoreCli store(String key, String value) {
    requireNonNull(key, "Property key cannot be null");
    requireNonNull(value, "Property value cannot be null");
    properties.computeIfAbsent(key, s -> {
      command.addParameter(key);
      return value;
    });
    return this;
  }

  public Process executeWith(Function<JavaCommand<?>, Process> commandLauncher) {
    Process process = commandLauncher.apply(command);
    writeValues(process);
    waitFor(process);
    checkExitValue(process.exitValue());
    return process;
  }

  private void writeValues(Process process) {
    try (OutputStream stdin = process.getOutputStream();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin, StandardCharsets.UTF_8))) {
      for (Entry<String, String> entry : properties.entrySet()) {
        writer.write(entry.getValue());
        writer.write("\n");
      }
      writer.flush();

    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void waitFor(Process process) {
    try {
      process.waitFor(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("EsKeyStoreCli has been interrupted", e);
    }
  }

  private static void checkExitValue(int code) {
    if (code != 0) {
      throw new IllegalStateException("Elasticsearch KeyStore tool exited with code: " + code);
    }
  }

  public static class EsKeyStoreJvmOptions extends JvmOptions<EsKeyStoreJvmOptions> {

    public EsKeyStoreJvmOptions(EsInstallation esInstallation) {
      super(mandatoryOptions(esInstallation));
    }

    private static Map<String, String> mandatoryOptions(EsInstallation esInstallation) {
      Map<String, String> res = new LinkedHashMap<>(7);
      res.put("-Xms4m", "");
      res.put("-Xmx64m", "");
      res.put("-XX:+UseSerialGC", "");
      res.put("-Dcli.name=", "");
      res.put("-Dcli.script=", "bin/elasticsearch-keystore");
      res.put("-Dcli.libs=", "lib/tools/keystore-cli");
      res.put("-Des.path.home=", esInstallation.getHomeDirectory().getAbsolutePath());
      res.put("-Des.path.conf=", esInstallation.getConfDirectory().getAbsolutePath());
      return res;
    }
  }
}
