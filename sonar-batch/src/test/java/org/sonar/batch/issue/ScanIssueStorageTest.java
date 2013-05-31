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
package org.sonar.batch.issue;

import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.resource.ResourceDao;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanIssueStorageTest extends AbstractDaoTestCase {
  @Test
  public void should_load_component_id_from_cache() throws Exception {
    SnapshotCache snapshotCache = mock(SnapshotCache.class);
    when(snapshotCache.get("struts:Action.java")).thenReturn(new Snapshot().setResourceId(123));

    ScanIssueStorage storage = new ScanIssueStorage(getMyBatis(), new FakeRuleFinder(), snapshotCache, new ResourceDao(getMyBatis()));
    int componentId = storage.componentId(new DefaultIssue().setComponentKey("struts:Action.java"));

    assertThat(componentId).isEqualTo(123);
  }

  @Test
  public void should_load_component_id_from_db() throws Exception {
    setupData("should_load_component_id_from_db");
    SnapshotCache snapshotCache = mock(SnapshotCache.class);
    when(snapshotCache.get("struts:Action.java")).thenReturn(null);

    ScanIssueStorage storage = new ScanIssueStorage(getMyBatis(), new FakeRuleFinder(), snapshotCache, new ResourceDao(getMyBatis()));
    int componentId = storage.componentId(new DefaultIssue().setComponentKey("struts:Action.java"));

    assertThat(componentId).isEqualTo(123);
  }

  @Test
  public void should_fail_to_load_component_id_if_unknown_component() throws Exception {
    setupData("should_fail_to_load_component_id_if_unknown_component");
    SnapshotCache snapshotCache = mock(SnapshotCache.class);
    when(snapshotCache.get("struts:Action.java")).thenReturn(null);

    ScanIssueStorage storage = new ScanIssueStorage(getMyBatis(), new FakeRuleFinder(), snapshotCache, new ResourceDao(getMyBatis()));
    try {
      storage.componentId(new DefaultIssue().setComponentKey("struts:Action.java"));
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Unknown component: struts:Action.java");
    }
  }

  @Test
  public void should_load_project_id() throws Exception {
    SnapshotCache snapshotCache = mock(SnapshotCache.class);
    when(snapshotCache.get("struts:Action.java")).thenReturn(new Snapshot().setResourceId(123).setRootProjectId(100));

    ScanIssueStorage storage = new ScanIssueStorage(getMyBatis(), new FakeRuleFinder(), snapshotCache, new ResourceDao(getMyBatis()));
    int projectId = storage.projectId(new DefaultIssue().setComponentKey("struts:Action.java"));

    assertThat(projectId).isEqualTo(100);
  }

  @Test
  public void should_fail_to_load_project_id_if_unknown_component() throws Exception {
    SnapshotCache snapshotCache = mock(SnapshotCache.class);
    when(snapshotCache.get("struts:Action.java")).thenReturn(null);

    ScanIssueStorage storage = new ScanIssueStorage(getMyBatis(), new FakeRuleFinder(), snapshotCache, new ResourceDao(getMyBatis()));
    try {
      storage.projectId(new DefaultIssue().setComponentKey("struts:Action.java"));
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Unknown component: struts:Action.java");
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
      Rule rule = Rule.create().setRepositoryKey(key.repository()).setKey(key.rule());
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
