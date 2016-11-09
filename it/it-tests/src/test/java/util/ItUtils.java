/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;

import static com.google.common.base.Preconditions.checkState;
import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ItUtils {
  public static final Splitter LINE_SPLITTER = Splitter.on(System.getProperty("line.separator"));

  private ItUtils() {
  }

  public static FileLocation xooPlugin() {
    return FileLocation.byWildcardMavenFilename(new File("../../plugins/sonar-xoo-plugin/target"), "sonar-xoo-plugin-*.jar");
  }

  public static List<Issue> getAllServerIssues(Orchestrator orchestrator) {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    return issueClient.find(IssueQuery.create()).list();
  }

  public static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return newUserWsClient(orchestrator, ADMIN_LOGIN, ADMIN_PASSWORD);
  }

  public static WsClient newWsClient(Orchestrator orchestrator) {
    return newUserWsClient(orchestrator, null, null);
  }

  public static WsClient newUserWsClient(Orchestrator orchestrator, @Nullable String login, @Nullable String password) {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .credentials(login, password)
      .build());
  }

  /**
   * Locate the directory of sample project
   *
   * @param relativePath path related to the directory it/it-projects, for example "qualitygate/xoo-sample"
   */
  public static File projectDir(String relativePath) {
    File dir = new File("../it-projects/" + relativePath);
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IllegalStateException("Directory does not exist: " + dir.getAbsolutePath());
    }
    return dir;
  }

  /**
   * Locate the artifact of a fake plugin stored in it/it-plugins.
   *
   * @param dirName the directory of it/it-plugins, for example "sonar-fake-plugin".
   *                     It assumes that version is 1.0-SNAPSHOT
   */
  public static FileLocation pluginArtifact(String dirName) {
    return FileLocation.byWildcardMavenFilename(new File("../it-plugins/" + dirName + "/target"), dirName + "-*.jar");
  }

  /**
   * Locate the pom file of a sample project
   *
   * @param projectName project path related to the directory it/it-projects, for example "qualitygate/xoo-sample"
   */
  public static File projectPom(String projectName) {
    File pom = new File(projectDir(projectName), "pom.xml");
    if (!pom.exists() || !pom.isFile()) {
      throw new IllegalStateException("pom file does not exist: " + pom.getAbsolutePath());
    }
    return pom;
  }

  public static String sanitizeTimezones(String s) {
    return s.replaceAll("[\\+\\-]\\d\\d\\d\\d", "+0000");
  }

  public static JSONObject getJSONReport(BuildResult result) {
    Pattern pattern = Pattern.compile("Export issues to (.*?).json");
    Matcher m = pattern.matcher(result.getLogs());
    if (m.find()) {
      String s = m.group(1);
      File path = new File(s + ".json");
      assertThat(path).exists();
      try {
        return (JSONObject) JSONValue.parse(FileUtils.readFileToString(path));
      } catch (IOException e) {
        throw new RuntimeException("Unable to read JSON report", e);
      }
    }
    fail("Unable to locate json report");
    return null;
  }

  public static int countIssuesInJsonReport(BuildResult result, boolean onlyNews) {
    JSONObject obj = getJSONReport(result);
    JSONArray issues = (JSONArray) obj.get("issues");
    int count = 0;
    for (Object issue : issues) {
      JSONObject jsonIssue = (JSONObject) issue;
      if (!onlyNews || (Boolean) jsonIssue.get("isNew")) {
        count++;
      }
    }
    return count;
  }

  public static void assertIssuesInJsonReport(BuildResult result, int newIssues, int resolvedIssues, int existingIssues) {
    JSONObject obj = getJSONReport(result);
    JSONArray issues = (JSONArray) obj.get("issues");
    int countNew = 0;
    int countResolved = 0;
    int countExisting = 0;

    for (Object issue : issues) {
      JSONObject jsonIssue = (JSONObject) issue;

      if ((Boolean) jsonIssue.get("isNew")) {
        countNew++;
      } else if (jsonIssue.get("resolution") != null) {
        countResolved++;
      } else {
        countExisting++;
      }
    }
    assertThat(countNew).isEqualTo(newIssues);
    assertThat(countResolved).isEqualTo(resolvedIssues);
    assertThat(countExisting).isEqualTo(existingIssues);
  }

  public static SonarRunner runVerboseProjectAnalysis(Orchestrator orchestrator, String projectRelativePath, String... properties) {
    return runProjectAnalysis(orchestrator, projectRelativePath, true, properties);
  }

  public static SonarRunner runProjectAnalysis(Orchestrator orchestrator, String projectRelativePath, String... properties) {
    return runProjectAnalysis(orchestrator, projectRelativePath, false, properties);
  }

  private static SonarRunner runProjectAnalysis(Orchestrator orchestrator, String projectRelativePath, boolean enableDebugLogs, String... properties) {
    SonarRunner sonarRunner = SonarRunner.create(projectDir(projectRelativePath));
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (int i = 0; i < properties.length; i += 2) {
      builder.put(properties[i], properties[i + 1]);
    }
    SonarRunner scan = sonarRunner.setDebugLogs(enableDebugLogs).setProperties(builder.build());
    orchestrator.executeBuild(scan);
    return scan;
  }

  public static void setServerProperty(Orchestrator orchestrator, String key, @Nullable String value) {
    setServerProperty(orchestrator, null, key, value);
  }

  public static void setServerProperty(Orchestrator orchestrator, @Nullable String componentKey, String key, @Nullable String value) {
    if (value == null) {
      newAdminWsClient(orchestrator).settingsService().reset(ResetRequest.builder().setKeys(key).setComponentKey(componentKey).build());
    } else {
      newAdminWsClient(orchestrator).settingsService().set(SetRequest.builder().setKey(key).setValue(value).setComponentKey(componentKey).build());
    }
  }

  public static void setServerProperties(Orchestrator orchestrator, @Nullable String componentKey, String... properties) {
    for (int i = 0; i < properties.length; i += 2) {
      setServerProperty(orchestrator, componentKey, properties[i], properties[i + 1]);
    }
  }

  public static void resetSettings(Orchestrator orchestrator, @Nullable String componentKey, String... keys) {
    newAdminWsClient(orchestrator).settingsService().reset(ResetRequest.builder().setKeys(keys).setComponentKey(componentKey).build());
  }

  public static void resetEmailSettings(Orchestrator orchestrator) {
    resetSettings(orchestrator, null, "email.smtp_host.secured", "email.smtp_port.secured", "email.smtp_secure_connection.secured", "email.smtp_username.secured",
      "email.smtp_password.secured", "email.from", "email.prefix");
  }

  public static void resetPeriods(Orchestrator orchestrator) {
    resetSettings(orchestrator, null, "sonar.timemachine.period1", "sonar.timemachine.period2", "sonar.timemachine.period3");
  }

  /**
   * Concatenates a vararg to a String array.
   *
   * Useful when using {@link #runVerboseProjectAnalysis(Orchestrator, String, String...)}, eg.:
   * <pre>
   * ItUtils.runProjectAnalysis(orchestrator, "project_name",
   *    ItUtils.concat(properties, "sonar.scm.disabled", "false")
   *    );
   * </pre>
   */
  public static String[] concat(String[] properties, String... str) {
    if (properties == null || properties.length == 0) {
      return str;
    }
    return Stream.concat(Arrays.stream(properties), Arrays.stream(str))
      .toArray(String[]::new);
  }

  public static void verifyHttpException(Exception e, int expectedCode) {
    assertThat(e).isInstanceOf(HttpException.class);
    HttpException exception = (HttpException) e;
    assertThat(exception.status()).isEqualTo(expectedCode);
  }

  public static Date toDate(String sDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static String formatDate(Date d) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.format(d);
  }

  public static String extractCeTaskId(BuildResult buildResult) {
    List<String> taskIds = extractCeTaskIds(buildResult);
    checkState(taskIds.size() == 1, "More than one task id retrieved from logs");
    return taskIds.iterator().next();
  }

  public static List<String> extractCeTaskIds(BuildResult buildResult) {
    String logs = buildResult.getLogs();
    return StreamSupport.stream(LINE_SPLITTER.split(logs).spliterator(), false)
      .filter(s -> s.contains("More about the report processing at"))
      .map(s -> s.substring(s.length() - 20, s.length()))
      .collect(Collectors.toList());
  }

  public static Map<String, Object> jsonToMap(String json) {
    Gson gson = new Gson();
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    return gson.fromJson(json, type);
  }
}
