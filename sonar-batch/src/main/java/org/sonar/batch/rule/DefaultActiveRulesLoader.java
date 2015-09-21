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

import org.sonarqube.ws.Rules.SearchResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;

import javax.annotation.Nullable;

import org.sonarqube.ws.Rules.Rule;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DefaultActiveRulesLoader implements ActiveRulesLoader {
  private static final String RULES_SEARCH_URL = "/api/rules/search?f=repo,name,severity,lang,internalKey,templateKey";

  private final WSLoader wsLoader;

  public DefaultActiveRulesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public List<Rule> load(String qualityProfileKey, @Nullable MutableBoolean fromCache) {
    WSLoaderResult<InputStream> result = wsLoader.loadStream(getUrl(qualityProfileKey));
    List<Rule> ruleList = loadFromStream(result.get());
    if (fromCache != null) {
      fromCache.setValue(result.isFromCache());
    }
    return ruleList;
  }

  private static String getUrl(String qualityProfileKey) {
    return RULES_SEARCH_URL + "&qprofile=" + qualityProfileKey;
  }

  private static List<Rule> loadFromStream(InputStream is) {
    try {
      SearchResponse response = SearchResponse.parseFrom(is);
      return response.getRulesList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load quality profiles", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

}
