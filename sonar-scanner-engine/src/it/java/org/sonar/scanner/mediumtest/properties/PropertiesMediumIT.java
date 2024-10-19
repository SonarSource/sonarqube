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
package org.sonar.scanner.mediumtest.properties;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarEdition;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesMediumIT {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .setEdition(SonarEdition.ENTERPRISE)
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    // active a rule just to be sure that xoo files are published
    .addActiveRule("xoo", "xoo:OneIssuePerFile", null, "One Issue Per File", null, null, null);

  @Test
  public void testProperties() throws IOException {
    File baseDir = prepareProject();

    tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.analysis.property", "value")
        .build())
      .execute();

    var properties = getProperties(baseDir);

    //We focus on the specific property that we would like to get added to the report
    assertThat(properties).containsEntry("sonar.analysis.property", "value");
  }

  private Map<String, String> getProperties(File baseDir) {
    File reportDir = new File(baseDir, ".sonar/scanner-report");
    FileStructure fileStructure = new FileStructure(reportDir);
    ScannerReportReader reader = new ScannerReportReader(fileStructure);

    Map<String, String> properties = new HashMap<>();

    try (var iterator = reader.readContextProperties()) {
      iterator.forEachRemaining(p -> properties.put(p.getKey(), p.getValue()));
    }

    return properties;
  }

  private File prepareProject() throws IOException {
    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile1 = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile1, "Sample xoo\ncontent\n3\n4\n5", StandardCharsets.UTF_8);

    return baseDir;
  }

}
