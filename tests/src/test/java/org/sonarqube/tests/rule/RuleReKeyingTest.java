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
package org.sonarqube.tests.rule;

import com.google.common.collect.ImmutableSet;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.concurrent.Immutable;
import org.assertj.core.api.iterable.ThrowingExtractor;
import org.junit.After;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.issues.SearchRequest;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.projectDir;

public class RuleReKeyingTest {

  private static Orchestrator orchestrator;
  private static Tester tester;

  @After
  public void tearDown() {
    if (tester != null) {
      tester.after();
    }
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void rules_are_re_keyed_when_upgrading_and_downgrading_plugin() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("foo-plugin-v1"))
      .build();
    orchestrator.start();

    tester = new Tester(orchestrator);
    tester.before();

    Set<RuleExtract> rules = verifyRules(
      arrayOf("foo:UnchangedRule",
        "foo:ChangedRule",
        "foo:ToBeDeactivatedRule",
        "foo:ToBeRemovedRule",
        "foo:RuleWithUnchangedParameter",
        "foo:RuleWithChangedParameter",
        "foo:RuleWithRemovedParameter",
        "foo:RuleWithAddedParameter",
        "foo:ToBeRenamed",
        "foo:ToBeRenamedAndMoved"),
      arrayOf("foo:Renamed",
        "foo2:RenamedAndMoved"));

    analyseProject("2017-12-31");
    List<Issues.Issue> issues = tester.wsClient().issues()
      .search(new SearchRequest().setProjects(singletonList("sample")))
      .getIssuesList();
    verifyRuleKey(issues, "foo:ToBeRenamed", "foo:ToBeRenamedAndMoved");
    verifyDate(issues, Issues.Issue::getCreationDate, "2017-12-31");
    verifyDate(issues, Issues.Issue::getUpdateDate, "2017-12-31");

    // uninstall plugin V1
    tester.wsClient().wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "foo")).failIfNotSuccessful();
    // install plugin V2
    File pluginsDir = new File(orchestrator.getServer().getHome() + "/extensions/plugins");
    orchestrator.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("foo-plugin-v2"), pluginsDir);

    orchestrator.restartServer();

    // one rule deleted, one rule added, two rules re-keyed
    Set<RuleExtract> rulesAfterUpgrade = verifyRules(
      arrayOf("foo:UnchangedRule",
        "foo:ChangedRule",
        "foo:ToBeDeactivatedRule",
        "foo:RuleWithUnchangedParameter",
        "foo:RuleWithChangedParameter",
        "foo:RuleWithRemovedParameter",
        "foo:RuleWithAddedParameter",
        "foo:Renamed",
        "foo2:RenamedAndMoved"),
      arrayOf("foo:ToBeRemovedRule"));
    assertThat(rulesAfterUpgrade).isEqualTo(rules.stream()
      .filter(t -> !"foo:ToBeRemovedRule".equals(t.ruleKey))
      .map(renamed("foo:ToBeRenamed", "foo:Renamed"))
      .map(renamed("foo:ToBeRenamedAndMoved", "foo2:RenamedAndMoved"))
      .collect(toSet()));

    analyseProject("2018-01-02");
    List<Issues.Issue> issuesAfterUpgrade = tester.wsClient().issues()
      .search(new SearchRequest().setProjects(singletonList("sample")))
      .getIssuesList();
    verifyRuleKey(issuesAfterUpgrade, "foo:Renamed", "foo2:RenamedAndMoved");
    verifyDate(issuesAfterUpgrade, Issues.Issue::getCreationDate, "2017-12-31");
    verifyDate(issuesAfterUpgrade, Issues.Issue::getUpdateDate, "2018-01-02");

    // uninstall plugin V2
    tester.wsClient().wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "foo")).failIfNotSuccessful();
    // install plugin V1
    orchestrator.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("foo-plugin-v1"), pluginsDir);

    orchestrator.restartServer();

    // new rule removed, removed rule recreated, two rules re-keyed back
    Set<RuleExtract> rulesAfterDowngrade = verifyRules(
      arrayOf("foo:UnchangedRule",
        "foo:ChangedRule",
        "foo:ToBeDeactivatedRule",
        "foo:ToBeRemovedRule",
        "foo:RuleWithUnchangedParameter",
        "foo:RuleWithChangedParameter",
        "foo:RuleWithRemovedParameter",
        "foo:RuleWithAddedParameter",
        "foo:ToBeRenamed",
        "foo:ToBeRenamedAndMoved"),
      arrayOf("foo:Renamed",
        "foo2:RenamedAndMoved"));
    assertThat(rulesAfterDowngrade).isEqualTo(rules);

    analyseProject("2018-01-16");
    List<Issues.Issue> issuesAfterDowngrade = tester.wsClient().issues()
      .search(new SearchRequest().setProjects(singletonList("sample")))
      .getIssuesList();
    verifyRuleKey(issuesAfterDowngrade, "foo:ToBeRenamed", "foo:ToBeRenamedAndMoved");
    verifyDate(issuesAfterDowngrade, Issues.Issue::getCreationDate, "2017-12-31");
    verifyDate(issuesAfterDowngrade, Issues.Issue::getUpdateDate, "2018-01-16");
  }

  /**
   * Verifies response from api/rules/search WS without filter returns rules for all keys in {@code contains} and
   * non for the keys in {@code doesNotContain}.
   *
   * @return a {@link RuleExtract} for each key in {@code contains} built from api/rules/search response
   */
  private Set<RuleExtract> verifyRules(String[] contains, String[] doesNotContain) {
    List<Rules.Rule> rules = tester.wsClient().rules().search(new org.sonarqube.ws.client.rules.SearchRequest())
      .getRulesList();

    assertThat(rules)
      .extracting(Rules.Rule::getKey)
      .contains(contains)
      .doesNotContain(doesNotContain);

    Set<String> expected = ImmutableSet.copyOf(contains);
    return rules.stream()
      .filter(t -> expected.contains(t.getKey()))
      .map(t -> new RuleExtract(t.getKey(), t.getCreatedAt()))
      .collect(toSet());
  }

  private Function<RuleExtract, RuleExtract> renamed(String oldRuleKey, String newRuleKey) {
    return ruleExtract -> {
      if (ruleExtract.ruleKey.equals(oldRuleKey)) {
        return ruleExtract.renameTo(newRuleKey);
      }
      return ruleExtract;
    };
  }

  private static void verifyRuleKey(List<Issues.Issue> issuesAfterDowngrade, String... ruleKeys) {
    assertThat(issuesAfterDowngrade)
      .extracting(Issues.Issue::getRule)
      .containsOnly(ruleKeys);
  }

  private static void verifyDate(List<Issues.Issue> issuesAfterUpgrade, ThrowingExtractor<Issues.Issue, String, RuntimeException> getUpdateDate, String date) {
    assertThat(issuesAfterUpgrade)
      .extracting(getUpdateDate)
      .usingElementComparator((a, b) -> a.startsWith(b) ? 0 : -1)
      .containsOnly(date + "T00:00:00");
  }

  private static String[] arrayOf(String... strings) {
    return strings;
  }

  @Immutable
  private static class RuleExtract {
    private final String ruleKey;
    private final String createdAt;

    private RuleExtract(String ruleKey, String createdAt) {
      this.ruleKey = ruleKey;
      this.createdAt = createdAt;
    }

    private RuleExtract renameTo(String newRuleKey) {
      return new RuleExtract(newRuleKey, createdAt);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RuleExtract that = (RuleExtract) o;
      return Objects.equals(ruleKey, that.ruleKey) &&
        Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, createdAt);
    }

    @Override
    public String toString() {
      return "{" + ruleKey + "@" + createdAt + '}';
    }
  }

  private void analyseProject(String projectDate) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("foo-sample"))
        .setProperty("sonar.projectDate", projectDate));
  }
}
