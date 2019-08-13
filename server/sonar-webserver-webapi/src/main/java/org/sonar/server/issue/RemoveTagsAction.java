/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.server.ServerSide;

@ServerSide
public class RemoveTagsAction extends AbstractChangeTagsAction {

  public static final String KEY = "remove_tags";

  public RemoveTagsAction(IssueFieldsSetter issueUpdater) {
    super(KEY, issueUpdater);
  }

  @Override
  protected Collection<String> getTagsToSet(Context context, Collection<String> tagsFromParams) {
    Set<String> newTags = new HashSet<>(context.issue().tags());
    newTags.removeAll(tagsFromParams);
    return newTags;
  }
}
