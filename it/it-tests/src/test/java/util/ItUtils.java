package util;/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

import com.sonar.orchestrator.build.BuildResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sonar.orchestrator.locator.FileLocation;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.commons.io.FileUtils;

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
   * @param relativePath project path related to the directory it/it-projects, for example "qualitygate/xoo-sample"
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
}
