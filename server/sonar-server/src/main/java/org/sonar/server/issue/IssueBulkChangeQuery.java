/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.util.RubyUtils;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * @since 3.7
 */
public class IssueBulkChangeQuery {

  private List<String> issues;
  private List<String> actions;
  private boolean hasComment;
  private boolean sendNotifications;

  Map<String, Map<String, Object>> propertiesByActions = new HashMap<>();

  public IssueBulkChangeQuery(Map<String, Object> props, String comment, boolean sendNotifications) {
    this.sendNotifications = sendNotifications;
    parse(props, comment);
  }

  @VisibleForTesting
  IssueBulkChangeQuery(Map<String, Object> props, boolean sendNotifications) {
    this.sendNotifications = sendNotifications;
    parse(props, null);
  }

  private void parse(Map<String, Object> props, @Nullable String comment) {
    this.issues = sanitizeList(RubyUtils.toStrings(props.get("issues")));
    if (issues == null || issues.isEmpty()) {
      throw new BadRequestException("issue_bulk_change.error.empty_issues");
    }
    actions = sanitizeList(RubyUtils.toStrings(props.get("actions")));
    if (actions == null || actions.isEmpty()) {
      throw new BadRequestException("issue_bulk_change.error.need_one_action");
    }
    for (String action : actions) {
      Map<String, Object> actionProperties = getActionProps(action, props);
      propertiesByActions.put(action, actionProperties);
    }
    if (!Strings.isNullOrEmpty(comment)) {
      hasComment = true;
      Map<String, Object> commentMap = newHashMap();
      commentMap.put(CommentAction.COMMENT_PROPERTY, comment);
      propertiesByActions.put(CommentAction.COMMENT_KEY, commentMap);
    }
  }

  private static List<String> sanitizeList(@Nullable List<String> list) {
    if (list == null || list.isEmpty()) {
      return Collections.emptyList();
    }
    return newArrayList(Iterables.filter(list, StringIsNotNull.INSTANCE));
  }

  public List<String> issues() {
    return issues;
  }

  /**
   * The list of actions to apply
   * Note that even if a comment has been added, this list will NOT contains the comment action
   */
  public List<String> actions() {
    return actions;
  }

  public boolean hasComment() {
    return hasComment;
  }

  public boolean sendNotifications() {
    return sendNotifications;
  }

  public Map<String, Object> properties(String action) {
    return propertiesByActions.get(action);
  }

  private static Map<String, Object> getActionProps(String action, Map<String, Object> props) {
    Map<String, Object> actionProps = newHashMap();
    for (Map.Entry<String, Object> propsEntry : props.entrySet()) {
      String key = propsEntry.getKey();
      String actionPrefix = action + ".";
      String property = StringUtils.substringAfter(key, actionPrefix);
      if (!property.isEmpty()) {
        actionProps.put(property, propsEntry.getValue());
      }
    }
    props.get(action);
    return actionProps;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  private enum StringIsNotNull implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable String input) {
      return !Strings.isNullOrEmpty(input);
    }
  }
}
