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
package org.sonar.server.platform;

import java.util.Random;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.component.index.ComponentDoc;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.IssueDocTesting;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresDoc;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class BackendCleanupTest {

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private BackendCleanup underTest = new BackendCleanup(es.client(), dbTester.getDbClient());
  private OrganizationDto organization;

  @Before
  public void setUp() {
    organization = OrganizationTesting.newOrganizationDto();
  }

  @Test
  public void clear_db() {
    insertSomeData();

    underTest.clearDb();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
  }

  @Test
  public void clear_indexes() {
    es.putDocuments(IssueIndexDefinition.TYPE_ISSUE, IssueDocTesting.newDoc());
    es.putDocuments(RuleIndexDefinition.TYPE_RULE, newRuleDoc());
    es.putDocuments(ComponentIndexDefinition.TYPE_COMPONENT, newComponentDoc());

    underTest.clearIndexes();

    assertThat(es.countDocuments(IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0);
    assertThat(es.countDocuments(ComponentIndexDefinition.TYPE_COMPONENT)).isEqualTo(0);
  }

  @Test
  public void clear_all() {
    insertSomeData();

    es.putDocuments(IssueIndexDefinition.TYPE_ISSUE, IssueDocTesting.newDoc());
    es.putDocuments(RuleIndexDefinition.TYPE_RULE, newRuleDoc());
    es.putDocuments(ComponentIndexDefinition.TYPE_COMPONENT, newComponentDoc());

    underTest.clearAll();

    assertThat(es.countDocuments(IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0);
    assertThat(es.countDocuments(RuleIndexDefinition.TYPE_RULE)).isEqualTo(0);
    assertThat(es.countDocuments(ComponentIndexDefinition.TYPE_COMPONENT)).isEqualTo(0);

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
  }

  @Test
  public void reset_data() {
    insertSomeData();

    es.putDocuments(IssueIndexDefinition.TYPE_ISSUE, IssueDocTesting.newDoc());
    es.putDocuments(ViewIndexDefinition.TYPE_VIEW, new ViewDoc().setUuid("CDEF").setProjects(newArrayList("DEFG")));
    es.putDocuments(RuleIndexDefinition.TYPE_RULE, newRuleDoc());
    es.putDocuments(ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES, new ProjectMeasuresDoc()
      .setId("PROJECT")
      .setKey("Key")
      .setName("Name"));
    es.putDocuments(ComponentIndexDefinition.TYPE_COMPONENT, newComponentDoc());

    underTest.resetData();

    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("properties")).isZero();
    assertThat(es.countDocuments(IssueIndexDefinition.TYPE_ISSUE)).isZero();
    assertThat(es.countDocuments(ViewIndexDefinition.TYPE_VIEW)).isZero();
    assertThat(es.countDocuments(ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES)).isZero();
    assertThat(es.countDocuments(ComponentIndexDefinition.TYPE_COMPONENT)).isZero();

    // Rules should not be removed
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(1);
    assertThat(es.countDocuments(RuleIndexDefinition.TYPE_RULE)).isEqualTo(1);
  }

  private void insertSomeData() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project);
    dbTester.rules().insert();
    dbTester.properties().insertProperty(new PropertyDto()
      .setKey("sonar.profile.java")
      .setValue("Sonar Way")
      .setResourceId(project.getId())
    );
  }

  private static RuleDoc newRuleDoc() {
    return new RuleDoc().setId(new Random().nextInt(942)).setKey(RuleTesting.XOO_X1.toString()).setRepository(RuleTesting.XOO_X1.repository());
  }

  private ComponentDoc newComponentDoc() {
    return ComponentIndexer.toDocument(ComponentTesting.newPrivateProjectDto(organization));
  }
}
