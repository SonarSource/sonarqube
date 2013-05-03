/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.api.issue;

import org.sonar.api.ServerComponent;
import org.sonar.api.component.Component;
import org.sonar.api.rules.Rule;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Search for issues. This component can be used only by server-side extensions. Batch extensions should
 * use the perspective {@link Issuable}.
 *
 * @since 3.6
 */
public interface IssueFinder extends ServerComponent {

  interface Results {
    List<Issue> issues();

    Rule rule(Issue issue);

    Collection<Rule> rules();

    Component component(Issue issue);

    Collection<Component> components();

    Collection<ActionPlan> actionPlans(Issue issue);

    Collection<ActionPlan> actionPlans();

    Paging paging();

    boolean securityExclusions();
  }

  Results find(IssueQuery query, @Nullable Integer currentUserId, String role);

  @CheckForNull
  Issue findByKey(String key /* TODO @Nullable Integer currentUserId */);

  /*
  Map<RuleKey, Rule> rules(Collection<Issue> issues);

  Map<String, Component> components(Collection<Issue> issues);
*/
}
