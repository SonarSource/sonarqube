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
package org.sonar.batch.rule;

import org.sonar.batch.util.BatchUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonarqube.ws.Rules.Active;
import org.sonarqube.ws.Rules.Active.Param;
import org.sonarqube.ws.Rules.ActiveList;
import org.sonarqube.ws.Rules.Rule;
import org.sonarqube.ws.Rules.SearchResponse;

public class DefaultActiveRulesLoader implements ActiveRulesLoader {
  private static final String RULES_SEARCH_URL = "/api/rules/search.protobuf?f=repo,name,severity,lang,internalKey,templateKey,params,actives&activation=true";

  private final WSLoader wsLoader;

  public DefaultActiveRulesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public List<LoadedActiveRule> load(String qualityProfileKey, @Nullable MutableBoolean fromCache) {
    List<LoadedActiveRule> ruleList = new LinkedList<>();
    int page = 1;
    int pageSize = 500;
    int loaded = 0;

    while (true) {
      WSLoaderResult<InputStream> result = wsLoader.loadStream(getUrl(qualityProfileKey, page, pageSize));
      SearchResponse response = loadFromStream(result.get());
      List<LoadedActiveRule> pageRules = readPage(response);
      ruleList.addAll(pageRules);
      loaded += response.getPs();

      if (response.getTotal() <= loaded) {
        break;
      }
      page++;
      if (fromCache != null) {
        fromCache.setValue(result.isFromCache());
      }
    }

    return ruleList;
  }

  private static String getUrl(String qualityProfileKey, int page, int pageSize) {
    StringBuilder builder = new StringBuilder(1024);
    builder.append(RULES_SEARCH_URL);
    builder.append("&qprofile=").append(BatchUtils.encodeForUrl(qualityProfileKey));
    builder.append("&p=").append(page);
    builder.append("&ps=").append(pageSize);
    return builder.toString();
  }

  private static SearchResponse loadFromStream(InputStream is) {
    try {
      return SearchResponse.parseFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load quality profiles", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  private static List<LoadedActiveRule> readPage(SearchResponse response) {
    List<LoadedActiveRule> loadedRules = new LinkedList<>();

    List<Rule> rulesList = response.getRulesList();
    Map<String, ActiveList> actives = response.getActives().getActives();

    for (Rule r : rulesList) {
      ActiveList activeList = actives.get(r.getKey());
      Active active = activeList.getActiveList(0);

      LoadedActiveRule loadedRule = new LoadedActiveRule();

      loadedRule.setRuleKey(RuleKey.parse(r.getKey()));
      loadedRule.setName(r.getName());
      loadedRule.setSeverity(active.getSeverity());
      loadedRule.setLanguage(r.getLang());
      loadedRule.setInternalKey(r.getInternalKey());
      if (r.hasTemplateKey()) {
        RuleKey templateRuleKey = RuleKey.parse(r.getTemplateKey());
        loadedRule.setTemplateRuleKey(templateRuleKey.rule());
      }

      Map<String, String> params = new HashMap<>();

      for (org.sonarqube.ws.Rules.Rule.Param param : r.getParams().getParamsList()) {
        params.put(param.getKey(), param.getDefaultValue());
      }

      // overrides defaultValue if the key is the same
      for (Param param : active.getParamsList()) {
        params.put(param.getKey(), param.getValue());
      }
      loadedRule.setParams(params);
      loadedRules.add(loadedRule);
    }

    return loadedRules;
  }
}
