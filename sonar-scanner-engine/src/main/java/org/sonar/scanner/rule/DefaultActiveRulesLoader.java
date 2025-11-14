/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.scanner.http.ScannerWsClient;
import org.sonarqube.ws.client.GetRequest;

import static java.util.Optional.ofNullable;

public class DefaultActiveRulesLoader implements ActiveRulesLoader {
  private static final String RULES_ACTIVE_URL = "/api/v2/analysis/active_rules?";

  private final ScannerWsClient wsClient;

  public DefaultActiveRulesLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public List<LoadedActiveRule> load(String projectKey) {
    GetRequest getRequest = new GetRequest(getUrl(projectKey));
    List<ActiveRuleGson> jsonResponse;
    try (Reader reader = wsClient.call(getRequest).contentReader()) {
      jsonResponse = new Gson().fromJson(reader, new TypeToken<ArrayList<ActiveRuleGson>>() {
      }.getType());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load active rules", e);
    }
    return convert(jsonResponse);
  }

  private static String getUrl(String projectKey) {
    return RULES_ACTIVE_URL + "projectKey=" + projectKey;
  }

  private static List<LoadedActiveRule> convert(List<ActiveRuleGson> activeRuleGsonList) {
    return activeRuleGsonList.stream()
      .map(DefaultActiveRulesLoader::convertActiveRule)
      .toList();
  }

  private static LoadedActiveRule convertActiveRule(ActiveRuleGson activeRule) {
    LoadedActiveRule loadedRule = new LoadedActiveRule();
    loadedRule.setRuleKey(convertRuleKey(activeRule.ruleKey()));
    loadedRule.setName(activeRule.name());
    loadedRule.setSeverity(activeRule.severity());
    loadedRule.setCreatedAt(DateUtils.dateToLong(DateUtils.parseDateTime(activeRule.createdAt())));
    loadedRule.setUpdatedAt(DateUtils.dateToLong(DateUtils.parseDateTime(activeRule.updatedAt())));
    loadedRule.setLanguage(activeRule.language());
    loadedRule.setInternalKey(activeRule.internalKey());
    loadedRule.setQProfileKey(activeRule.qProfileKey());
    ofNullable(activeRule.templateRuleKey())
      .map(RuleKey::parse)
      .map(RuleKey::rule)
      .ifPresent(loadedRule::setTemplateRuleKey);
    loadedRule.setParams(activeRule.params() != null ? convertParams(activeRule.params()) : Map.of());
    loadedRule.setImpacts(activeRule.impacts() != null ? activeRule.impacts() : Map.of());
    loadedRule.setDeprecatedKeys(convertDeprecatedKeys(activeRule.deprecatedKeys()));
    return loadedRule;
  }

  private static Map<String, String> convertParams(List<ParamGson> params) {
    return params.stream().collect(Collectors.toMap(ParamGson::key, ParamGson::value));
  }

  private static Set<RuleKey> convertDeprecatedKeys(@Nullable List<RuleKeyGson> deprecatedKeysList) {
    return ofNullable(deprecatedKeysList)
      .orElse(List.of())
      .stream()
      .map(value -> RuleKey.of(value.repository(), value.rule()))
      .collect(Collectors.toSet());
  }

  private static RuleKey convertRuleKey(RuleKeyGson ruleKey) {
    return RuleKey.of(ruleKey.repository(), ruleKey.rule());
  }

  record ActiveRuleGson(
    @SerializedName("ruleKey") RuleKeyGson ruleKey,
    @SerializedName("name") String name,
    @SerializedName("severity") String severity,
    @SerializedName("createdAt") String createdAt,
    @SerializedName("updatedAt") String updatedAt,
    @SerializedName("internalKey") @Nullable String internalKey,
    @SerializedName("language") String language,
    @SerializedName("templateRuleKey") @Nullable String templateRuleKey,
    @SerializedName("qProfileKey") String qProfileKey,
    @SerializedName("deprecatedKeys") @Nullable List<RuleKeyGson> deprecatedKeys,
    @SerializedName("params") @Nullable List<ParamGson> params,
    @SerializedName("impacts") @Nullable Map<SoftwareQuality, Severity> impacts) {
  }

  record RuleKeyGson(@SerializedName("repository") String repository, @SerializedName("rule") String rule) {
  }

  record ParamGson(@SerializedName("key") String key, @SerializedName("value") String value) {
  }

}
