package util;/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpWsClient;
import org.sonarqube.ws.client.WsClient;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ItUtils {

  private ItUtils() {
  }

  private static final Supplier<File> HOME_DIR = Suppliers.memoize(new Supplier<File>() {
    @Override
    public File get() {
      File testResources = FileUtils.toFile(ItUtils.class.getResource("/ItUtils.txt"));
      return testResources // ${home}/it/it-tests/src/test/resources
        .getParentFile() // ${home}/it/it-tests/src/test
        .getParentFile() // ${home}/it/it-tests/src
        .getParentFile() // ${home}/it/it-tests
        .getParentFile() // ${home}/it
        .getParentFile(); // ${home}
    }
  });

  public static FileLocation xooPlugin() {
    File target = new File(HOME_DIR.get(), "plugins/sonar-xoo-plugin/target");
    if (target.exists()) {
      for (File jar : FileUtils.listFiles(target, new String[] {"jar"}, false)) {
        if (jar.getName().startsWith("sonar-xoo-plugin-") && !jar.getName().contains("-sources")) {
          return FileLocation.of(jar);
        }
      }
    }
    throw new IllegalStateException("XOO plugin is not built");
  }

  public static List<Issue> getAllServerIssues(Orchestrator orchestrator) {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    return issueClient.find(IssueQuery.create()).list();
  }

  public static WsClient newAdminWsClient(Orchestrator orchestrator) {
    Server server = orchestrator.getServer();
    return new HttpWsClient(new HttpConnector.Builder()
      .url(server.getUrl())
      .credentials(server.ADMIN_LOGIN, server.ADMIN_PASSWORD)
      .build());
  }

  /**
   * Locate the directory of sample project
   *
   * @param relativePath path related to the directory it/it-projects, for example "qualitygate/xoo-sample"
   */
  public static File projectDir(String relativePath) {
    File dir = new File(HOME_DIR.get(), "it/it-projects/" + relativePath);
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
    File file = new File(HOME_DIR.get(), "it/it-plugins/" + dirName + "/target/" + dirName + "-1.0-SNAPSHOT.jar");
    if (!file.exists()) {
      throw new IllegalStateException(String.format("Plugin [%s]for integration tests is not built. File not found:%s", dirName, file));
    }
    return FileLocation.of(file);
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
    if (value == null) {
      orchestrator.getServer().getAdminWsClient().delete(new PropertyDeleteQuery(key));
    } else {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery().setKey(key).setValue(value));
    }
  }

  public static void resetPeriods(Orchestrator orchestrator) {
    setServerProperty(orchestrator, "sonar.timemachine.period1", null);
    setServerProperty(orchestrator, "sonar.timemachine.period2", null);
    setServerProperty(orchestrator, "sonar.timemachine.period3", null);
    setServerProperty(orchestrator, "sonar.timemachine.period4", null);
    setServerProperty(orchestrator, "sonar.timemachine.period5", null);
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
    return from(Iterables.concat(asList(properties), asList(str))).toArray(String.class);
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

}
