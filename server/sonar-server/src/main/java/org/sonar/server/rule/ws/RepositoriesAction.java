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
package org.sonar.server.rule.ws;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleRepositories.Repository;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Pattern;

/**
 * @since 5.1
 */
public class RepositoriesAction implements RulesAction {

  private static final String LANGUAGE = "language";
  private static final String MATCH_ALL = ".*";
  private final RuleRepositories repositories;

  public RepositoriesAction(RuleRepositories repositories) {
    this.repositories = repositories;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String query = request.param("q");
    String languageKey = request.param(LANGUAGE);
    int pageSize = request.mandatoryParamAsInt("ps");

    JsonWriter json = response.newJsonWriter().beginObject().name("repositories").beginArray();
    for (Repo repo : listMatchingRepositories(query, languageKey, pageSize)) {
      json.beginObject().prop("key", repo.key).prop("name", repo.name).prop(LANGUAGE, repo.language).endObject();
    }
    json.endArray().endObject().close();
  }

  private Collection<Repo> listMatchingRepositories(@Nullable String query, @Nullable String languageKey, int pageSize) {
    Pattern pattern = Pattern.compile(query == null ? MATCH_ALL : MATCH_ALL + query + MATCH_ALL, Pattern.CASE_INSENSITIVE);

    SortedMap<String, Repo> reposByName = Maps.newTreeMap();
    Collection<Repo> repos = listRepositories(languageKey);

    for (Repo repo : repos) {
      if (pattern.matcher(repo.key).matches() || pattern.matcher(repo.name).matches()) {
        reposByName.put(repo.name + " -- " + repo.language, repo);
      }
    }

    List<Repo> result = Lists.newArrayList(reposByName.values());
    if (pageSize > 0 && pageSize < result.size()) {
      result = result.subList(0, pageSize);
    }
    return result;
  }

  private Collection<Repo> listRepositories(String languageKey) {
    List<Repo> allRepos = Lists.newArrayList();
    Collection<Repository> reposFromPlugins = languageKey == null ? repositories.repositories() : repositories.repositoriesForLang(languageKey);
    for (Repository repo: reposFromPlugins) {
      allRepos.add(new Repo(repo));
    }
    if (languageKey == null) {
      allRepos.add(new Repo(RuleKey.MANUAL_REPOSITORY_KEY, "Manual Rule", "None"));
    }
    return allRepos;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("repositories")
      .setDescription("List available rule repositories")
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-repositories.json"));

    action.createParam("q")
      .setDescription("A pattern to match repository keys/names against")
      .setExampleValue("squid");
    action.createParam(LANGUAGE)
      .setDescription("A language key; if provided, only repositories for the given language will be returned")
      .setExampleValue("java");
    action.createParam("ps")
      .setDescription("The size of the list to return, 0 for all repositories")
      .setExampleValue("25")
      .setDefaultValue("0");
  }

  private static final class Repo {
    private String key;
    private String name;
    private String language;

    private Repo(String key, String name, String language) {
      this.key = key;
      this.name = name;
      this.language = language;
    }

    private Repo(Repository repo) {
      this(repo.key(), repo.name(), repo.language());
    }
  }
}
