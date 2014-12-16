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

import org.apache.commons.collections.CollectionUtils;
import org.sonar.api.ServerComponent;
import org.sonar.core.issue.IssueUpdater;

import java.util.Collection;


public class RemoveTagsAction extends AbstractChangeTagsAction implements ServerComponent {

  public static final String KEY = "remove_tags";

  public RemoveTagsAction(IssueUpdater issueUpdater) {
    super(KEY, issueUpdater);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Collection<String> getTagsToSet(Context context, Collection<String> tagsFromParams) {
    return CollectionUtils.subtract(context.issue().tags(), tagsFromParams);
  }
}
