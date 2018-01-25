/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
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
  public EsTester esTester = new EsTester(
    new RuleIndexDefinition(new MapSettings().asConfig()),
    new IssueIndexDefinition(new MapSettings().asConfig()),
    new ViewIndexDefinition(new MapSettings().asConfig()),
    new ProjectMeasuresIndexDefinition(new MapSettings().asConfig()),
    new ComponentIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private BackendCleanup underTest = new BackendCleanup(esTester.client(), dbTester.getDbClient());
  private OrganizationDto organization;

  @Before
  public void setUp() {
    organization = OrganizationTesting.newOrganizationDto();
  }

  @Test
  public void clear_db() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.clearDb();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
  }

  @Test
  public void clear_indexes() {
    esTester.putDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDocTesting.newDoc());
    esTester.putDocuments(RuleIndexDefinition.INDEX_TYPE_RULE, newRuleDoc());
    esTester.putDocuments(ComponentIndexDefinition.INDEX_TYPE_COMPONENT, newComponentDoc());

    underTest.clearIndexes();

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isEqualTo(0);
    assertThat(esTester.countDocuments(ComponentIndexDefinition.INDEX_TYPE_COMPONENT)).isEqualTo(0);
  }

  @Test
  public void clear_all() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    esTester.putDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDocTesting.newDoc());
    esTester.putDocuments(RuleIndexDefinition.INDEX_TYPE_RULE, newRuleDoc());
    esTester.putDocuments(ComponentIndexDefinition.INDEX_TYPE_COMPONENT, newComponentDoc());

    underTest.clearAll();

    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isEqualTo(0);
    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX_TYPE_RULE)).isEqualTo(0);
    assertThat(esTester.countDocuments(ComponentIndexDefinition.INDEX_TYPE_COMPONENT)).isEqualTo(0);

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
  }

  @Test
  public void reset_data() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    esTester.putDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE, IssueDocTesting.newDoc());
    esTester.putDocuments(ViewIndexDefinition.INDEX_TYPE_VIEW, new ViewDoc().setUuid("CDEF").setProjects(newArrayList("DEFG")));
    esTester.putDocuments(RuleIndexDefinition.INDEX_TYPE_RULE, newRuleDoc());
    esTester.putDocuments(ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES, new ProjectMeasuresDoc()
      .setId("PROJECT")
      .setKey("Key")
      .setName("Name"));
    esTester.putDocuments(ComponentIndexDefinition.INDEX_TYPE_COMPONENT, newComponentDoc());

    underTest.resetData();

    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("properties")).isZero();
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_TYPE_ISSUE)).isZero();
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX_TYPE_VIEW)).isZero();
    assertThat(esTester.countDocuments(ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES)).isZero();
    assertThat(esTester.countDocuments(ComponentIndexDefinition.INDEX_TYPE_COMPONENT)).isZero();

    // Rules should not be removed
    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(1);
    assertThat(esTester.countDocuments(RuleIndexDefinition.INDEX_TYPE_RULE)).isEqualTo(1);
  }

  private static RuleDoc newRuleDoc() {
    return new RuleDoc().setId(new Random().nextInt(942)).setKey(RuleTesting.XOO_X1.toString()).setRepository(RuleTesting.XOO_X1.repository());
  }

  private ComponentDoc newComponentDoc() {
    return ComponentIndexer.toDocument(ComponentTesting.newPrivateProjectDto(organization));
  }
}
