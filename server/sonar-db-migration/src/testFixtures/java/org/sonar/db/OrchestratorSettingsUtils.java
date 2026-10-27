/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.sonar.api.config.internal.Settings;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class OrchestratorSettingsUtils {
  /**
   * Name of the database (user on Oracle) the test task owns on the vendor database server, so that the test tasks
   * of different modules can run in parallel without sharing a schema.
   */
  public static final String DEDICATED_DATABASE_PROPERTY = "sonar.test.dedicatedDatabase";

  private OrchestratorSettingsUtils() {
    // prevents instantiation
  }

  public static void loadOrchestratorSettings(Settings settings) {
    String url = settings.getString("orchestrator.configUrl");
    if (isEmpty(url)) {
      return;
    }

    InputStream input = null;
    try {
      URI uri = new URI(url);

      if (url.startsWith("file:")) {
        File file = new File(uri);
        input = FileUtils.openInputStream(file);
      } else {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
          throw new IllegalStateException("Fail to request: " + uri + ". Status code=" + responseCode);
        }

        input = connection.getInputStream();
      }

      Properties props = new Properties();
      props.load(input);
      settings.addProperties(props);
      for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
        String interpolatedValue = StrSubstitutor.replace(entry.getValue(), System.getenv(), "${", "}");
        settings.setProperty(entry.getKey(), interpolatedValue);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load OrchestratorRule properties from:" + url, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
    useDedicatedDatabase(settings);
  }

  /**
   * Orchestrator drops and creates the database named after the login on PostgreSQL and SQL Server, and the schema
   * of the login on Oracle, so a dedicated login is enough to isolate the database.
   */
  static void useDedicatedDatabase(Settings settings) {
    String name = settings.getString(DEDICATED_DATABASE_PROPERTY);
    String url = settings.getString("sonar.jdbc.url");
    if (isEmpty(name) || isEmpty(url)) {
      return;
    }
    settings.setProperty("sonar.jdbc.username", name);
    String replacement = Matcher.quoteReplacement(name);
    switch (StringUtils.defaultString(settings.getString("sonar.jdbc.dialect"))) {
      case "postgresql" -> settings.setProperty("sonar.jdbc.url", url.replaceFirst("^(jdbc:postgresql://[^/]+/)[^?]*", "$1" + replacement));
      case "mssql" -> settings.setProperty("sonar.jdbc.url", url.replaceFirst("databaseName=[^;]*", "databaseName=" + replacement));
      default -> {
        // Oracle connects to the same service with another user
      }
    }
  }
}
