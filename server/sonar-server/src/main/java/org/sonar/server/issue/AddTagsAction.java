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

import com.google.common.collect.Sets;
import org.sonar.api.ServerSide;
import org.sonar.core.issue.IssueUpdater;

import java.util.Collection;
import java.util.Set;

@ServerSide
public class AddTagsAction extends AbstractChangeTagsAction {

  public static final String KEY = "add_tags";

  public AddTagsAction(IssueUpdater issueUpdater) {
    super(KEY, issueUpdater);
  }

  @Override
  protected Collection<String> getTagsToSet(Context context, Collection<String> tagsFromParams) {
    Set<String> allTags = Sets.newHashSet(context.issue().tags());
    allTags.addAll(tagsFromParams);
    return allTags;
  }
}
