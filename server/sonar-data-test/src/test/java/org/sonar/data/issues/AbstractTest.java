/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.data.issues;

import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.rule.RuleTesting;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class AbstractTest {

  final static int RULES_NUMBER = 25;
  final static int USERS_NUMBER = 100;

  private static final double ACCEPTED_DURATION_VARIATION_IN_PERCENTS = 10.0;

  static Iterator<RuleDto> rules;
  static Iterator<String> users;
  static Iterator<String> severities;
  static Iterator<String> statuses;
  static Iterator<String> closedResolutions;
  static Iterator<String> resolvedResolutions;

  static Properties properties = new Properties();
  @Rule
  public TestName testName = new TestName();
  protected AtomicLong counter;
  protected ProgressTask progressTask;

  @BeforeClass
  public static void loadProperties() {
    try {
      properties = new Properties();
      File propertiesFile = Resources.getResourceAsFile("assertions.properties");
      FileReader reader = new FileReader(propertiesFile);
      properties.load(reader);
      properties.putAll(System.getProperties());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    severities = Iterables.cycle(Severity.ALL).iterator();
    statuses = Iterables.cycle(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED).iterator();
    closedResolutions = Iterables.cycle(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED, Issue.RESOLUTION_REMOVED).iterator();
    resolvedResolutions = Iterables.cycle(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED).iterator();
    users = Iterables.cycle(generateUsers()).iterator();
    rules = Iterables.cycle(generateRules()).iterator();
  }

  protected static List<RuleDto> generateRules() {
    List<RuleDto> rules = newArrayList();
    for (int i = 0; i < RULES_NUMBER; i++) {
      rules.add(RuleTesting.newDto(RuleKey.of("rule-repo", "rule-key-" + i)));
    }
    return rules;
  }

  protected static List<String> generateUsers() {
    List<String> users = newArrayList();
    for (int i = 0; i < USERS_NUMBER; i++) {
      users.add("user-" + i);
    }
    return users;
  }

  @Before
  public void initCounter() throws Exception {
    counter = new AtomicLong(0L);
    progressTask = new ProgressTask(counter);
  }

  protected String getProperty(String test) {
    String currentUser = StringUtils.defaultString(properties.getProperty("user"), "default");
    String property = currentUser + "." + test;
    String value = properties.getProperty(property);
    if (value == null) {
      throw new IllegalArgumentException(String.format("Property '%s' hasn't been found", property));
    }
    return value;
  }

  protected void assertDurationAround(long duration, long expectedDuration) {
    double variation = 100.0 * (0.0 + duration - expectedDuration) / expectedDuration;
    System.out.printf("Test %s : executed in %d ms (%.2f %% from target)\n", testName.getMethodName(), duration, variation);
    assertThat(Math.abs(variation)).as(String.format("Expected %d ms, got %d ms", expectedDuration, duration)).isLessThan(ACCEPTED_DURATION_VARIATION_IN_PERCENTS);
  }

  protected IssueDto newIssue(int index, ComponentDto file, ComponentDto project, RuleDto rule) {
    String status = statuses.next();
    String resolution = null;
    if (status.equals(Issue.STATUS_CLOSED)) {
      resolution = closedResolutions.next();
    } else if (status.equals(Issue.STATUS_RESOLVED)) {
      resolution = resolvedResolutions.next();
    }
    return IssueTesting.newDto(rule, file, project)
      .setMessage("Message from rule " + rule.getKey().toString() + " on line " + index)
      .setLine(index)
      .setAssignee(users.next())
      .setReporter(users.next())
      .setAuthorLogin(users.next())
      .setSeverity(severities.next())
      .setStatus(status)
      .setResolution(resolution);
  }

  protected static class ProgressTask extends TimerTask {
    public static final long PERIOD_MS = 60000L;
    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceTests");
    private final AtomicLong counter;

    public ProgressTask(AtomicLong counter) {
      this.counter = counter;
    }

    @Override
    public void run() {
      log();
    }

    public void log() {
      LOGGER.info(String.format("%d issues processed", counter.get()));
    }
  }

}
