/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.bootstrapper.LoggingConfiguration;

import static org.sonar.batch.bootstrapper.LoggingConfiguration.LEVEL_ROOT_DEFAULT;
import static org.sonar.batch.bootstrapper.LoggingConfiguration.LEVEL_ROOT_VERBOSE;

public class ScannerMain {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ScannerMain.class);

  private static final String SCANNER_APP_KEY = "sonar.scanner.app";
  private static final String SCANNER_APP_VERSION_KEY = "sonar.scanner.appVersion";

  public static void main(String... args) {
    System.exit(run(System.in));
  }

  public static int run(InputStream in) {
    try {
      LOG.info("Starting SonarScanner Engine...");
      LOG.atInfo().log(ScannerMain::java);

      var properties = parseInputProperties(in);

      EnvironmentConfig.processEnvVariables(properties);

      configureLogLevel(properties);

      runScannerEngine(properties);

      LOG.info("SonarScanner Engine completed successfully");
      return 0;
    } catch (Exception e) {
      handleException(e);
      return 1;
    }
  }

  static String java() {
    StringBuilder sb = new StringBuilder();
    sb
      .append("Java ")
      .append(System.getProperty("java.version"))
      .append(" ")
      .append(System.getProperty("java.vendor"));
    String bits = System.getProperty("sun.arch.data.model");
    if ("32".equals(bits) || "64".equals(bits)) {
      sb.append(" (").append(bits).append("-bit)");
    }
    return sb.toString();
  }

  private static void handleException(Exception e) {
    var messageException = unwrapMessageException(e);
    if (messageException.isPresent()) {
      // Don't show the stacktrace for a message exception to not pollute the logs
      if (LoggerFactory.getLogger(ScannerMain.class).isDebugEnabled()) {
        LOG.error(messageException.get(), e);
      } else {
        LOG.error(messageException.get());
      }
    } else {
      LOG.error("Error during SonarScanner Engine execution", e);
    }
  }

  private static Optional<String> unwrapMessageException(Exception t) {
    Throwable y = t;
    do {
      if (y instanceof MessageException messageException) {
        return Optional.of(messageException.getMessage());
      }
      y = y.getCause();
    } while (y != null);

    return Optional.empty();
  }

  private static @NotNull Map<String, String> parseInputProperties(InputStream in) {
    Map<String, String> properties = new HashMap<>();
    var input = parseJsonInput(in);
    if (input != null && input.scannerProperties != null) {
      input.scannerProperties.forEach(prop -> {
        if (prop == null) {
          LOG.warn("Ignoring null property");
        } else if (prop.key == null) {
          LOG.warn("Ignoring property with null key: '{}'", prop.value);
        } else {
          if (properties.containsKey(prop.key)) {
            LOG.warn("Duplicated properties with key: '{}'", prop.key);
          }
          properties.put(prop.key, prop.value);
        }
      });
    }
    return properties;
  }

  @CheckForNull
  private static Input parseJsonInput(InputStream in) {
    try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return new Gson().fromJson(reader, Input.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse JSON input", e);
    }
  }

  private static void runScannerEngine(Map<String, String> properties) {
    var scannerAppKey = properties.get(SCANNER_APP_KEY);
    var scannerAppVersion = properties.get(SCANNER_APP_VERSION_KEY);
    var env = new EnvironmentInformation(scannerAppKey, scannerAppVersion);
    SpringGlobalContainer.create(properties, List.of(env)).execute();
  }

  private static void configureLogLevel(Map<String, String> properties) {
    var verbose = LoggingConfiguration.isVerboseEnabled(properties);
    var rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.toLevel(verbose ? LEVEL_ROOT_VERBOSE : LEVEL_ROOT_DEFAULT));
  }

  private static class Input {
    @SerializedName("scannerProperties")
    private List<ScannerProperty> scannerProperties;
  }

  private static class ScannerProperty {
    @SerializedName("key")
    private String key;

    @SerializedName("value")
    private String value;
  }

}
