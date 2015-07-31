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

import org.sonar.batch.bootstrap.AbstractServerLoader;

import org.sonar.batch.bootstrap.WSLoaderResult;
import org.sonarqube.ws.Rules.ListResponse.Rule;
import com.google.common.io.ByteSource;
import org.sonarqube.ws.Rules.ListResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.sonar.batch.bootstrap.WSLoader;

public class DefaultRulesLoader extends AbstractServerLoader implements RulesLoader {
  private static final String RULES_SEARCH_URL = "/api/rules/list";

  private final WSLoader wsLoader;

  public DefaultRulesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public List<Rule> load() {
    WSLoaderResult<ByteSource> result = wsLoader.loadSource(RULES_SEARCH_URL);
    ListResponse list = loadFromSource(result.get());
    super.loadedFromCache = result.isFromCache();
    return list.getRulesList();
  }

  private static ListResponse loadFromSource(ByteSource input) {
    try (InputStream is = input.openStream()) {
      return ListResponse.parseFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get previous issues", e);
    }
  }

}
