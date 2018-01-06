/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.qa.util.SettingTester;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.measures.ComponentRequest;
import org.sonarqube.ws.client.settings.ResetRequest;
import org.sonarqube.ws.client.settings.SetRequest;

import static com.google.common.base.Preconditions.checkState;
import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.RestoreActionParameters.PARAM_BACKUP;

public class ItUtils {
  public static final Splitter LINE_SPLITTER = Splitter.on(System.getProperty("line.separator"));

  private ItUtils() {
  }

  public static FileLocation xooPlugin() {
    return FileLocation.byWildcardMavenFilename(new File("../plugins/sonar-xoo-plugin/target"), "sonar-xoo-plugin-*.jar");
  }

  public static List<Issue> getAllServerIssues(Orchestrator orchestrator) {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    return issueClient.find(IssueQuery.create()).list();
  }

  /**
   * @deprecated replaced by {@link Tester#wsClient()}
   */
  @Deprecated
  public static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return newUserWsClient(orchestrator, ADMIN_LOGIN, ADMIN_PASSWORD);
  }

  /**
   * @deprecated replaced by {@link Tester#wsClient()}
   */
  @Deprecated
  public static WsClient newWsClient(Orchestrator orchestrator) {
    return newUserWsClient(orchestrator, null, null);
  }

  /**
   * @deprecated replaced by {@link Tester#wsClient()}
   */
  @Deprecated
  public static WsClient newUserWsClient(Orchestrator orchestrator, @Nullable String login, @Nullable String password) {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .credentials(login, password)
      .build());
  }

  /**
   * @deprecated replaced by {@link Tester#wsClient()}
   */
  @Deprecated
  public static WsClient newSystemUserWsClient(Orchestrator orchestrator, @Nullable String systemPassCode) {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .systemPassCode(systemPassCode)
      .build());
  }

  /**
   * Locate the directory of sample project
   *
   * @param relativePath path related to the directory it/projects, for example "qualitygate/xoo-sample"
   */
  public static File projectDir(String relativePath) {
    File dir = new File("projects/" + relativePath);
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IllegalStateException("Directory does not exist: " + dir.getAbsolutePath());
    }
    return dir;
  }

  /**
   * Locate the artifact of a fake plugin stored in it/plugins.
   *
   * @param dirName the directory of it/plugins, for example "sonar-fake-plugin".
   */
  public static FileLocation pluginArtifact(String dirName) {
    return FileLocation.byWildcardMavenFilename(new File("plugins/" + dirName + "/target"), dirName + "-*.jar");
  }

  /**
   * Locate the pom file of a sample project
   *
   * @param projectName project path related to the directory it/projects, for example "qualitygate/xoo-sample"
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

  /**
   * @deprecated replaced by {@link SettingTester#setGlobalSetting(String, String)}
   */
  @Deprecated
  public static void setServerProperty(Orchestrator orchestrator, String key, @Nullable String value) {
    setServerProperty(orchestrator, null, key, value);
  }

  /**
   * @deprecated replaced by {@link SettingTester#setProjectSetting(String, String, String)}
   */
  @Deprecated
  public static void setServerProperty(Orchestrator orchestrator, @Nullable String componentKey, String key, @Nullable String value) {
    if (value == null) {
      newAdminWsClient(orchestrator).settings().reset(new ResetRequest().setKeys(asList(key)).setComponent(componentKey));
    } else {
      newAdminWsClient(orchestrator).settings().set(new SetRequest().setKey(key).setValue(value).setComponent(componentKey));
    }
  }

  /**
   * @deprecated replaced by {@link SettingTester#setGlobalSetting(String, String)} or {@link SettingTester#setProjectSettings(String, String...)}
   */
  @Deprecated
  public static void setServerProperties(Orchestrator orchestrator, @Nullable String componentKey, String... properties) {
    for (int i = 0; i < properties.length; i += 2) {
      setServerProperty(orchestrator, componentKey, properties[i], properties[i + 1]);
    }
  }

  /**
   * @deprecated replaced by {@link SettingTester#resetSettings(String...)}
   */
  @Deprecated
  public static void resetSettings(Orchestrator orchestrator, @Nullable String componentKey, String... keys) {
    if (keys.length > 0) {
      newAdminWsClient(orchestrator).settings().reset(new ResetRequest().setKeys(Arrays.asList(keys)).setComponent(componentKey));
    }
  }

  /**
   * @deprecated no more needed as already done by n by {@link Tester#after()}
   */
  @Deprecated
  public static void resetEmailSettings(Orchestrator orchestrator) {
    resetSettings(orchestrator, null, "email.smtp_host.secured", "email.smtp_port.secured", "email.smtp_secure_connection.secured", "email.smtp_username.secured",
      "email.smtp_password.secured", "email.from", "email.prefix");
  }

  /**
   * @deprecated no more needed as already done by n by {@link Tester#after()}
   */
  @Deprecated
  public static void resetPeriod(Orchestrator orchestrator) {
    resetSettings(orchestrator, null, "sonar.leak.period");
  }

  @CheckForNull
  public static Measure getMeasure(Orchestrator orchestrator, String componentKey, String metricKey) {
    return getMeasuresByMetricKey(orchestrator, componentKey, metricKey).get(metricKey);
  }

  @CheckForNull
  public static Double getMeasureAsDouble(Orchestrator orchestrator, String componentKey, String metricKey) {
    Measure measure = getMeasure(orchestrator, componentKey, metricKey);
    return (measure == null) ? null : Double.parseDouble(measure.getValue());
  }

  public static Map<String, Measure> getMeasuresByMetricKey(Orchestrator orchestrator, String componentKey, String... metricKeys) {
    return getStreamMeasures(orchestrator, componentKey, metricKeys)
      .filter(Measure::hasValue)
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  public static Map<String, Double> getMeasuresAsDoubleByMetricKey(Orchestrator orchestrator, String componentKey, String... metricKeys) {
    return getStreamMeasures(orchestrator, componentKey, metricKeys)
      .filter(Measure::hasValue)
      .collect(Collectors.toMap(Measure::getMetric, measure -> parseDouble(measure.getValue())));
  }

  private static Stream<Measure> getStreamMeasures(Orchestrator orchestrator, String componentKey, String... metricKeys) {
    return newWsClient(orchestrator).measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(asList(metricKeys)))
      .getComponent().getMeasuresList()
      .stream();
  }

  @CheckForNull
  public static Measure getMeasureWithVariation(Orchestrator orchestrator, String componentKey, String metricKey) {
    Measures.ComponentWsResponse response = newWsClient(orchestrator).measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(singletonList(metricKey))
      .setAdditionalFields(singletonList("periods")));
    List<Measure> measures = response.getComponent().getMeasuresList();
    return measures.size() == 1 ? measures.get(0) : null;
  }

  @CheckForNull
  public static Map<String, Measure> getMeasuresWithVariationsByMetricKey(Orchestrator orchestrator, String componentKey, String... metricKeys) {
    return newWsClient(orchestrator).measures().component(new ComponentRequest()
      .setComponent(componentKey)
      .setMetricKeys(asList(metricKeys))
      .setAdditionalFields(singletonList("periods"))).getComponent().getMeasuresList()
      .stream()
      .collect(Collectors.toMap(Measure::getMetric, Function.identity()));
  }

  /**
   * Return leak period value
   */
  @CheckForNull
  public static Double getLeakPeriodValue(Orchestrator orchestrator, String componentKey, String metricKey) {
    List<Measures.PeriodValue> periodsValueList = getMeasureWithVariation(orchestrator, componentKey, metricKey).getPeriods().getPeriodsValueList();
    return periodsValueList.size() > 0 ? Double.parseDouble(periodsValueList.get(0).getValue()) : null;
  }

  @CheckForNull
  public static Component getComponent(Orchestrator orchestrator, String componentKey) {
    try {
      return newWsClient(orchestrator).components().show(new ShowRequest().setComponent((componentKey))).getComponent();
    } catch (org.sonarqube.ws.client.HttpException e) {
      if (e.code() == 404) {
        return null;
      }
      throw new IllegalStateException(e);
    }
  }

  @CheckForNull
  public static ComponentNavigation getComponentNavigation(Orchestrator orchestrator, String componentKey) {
    // Waiting for SONAR-7745 to have version in api/components/show, we use internal api/navigation/component WS to get the component
    // version
    String content = newWsClient(orchestrator).wsConnector().call(new GetRequest("api/navigation/component").setParam("componentKey", componentKey)).failIfNotSuccessful()
      .content();
    return ComponentNavigation.parse(content);
  }

  public static void restoreProfile(Orchestrator orchestrator, URL resource) {
    restoreProfile(orchestrator, resource, null);
  }

  public static void restoreProfile(Orchestrator orchestrator, URL resource, String organization) {
    URI uri;
    try {
      uri = resource.toURI();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Cannot find quality profile xml file '" + resource + "' in classpath");
    }

    PostRequest httpRequest = new PostRequest("api/qualityprofiles/restore")
      .setParam(PARAM_ORGANIZATION, organization)
      .setPart(PARAM_BACKUP, new PostRequest.Part(MediaTypes.XML, new File(uri)));
    HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .credentials(ADMIN_LOGIN, ADMIN_PASSWORD)
      .build().call(httpRequest);
  }

  public static String newOrganizationKey() {
    return randomAlphabetic(32).toLowerCase(ENGLISH);
  }

  public static String newProjectKey() {
    return "key-" + randomAlphabetic(100);
  }

  public static class ComponentNavigation {
    private String version;
    private String analysisDate;

    public String getVersion() {
      return version;
    }

    public Date getDate() {
      return toDatetime(analysisDate);
    }

    public static ComponentNavigation parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ComponentNavigation.class);
    }
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

  public static Date toDate(String sDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static Date toDatetime(String sDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static String formatDate(Date d) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    return sdf.format(d);
  }

  public static String formatDateTime(Date d) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    return sdf.format(d);
  }

  public static String extractCeTaskId(BuildResult buildResult) {
    List<String> taskIds = extractCeTaskIds(buildResult);
    checkState(taskIds.size() == 1, "More than one task id retrieved from logs");
    return taskIds.iterator().next();
  }

  private static List<String> extractCeTaskIds(BuildResult buildResult) {
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

  /**
   * @deprecated replaced by {@code orchestrator.getServer().newHttpCall()}
   */
  @Deprecated
  public static Response call(String url, String... headers) {
    Request.Builder requestBuilder = new Request.Builder().get().url(url);
    for (int i = 0; i < headers.length; i += 2) {
      String headerName = headers[i];
      String headerValue = headers[i + 1];
      if (headerValue != null) {
        requestBuilder.addHeader(headerName, headerValue);
      }
    }
    try {
      return new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        .newCall(requestBuilder.build())
        .execute();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static void expectBadRequestError(Runnable runnable) {
    expectHttpError(400, runnable);
  }

  public static void expectMissingError(Runnable runnable) {
    expectHttpError(404, runnable);
  }

  /**
   * Missing permissions
   */
  public static void expectForbiddenError(Runnable runnable) {
    expectHttpError(403, runnable);
  }

  /**
   * Not authenticated
   */
  public static void expectUnauthorizedError(Runnable runnable) {
    expectHttpError(401, runnable);
  }

  public static void expectNotFoundError(Runnable runnable) {
    expectHttpError(404, runnable);
  }

  public static void expectHttpError(int expectedCode, Runnable runnable) {
    try {
      runnable.run();
      Assert.fail("Ws call should have failed");
    } catch (org.sonarqube.ws.client.HttpException e) {
      assertThat(e.code()).isEqualTo(expectedCode);
    }
  }

  public static void expectHttpError(int expectedCode, String expectedMessage, Runnable runnable) {
    try {
      runnable.run();
      Assert.fail("Ws call should have failed");
    } catch (org.sonarqube.ws.client.HttpException e) {
      assertThat(e.code()).isEqualTo(expectedCode);
      assertThat(e.getMessage()).contains(expectedMessage);
    }
  }
}
