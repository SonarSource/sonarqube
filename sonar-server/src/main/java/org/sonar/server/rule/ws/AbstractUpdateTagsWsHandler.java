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

import com.google.common.collect.Sets;
import org.elasticsearch.common.collect.Lists;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.Rules;

import java.util.Set;

public abstract class AbstractUpdateTagsWsHandler implements RequestHandler {

  private final Rules rules;

  protected AbstractUpdateTagsWsHandler(Rules rules) {
    this.rules = rules;
  }

  @Override
  public void handle(Request request, Response response) {
    Rule rule = rules.findByKey(RuleKey.parse(request.mandatoryParam("key")));
    Set<String> allAdminTags = Sets.newHashSet(rule.adminTags());
    String[] tagsFromRequest = request.mandatoryParam("tags").split(",");
    updateTags(allAdminTags, tagsFromRequest);
    rules.updateRuleTags(rule.id(), Lists.newArrayList(allAdminTags));

    response.noContent();
  }

  protected abstract void updateTags(Set<String> currentTags, String[] tagsFromRequest);
}
