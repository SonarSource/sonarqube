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

package org.sonar.batch.report;

import com.google.common.collect.Lists;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.issue.ScanIssues;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.issue.DefaultIssue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarReportTest {

  @org.junit.Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  SonarReport sonarReport;
  SensorContext sensorContext = mock(SensorContext.class);
  Resource resource = mock(Resource.class);
  ModuleFileSystem fileSystem = mock(ModuleFileSystem.class);
  Server server = mock(Server.class);
  RuleI18nManager ruleI18nManager = mock(RuleI18nManager.class);
  Settings settings;
  ScanIssues scanIssues = mock(ScanIssues.class);

  @Before
  public void before() {
    when(resource.getEffectiveKey()).thenReturn("Action.java");
    when(server.getVersion()).thenReturn("3.6");

    settings = new Settings();
    settings.setProperty(CoreProperties.DRY_RUN, true);
    sonarReport = new SonarReport(settings, fileSystem, server, ruleI18nManager, scanIssues);
  }

  @Test
  public void should_export_json() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("Action.java")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setNew(false);

    when(sonarReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));

    JSONObject json = sonarReport.createJson();
    assertThat(json.values()).hasSize(4);

    assertThat(json.get("version")).isEqualTo("3.6");

    assertThat(json.get("components")).isNotNull();
    JSONArray components = (JSONArray) json.get("components");
    assertThat(components).hasSize(1);

    assertThat(json.get("issues")).isNotNull();
    JSONArray issues = (JSONArray) json.get("issues");
    assertThat(issues).hasSize(1);

    assertThat(json.get("rules")).isNotNull();
    JSONArray rules = (JSONArray) json.get("rules");
    assertThat(rules).hasSize(1);
  }

  @Test
  public void should_export_components() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("Action.java")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setNew(false);

    when(sonarReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));

    JSONObject json = sonarReport.createJson();
    assertThat(json.get("version")).isEqualTo("3.6");

    assertThat(json.get("components")).isNotNull();
    JSONArray components = (JSONArray) json.get("components");
    assertThat(components).hasSize(1);
    JSONObject jsonComponent = (JSONObject) components.get(0);
    assertThat(jsonComponent.values()).hasSize(1);
    assertThat(jsonComponent.get("key")).isEqualTo("Action.java");
  }

  @Test
  public void should_export_issues() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("Action.java")
      .setDescription("SystemPrintln")
      .setSeverity("MINOR")
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setLine(1)
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setCreatedAt(DateUtils.parseDate("2013-04-24"))
      .setUpdatedAt(DateUtils.parseDate("2013-04-25"))
      .setClosedAt(DateUtils.parseDate("2013-04-26"))
      .setNew(false);

    when(sonarReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));

    JSONObject json = sonarReport.createJson();
    assertThat(json.get("issues")).isNotNull();
    JSONArray issues = (JSONArray) json.get("issues");
    assertThat(issues).hasSize(1);
    JSONObject jsonIssue = (JSONObject) issues.get(0);
    assertThat(jsonIssue.values()).hasSize(12);

    assertThat(jsonIssue.get("key")).isEqualTo("200");
    assertThat(jsonIssue.get("component")).isEqualTo("Action.java");
    assertThat(jsonIssue.get("line")).isEqualTo(1);
    assertThat(jsonIssue.get("description")).isEqualTo("SystemPrintln");
    assertThat(jsonIssue.get("severity")).isEqualTo("MINOR");
    assertThat(jsonIssue.get("rule")).isEqualTo("squid:AvoidCycle");
    assertThat(jsonIssue.get("status")).isEqualTo("CLOSED");
    assertThat(jsonIssue.get("resolution")).isEqualTo("FALSE-POSITIVE");
    assertThat(jsonIssue.get("isNew")).isEqualTo(false);
    assertThat((String) jsonIssue.get("createdAt")).contains("2013-04-24T00:00");
    assertThat((String) jsonIssue.get("updatedAt")).contains("2013-04-25T00:00");
    assertThat((String) jsonIssue.get("closedAt")).contains("2013-04-26T00:00");
  }

  @Test
  public void should_export_rules() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("Action.java")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    when(ruleI18nManager.getName("squid", "AvoidCycle", Locale.getDefault())).thenReturn("Avoid Cycle");
    when(sonarReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));

    JSONObject root = sonarReport.createJson();

    assertThat(root.get("rules")).isNotNull();
    JSONArray rules = (JSONArray) root.get("rules");
    assertThat(rules).hasSize(1);
    JSONObject json = (JSONObject) rules.get(0);
    assertThat(json.values()).hasSize(4);

    assertThat(json.get("key")).isEqualTo("squid:AvoidCycle");
    assertThat(json.get("rule")).isEqualTo("AvoidCycle");
    assertThat(json.get("repository")).isEqualTo("squid");
    assertThat(json.get("name")).isEqualTo("Avoid Cycle");
  }

  @Test
  public void should_export_issues_with_no_line() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("200")
      .setComponentKey("Action.java")
      .setLine(null)
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    when(sonarReport.getIssues()).thenReturn(Lists.<DefaultIssue>newArrayList(issue));

    JSONObject json = sonarReport.createJson();
    assertThat(json.get("issues")).isNotNull();

    JSONArray issues = (JSONArray) json.get("issues");
    JSONObject jsonIssue = (JSONObject) issues.get(0);
    assertThat(jsonIssue.get("key")).isEqualTo("200");
    assertThat(jsonIssue.containsKey("line")).isFalse();
  }

  @Test
  public void should_ignore_resources_without_issue() {
    when(sonarReport.getIssues()).thenReturn(Collections.<DefaultIssue>emptyList());

    JSONObject json = sonarReport.createJson();
    assertThat(json.get("version")).isEqualTo("3.6");

    assertThat(json.get("components")).isNotNull();
    JSONArray components = (JSONArray) json.get("components");
    assertThat(components).isEmpty();

    assertThat(json.get("issues")).isNotNull();
    JSONArray issues = (JSONArray) json.get("issues");
    assertThat(issues).isEmpty();
  }

  @Test
  public void should_export_issues_to_file() throws IOException {
    File sonarDirectory = temporaryFolder.newFolder("sonar");

    Rule rule = Rule.create("squid", "AvoidCycle");
    when(ruleI18nManager.getName(rule, Locale.getDefault())).thenReturn("Avoid Cycle");
    when(sonarReport.getIssues()).thenReturn(Collections.<DefaultIssue>emptyList());

    settings.setProperty("sonar.report.export.path", "output.json");
    when(fileSystem.workingDir()).thenReturn(sonarDirectory);

    sonarReport.execute(sensorContext);

    assertThat(new File(sonarDirectory, "output.json")).exists();
  }

}
