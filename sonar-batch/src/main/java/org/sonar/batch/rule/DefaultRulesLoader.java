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

import org.apache.commons.io.IOUtils;

import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;

import javax.annotation.Nullable;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonarqube.ws.Rules.ListResponse.Rule;
import org.sonarqube.ws.Rules.ListResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DefaultRulesLoader implements RulesLoader {
  private static final String RULES_SEARCH_URL = "/api/rules/list";

  private final WSLoader wsLoader;

  public DefaultRulesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public List<Rule> load(@Nullable MutableBoolean fromCache) {
    WSLoaderResult<InputStream> result = wsLoader.loadStream(RULES_SEARCH_URL);
    ListResponse list = loadFromStream(result.get());
    if (fromCache != null) {
      fromCache.setValue(result.isFromCache());
    }
    return list.getRulesList();
  }

  private static ListResponse loadFromStream(InputStream is) {
    try {
      return ListResponse.parseFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get rules", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

}
