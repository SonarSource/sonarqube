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
package org.sonar.server.issue.ws;

import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.ActionPlanDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

/**
 * Loads issues and related information from database.
 */
public class IssueDataLoader {

  private final DbClient dbClient;
  private final UserSession userSession;

  public IssueDataLoader(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public Data load(List<String> issueKeys, Selection selection) {
    Data result = new Data();
    DbSession dbSession = dbClient.openSession(false);
    try {
      result.issues = dbClient.issueDao().selectByOrderedKeys(dbSession, issueKeys);
      Collector collector = collect(result.issues, selection);
      if (selection.contains(SelectionKey.RULE)) {
        result.rules = dbClient.ruleDao().selectByKeys(dbSession, collector.<RuleKey>get(SelectionKey.RULE));
      }
      if (selection.contains(SelectionKey.COMMENT)) {
        result.comments = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbSession, issueKeys, IssueChangeDto.TYPE_COMMENT);
        for (IssueChangeDto comment : result.comments) {
          collector.add(SelectionKey.USER, comment.getUserLogin());
        }
      }
      if (selection.contains(SelectionKey.USER)) {
        result.users = dbClient.userDao().selectByLogins(dbSession, collector.<String>get(SelectionKey.USER));
      }
      if (selection.contains(SelectionKey.ACTION_PLAN)) {
        result.actionPlans = dbClient.actionPlanDao().selectByKeys(dbSession, collector.<String>get(SelectionKey.ACTION_PLAN));
      }
      if (selection.contains(SelectionKey.COMPONENT) || selection.contains(SelectionKey.PROJECT)) {
        result.components = dbClient.componentDao().selectByKeys(dbSession, collector.<String>get(SelectionKey.COMPONENT));
        result.components.addAll(dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession, collector.<String>get(SelectionKey.COMPONENT)));
        for (ComponentDto component : result.components) {
          collector.add(SelectionKey.PROJECT, component.projectUuid());
        }
        if (selection.contains(SelectionKey.PROJECT)) {
          result.projects = dbClient.componentDao().selectByKeys(dbSession, collector.<String>get(SelectionKey.PROJECT));
          result.components.addAll(result.projects);
        }
      }
      return result;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private Collector collect(List<IssueDto> issues, Selection selection) {
    Collector collector = new Collector();
    collector.collect(issues);
    collector.collect(selection);
    if (userSession.isLoggedIn()) {
      collector.add(SelectionKey.USER, userSession.getLogin());
    }
    return collector;
  }

  public static class Data {
    private List<IssueDto> issues;
    private List<ComponentDto> components;
    private List<ComponentDto> projects;
    private List<UserDto> users;
    private List<RuleDto> rules;
    private List<ActionPlanDto> actionPlans;
    private List<IssueChangeDto> comments;
  }

  public static class Selection {
    private final EnumSet<SelectionKey> selectionKeys;
    private List<RuleKey> ruleKeys = null;
    private List<String> projectUuids = null;
    private List<String> componentUuids = null;
    private List<String> userLogins = null;
    private List<String> actionPlanKeys = null;

    public Selection(EnumSet<SelectionKey> selectionKeys) {
      this.selectionKeys = selectionKeys;
    }

    boolean contains(SelectionKey selectionKey) {
      return selectionKeys.contains(selectionKey);
    }
  }

  public enum SelectionKey {
    ACTION_PLAN, COMMENT, COMPONENT, PROJECT, RULE, USER
  }

  /**
   * Collects the keys of all the data to be loaded (users, rules, ...)
   */
  private static class Collector {
    private final SetMultimap<SelectionKey, Object> map = MultimapBuilder.enumKeys(SelectionKey.class).hashSetValues().build();

    void collect(List<IssueDto> issues) {
      for (IssueDto issue : issues) {
        add(SelectionKey.ACTION_PLAN, issue.getActionPlanKey());
        add(SelectionKey.COMPONENT, issue.getComponentUuid());
        add(SelectionKey.PROJECT, issue.getProjectUuid());
        add(SelectionKey.RULE, issue.getRuleKey());
        add(SelectionKey.USER, issue.getReporter());
        add(SelectionKey.USER, issue.getAssignee());
      }
    }

    void collect(Selection selection) {
      addAll(SelectionKey.ACTION_PLAN, selection.actionPlanKeys);
      addAll(SelectionKey.COMPONENT, selection.componentUuids);
      addAll(SelectionKey.PROJECT, selection.projectUuids);
      addAll(SelectionKey.RULE, selection.ruleKeys);
      addAll(SelectionKey.USER, selection.userLogins);

    }

    void add(SelectionKey key, @Nullable Object value) {
      if (value != null) {
        map.put(key, value);
      }
    }

    void addAll(SelectionKey key, @Nullable Collection values) {
      if (values != null) {
        for (Object value : values) {
          add(key, value);
        }
      }
    }

    <T> List<T> get(SelectionKey key) {
      return Lists.newArrayList((Set<T>) map.get(key));
    }
  }
}
