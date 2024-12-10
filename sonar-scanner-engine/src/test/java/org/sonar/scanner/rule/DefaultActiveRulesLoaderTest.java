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
package org.sonar.scanner.rule;

import com.google.gson.Gson;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.scanner.rule.DefaultActiveRulesLoader.ActiveRuleGson;
import static org.sonar.scanner.rule.DefaultActiveRulesLoader.ParamGson;
import static org.sonar.scanner.rule.DefaultActiveRulesLoader.RuleKeyGson;

class DefaultActiveRulesLoaderTest {

  private static final int NUMBER_OF_RULES = 150;
  private static final RuleKey EXAMPLE_KEY = RuleKey.of("java", "S108");
  private static final RuleKey CUSTOM_RULE_KEY = RuleKey.of("java", "my-custom-rule");
  private static final String FORMAT_KEY = "format";
  private static final String FORMAT_VALUE = "^[a-z][a-zA-Z0-9]*$";
  private static final String PROJECT_KEY = "myProjectKey";
  private static final String SEVERITY_VALUE = Severity.MINOR;

  private DefaultActiveRulesLoader loader;
  private DefaultScannerWsClient wsClient;

  @BeforeEach
  void setUp() {
    wsClient = mock(DefaultScannerWsClient.class);
    BranchConfiguration branchConfig = mock(BranchConfiguration.class);
    when(branchConfig.isPullRequest()).thenReturn(false);
    loader = new DefaultActiveRulesLoader(wsClient);
  }

  @Test
  void load_shouldRequestRulesAndParseResponse() {
    WsTestUtil.mockReader(wsClient, getUrl(), response());

    Map<RuleKey, LoadedActiveRule> activeRulesByKey = loader.load(PROJECT_KEY).stream().collect(Collectors.toMap(LoadedActiveRule::getRuleKey, r -> r));
    assertThat(activeRulesByKey).hasSize(NUMBER_OF_RULES);

    var exampleRule = activeRulesByKey.get(EXAMPLE_KEY);
    assertThat(exampleRule.getParams()).containsEntry(FORMAT_KEY, FORMAT_VALUE);
    assertThat(exampleRule.getSeverity()).isEqualTo(SEVERITY_VALUE);
    assertThat(exampleRule.getImpacts()).containsExactly(entry(SoftwareQuality.MAINTAINABILITY, HIGH));

    var customRule = activeRulesByKey.get(CUSTOM_RULE_KEY);
    assertThat(customRule.getTemplateRuleKey()).isEqualTo("ruleTemplate");

    WsTestUtil.verifyCall(wsClient, getUrl());

    verifyNoMoreInteractions(wsClient);
  }

  private String getUrl() {
    return "/api/v2/analysis/active_rules?projectKey=" + PROJECT_KEY;
  }

  private Reader response() {
    List<ActiveRuleGson> activeRules = new ArrayList<>();

    IntStream.rangeClosed(1, NUMBER_OF_RULES - 1)
      .mapToObj(i -> RuleKey.of("java", "S" + i))
      .forEach(key -> {
        ActiveRuleGsonBuilder builder = new ActiveRuleGsonBuilder();

        builder.setRuleKey(new RuleKeyGson(key.repository(), key.rule()));

        builder.setCreatedAt("2014-05-27T15:50:45+0100");
        builder.setUpdatedAt("2014-05-27T15:50:45+0100");
        if (EXAMPLE_KEY.equals(key)) {
          builder.setParams(List.of(new ParamGson(FORMAT_KEY, FORMAT_VALUE)));
          builder.setSeverity(SEVERITY_VALUE);
          builder.setImpacts(Map.of(SoftwareQuality.MAINTAINABILITY, HIGH));
        }

        activeRules.add(builder.build());
      });

    ActiveRuleGsonBuilder builder = new ActiveRuleGsonBuilder();
    builder.setRuleKey(new RuleKeyGson(CUSTOM_RULE_KEY.repository(), CUSTOM_RULE_KEY.rule()));
    builder.setCreatedAt("2014-05-27T15:50:45+0100");
    builder.setUpdatedAt("2014-05-27T15:50:45+0100");
    builder.setTemplateRuleKey("java:ruleTemplate");
    activeRules.add(builder.build());

    return toReader(activeRules);
  }

  private static Reader toReader(List<ActiveRuleGson> activeRules) {
    String json = new Gson().toJson(activeRules);
    return new StringReader(json);
  }

  private static class ActiveRuleGsonBuilder {
    private RuleKeyGson ruleKey;
    private String name;
    private String severity;
    private String createdAt;
    private String updatedAt;
    private String internalKey;
    private String language;
    private String templateRuleKey;
    private String qProfilKey;
    private final List<RuleKeyGson> deprecatedKeys = new ArrayList<>();
    private final List<ParamGson> params = new ArrayList<>();
    private final Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = new EnumMap<>(SoftwareQuality.class);

    public void setRuleKey(RuleKeyGson ruleKey) {
      this.ruleKey = ruleKey;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setSeverity(String severity) {
      this.severity = severity;
    }

    public void setCreatedAt(String createdAt) {
      this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
      this.updatedAt = updatedAt;
    }

    public void setInternalKey(String internalKey) {
      this.internalKey = internalKey;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public void setTemplateRuleKey(String templateRuleKey) {
      this.templateRuleKey = templateRuleKey;
    }

    public void setQProfilKey(String qProfilKey) {
      this.qProfilKey = qProfilKey;
    }

    public void setParams(List<ParamGson> params) {
      this.params.clear();
      this.params.addAll(params);
    }

    public void addAllDeprecatedKeys(List<RuleKeyGson> deprecatedKeys) {
      this.deprecatedKeys.addAll(deprecatedKeys);
    }

    public void setImpacts(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
      this.impacts.clear();
      this.impacts.putAll(impacts);
    }

    public ActiveRuleGson build() {
      return new ActiveRuleGson(ruleKey, name, severity, createdAt, updatedAt, internalKey, language, templateRuleKey, qProfilKey, deprecatedKeys, params, impacts);
    }
  }
}
