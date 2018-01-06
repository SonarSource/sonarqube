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
package org.sonar.server.issue.notification;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.user.index.UserIndex;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.EFFORT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE_TYPE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesNotificationTest {

  private final Random random = new Random();
  private final RuleType randomRuleType = RuleType.values()[random.nextInt(RuleType.values().length)];
  private NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
  private UserIndex userIndex = mock(UserIndex.class);
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private RuleDao ruleDao = mock(RuleDao.class);
  private Durations durations = mock(Durations.class);
  private NewIssuesNotification underTest = new NewIssuesNotification(userIndex, dbClient, durations);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.ruleDao()).thenReturn(ruleDao);
    when(componentDao.selectByUuids(same(dbSession), anyCollection())).thenReturn(Collections.emptyList());
  }

  @Test
  public void set_project_without_branch() {
    underTest.setProject("project-key", "project-long-name", null);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_BRANCH)).isNull();
  }

  @Test
  public void set_project_with_branch() {
    underTest.setProject("project-key", "project-long-name", "feature");

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_BRANCH)).isEqualTo("feature");
  }

  @Test
  public void set_project_version() {
    String version = randomAlphanumeric(5);

    underTest.setProjectVersion(version);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_VERSION)).isEqualTo(version);
  }

  @Test
  public void set_project_version_supports_null() {
    underTest.setProjectVersion(null);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_VERSION)).isNull();

  }

  @Test
  public void set_date() {
    Date date = new Date();

    underTest.setAnalysisDate(date);

    assertThat(underTest.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_DATE)).isEqualTo(DateUtils.formatDateTime(date));
  }

  @Test
  public void set_statistics() {
    addIssueNTimes(newIssue1(), 5);
    addIssueNTimes(newIssue2(), 3);
    when(componentDao.selectByUuids(dbSession, ImmutableSet.of("file-uuid", "directory-uuid")))
      .thenReturn(Arrays.asList(
        new ComponentDto().setUuid("file-uuid").setName("file-name"),
        new ComponentDto().setUuid("directory-uuid").setName("directory-name")));
    RuleKey rule1 = RuleKey.of("SonarQube", "rule-the-world");
    RuleKey rule2 = RuleKey.of("SonarQube", "rule-the-universe");
    when(ruleDao.selectDefinitionByKeys(dbSession, ImmutableSet.of(rule1, rule2)))
      .thenReturn(
        ImmutableList.of(newRule(rule1, "Rule the World", "Java"), newRule(rule2, "Rule the Universe", "Clojure")));

    underTest.setStatistics("project-long-name", stats);

    assertThat(underTest.getFieldValue(RULE_TYPE + ".BUG.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(RULE_TYPE + ".CODE_SMELL.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo("maynard");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.label")).isEqualTo("keenan");
    assertThat(underTest.getFieldValue(ASSIGNEE + ".2.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(TAG + ".1.label")).isEqualTo("owasp");
    assertThat(underTest.getFieldValue(TAG + ".1.count")).isEqualTo("8");
    assertThat(underTest.getFieldValue(TAG + ".2.label")).isEqualTo("bug");
    assertThat(underTest.getFieldValue(TAG + ".2.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(COMPONENT + ".1.label")).isEqualTo("file-name");
    assertThat(underTest.getFieldValue(COMPONENT + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.label")).isEqualTo("directory-name");
    assertThat(underTest.getFieldValue(COMPONENT + ".2.count")).isEqualTo("3");
    assertThat(underTest.getFieldValue(RULE + ".1.label")).isEqualTo("Rule the World (Java)");
    assertThat(underTest.getFieldValue(RULE + ".1.count")).isEqualTo("5");
    assertThat(underTest.getFieldValue(RULE + ".2.label")).isEqualTo("Rule the Universe (Clojure)");
    assertThat(underTest.getFieldValue(RULE + ".2.count")).isEqualTo("3");
    assertThat(underTest.getDefaultMessage()).startsWith("8 new issues on project-long-name");
  }

  @Test
  public void add_only_5_assignees_with_biggest_issue_counts() {
    String[] assignees = IntStream.range(0, 6 + random.nextInt(10)).mapToObj(s -> "assignee" + s).toArray(String[]::new);
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    int i = assignees.length;
    for (String assignee : assignees) {
      IntStream.range(0, i).mapToObj(j -> new DefaultIssue().setType(randomRuleType).setAssignee(assignee)).forEach(stats::add);
      i--;
    }

    underTest.setStatistics(randomAlphanumeric(20), stats);

    for (int j = 0; j < 5; j++) {
      String fieldBase = ASSIGNEE + "." + (j + 1);
      assertThat(underTest.getFieldValue(fieldBase + ".label")).as("label of %s", fieldBase).isEqualTo(assignees[j]);
      assertThat(underTest.getFieldValue(fieldBase + ".count")).as("count of %s", fieldBase).isEqualTo(String.valueOf(assignees.length - j));
    }
    assertThat(underTest.getFieldValue(ASSIGNEE + ".6.label")).isNull();
    assertThat(underTest.getFieldValue(ASSIGNEE + ".6.count")).isNull();
  }

  @Test
  public void add_only_5_components_with_biggest_issue_counts() {
    String[] componentUuids = IntStream.range(0, 6 + random.nextInt(10)).mapToObj(s -> "component_uuid_" + s).toArray(String[]::new);
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    int i = componentUuids.length;
    for (String component : componentUuids) {
      IntStream.range(0, i).mapToObj(j -> new DefaultIssue().setType(randomRuleType).setComponentUuid(component)).forEach(stats::add);
      i--;
    }
    when(componentDao.selectByUuids(dbSession, Arrays.stream(componentUuids).limit(5).collect(Collectors.toSet())))
      .thenReturn(
        Arrays.stream(componentUuids).map(uuid -> new ComponentDto().setUuid(uuid).setName("name_" + uuid)).collect(MoreCollectors.toList()));

    underTest.setStatistics(randomAlphanumeric(20), stats);

    for (int j = 0; j < 5; j++) {
      String fieldBase = COMPONENT + "." + (j + 1);
      assertThat(underTest.getFieldValue(fieldBase + ".label")).as("label of %s", fieldBase).isEqualTo("name_" + componentUuids[j]);
      assertThat(underTest.getFieldValue(fieldBase + ".count")).as("count of %s", fieldBase).isEqualTo(String.valueOf(componentUuids.length - j));
    }
    assertThat(underTest.getFieldValue(COMPONENT + ".6.label")).isNull();
    assertThat(underTest.getFieldValue(COMPONENT + ".6.count")).isNull();
  }

  @Test
  public void add_only_5_rules_with_biggest_issue_counts() {
    String repository = randomAlphanumeric(4);
    String[] ruleKeys = IntStream.range(0, 6 + random.nextInt(10)).mapToObj(s -> "rule_" + s).toArray(String[]::new);
    NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats(i -> true);
    int i = ruleKeys.length;
    for (String ruleKey : ruleKeys) {
      IntStream.range(0, i).mapToObj(j -> new DefaultIssue().setType(randomRuleType).setRuleKey(RuleKey.of(repository, ruleKey))).forEach(stats::add);
      i--;
    }
    when(ruleDao.selectDefinitionByKeys(dbSession, Arrays.stream(ruleKeys).limit(5).map(s -> RuleKey.of(repository, s)).collect(MoreCollectors.toSet(5))))
      .thenReturn(
        Arrays.stream(ruleKeys).limit(5).map(ruleKey -> new RuleDefinitionDto()
          .setRuleKey(RuleKey.of(repository, ruleKey))
          .setName("name_" + ruleKey)
          .setLanguage("language_" + ruleKey))
          .collect(MoreCollectors.toList(5)));

    underTest.setStatistics(randomAlphanumeric(20), stats);

    for (int j = 0; j < 5; j++) {
      String fieldBase = RULE + "." + (j + 1);
      assertThat(underTest.getFieldValue(fieldBase + ".label")).as("label of %s", fieldBase).isEqualTo("name_" + ruleKeys[j] + " (language_" + ruleKeys[j] + ")");
      assertThat(underTest.getFieldValue(fieldBase + ".count")).as("count of %s", fieldBase).isEqualTo(String.valueOf(ruleKeys.length - j));
    }
    assertThat(underTest.getFieldValue(RULE + ".6.label")).isNull();
    assertThat(underTest.getFieldValue(RULE + ".6.count")).isNull();
  }

  @Test
  public void set_debt() {
    when(durations.format(any(Duration.class))).thenReturn("55 min");

    underTest.setDebt(Duration.create(55));

    assertThat(underTest.getFieldValue(EFFORT + ".count")).isEqualTo("55 min");
  }

  private void addIssueNTimes(DefaultIssue issue, int times) {
    for (int i = 0; i < times; i++) {
      stats.add(issue);
    }
  }

  private DefaultIssue newIssue1() {
    return new DefaultIssue()
      .setAssignee("maynard")
      .setComponentUuid("file-uuid")
      .setType(RuleType.BUG)
      .setTags(Lists.newArrayList("bug", "owasp"))
      .setRuleKey(RuleKey.of("SonarQube", "rule-the-world"))
      .setEffort(Duration.create(5L));
  }

  private DefaultIssue newIssue2() {
    return new DefaultIssue()
      .setAssignee("keenan")
      .setComponentUuid("directory-uuid")
      .setType(RuleType.CODE_SMELL)
      .setTags(Lists.newArrayList("owasp"))
      .setRuleKey(RuleKey.of("SonarQube", "rule-the-universe"))
      .setEffort(Duration.create(10L));
  }

  private RuleDefinitionDto newRule(RuleKey ruleKey, String name, String language) {
    return new RuleDefinitionDto()
      .setRuleKey(ruleKey)
      .setName(name)
      .setLanguage(language);
  }
}
