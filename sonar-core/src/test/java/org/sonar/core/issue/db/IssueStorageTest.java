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
package org.sonar.core.issue.db;

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueComment;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.Date;

public class IssueStorageTest extends AbstractDaoTestCase {

  IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

  @Test
  public void should_insert_new_issues() throws Exception {
    FakeSaver saver = new FakeSaver(getMyBatis(), new FakeRuleFinder());

    IssueComment comment = IssueComment.create("emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDate("2013-05-18");
    DefaultIssue issue = new DefaultIssue();
    issue.setKey("ABCDE");
    issue.setRuleKey(RuleKey.of("squid", "AvoidCycle"));
    issue.setLine(5000);
    issue.setNew(true);
    issue.setUserLogin("emmerik");
    issue.setResolution("OPEN").setStatus("OPEN").setSeverity("BLOCKER");
    issue.setAttribute("foo", "bar");
    issue.addComment(comment);
    issue.setCreationDate(date);
    issue.setUpdateDate(date);
    issue.setCloseDate(date);

    saver.save(issue);

    checkTables("should_insert_new_issues", new String[]{"id", "created_at", "updated_at"}, "issues", "issue_changes");
  }

  @Test
  public void should_update_issues() throws Exception {
    setupData("should_update_issues");

    FakeSaver saver = new FakeSaver(getMyBatis(), new FakeRuleFinder());

    IssueComment comment = IssueComment.create("emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDate("2013-05-18");
    DefaultIssue issue = new DefaultIssue();
    issue.setKey("ABCDE");
    issue.setRuleKey(RuleKey.of("squid", "AvoidCycle"));
    issue.setLine(5000);
    issue.setNew(false);
    issue.setChecksum("FFFFF");
    issue.setAuthorLogin("simon");
    issue.setAssignee("loic");
    issue.setFieldDiff(context, "severity", "INFO", "BLOCKER");
    issue.setUserLogin("emmerik");
    issue.setResolution("FIXED").setStatus("RESOLVED").setSeverity("BLOCKER");
    issue.setAttribute("foo", "bar");
    issue.addComment(comment);
    issue.setCreationDate(date);
    issue.setUpdateDate(date);
    issue.setCloseDate(date);

    saver.save(issue);

    checkTables("should_update_issues", new String[]{"id", "created_at", "updated_at"}, "issues", "issue_changes");
  }

  static class FakeSaver extends IssueStorage {
    protected FakeSaver(MyBatis mybatis, RuleFinder ruleFinder) {
      super(mybatis, ruleFinder);
    }

    @Override
    protected int componentId(DefaultIssue issue) {
      return 100;
    }
  }

  static class FakeRuleFinder implements RuleFinder {

    @Override
    public Rule findById(int ruleId) {
      return null;
    }

    @Override
    public Rule findByKey(String repositoryKey, String key) {
      return null;
    }

    @Override
    public Rule findByKey(RuleKey key) {
      Rule rule = new Rule().setRepositoryKey(key.repository()).setKey(key.rule());
      rule.setId(200);
      return rule;
    }

    @Override
    public Rule find(RuleQuery query) {
      return null;
    }

    @Override
    public Collection<Rule> findAll(RuleQuery query) {
      return null;
    }
  }
}
