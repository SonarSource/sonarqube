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

package org.sonar.server.issue;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RemoveTagsAction extends Action implements ServerComponent {

  public static final String KEY = "remove_tags";

  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final IssueUpdater issueUpdater;

  public RemoveTagsAction(IssueUpdater issueUpdater) {
    super(KEY);
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, List<Issue> issues, UserSession userSession){
    parseTags(properties);
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean execute(Map<String, Object> properties, Context context) {
    Collection<String> tags = CollectionUtils.subtract(context.issue().tags(), parseTags(properties));
    return issueUpdater.setTags((DefaultIssue) context.issue(), tags, context.issueChangeContext());
  }

  private Set<String> parseTags(Map<String, Object> properties) {
    Set<String> result = Sets.newHashSet();
    String tagsString = (String) properties.get("tags");
    if (!Strings.isNullOrEmpty(tagsString)) {
      for(String tag: TAGS_SPLITTER.split(tagsString)) {
        RuleTagFormat.validate(tag);
        result.add(tag);
      }
    }
    return result;
  }
}
