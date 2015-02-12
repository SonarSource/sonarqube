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

package org.sonar.server.computation.step;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PurgeRemovedViewsStepTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new ViewIndexDefinition(new Settings()));

  @ClassRule
  public static DbTester db = new DbTester();

  ComputationContext context;

  DbSession session;

  DbClient dbClient;

  PurgeRemovedViewsStep step;

  @Before
  public void setUp() {
    esTester.truncateIndices();
    db.truncateTables();

    context = mock(ComputationContext.class);
    session = db.myBatis().openSession(false);
    dbClient = new DbClient(db.database(), db.myBatis(), new IssueDao(db.myBatis()), new ComponentDao());
    step = new PurgeRemovedViewsStep(new ViewIndex(esTester.client()), dbClient);
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void purge_removed_views() throws Exception {
    when(context.getProject()).thenReturn(ComponentTesting.newProjectDto("DBCA").setQualifier(Qualifiers.VIEW));

    esTester.putDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW,
      new ViewDoc().setUuid("ABCD"),
      new ViewDoc().setUuid("BCDE"),
      // Should be removed as it no more exists in db
      new ViewDoc().setUuid("CDEF"));

    ComponentDto view = ComponentTesting.newProjectDto("ABCD").setQualifier(Qualifiers.VIEW);
    ComponentDto subView = ComponentTesting.newModuleDto("BCDE", view).setQualifier(Qualifiers.SUBVIEW);
    dbClient.componentDao().insert(session, view, subView);
    session.commit();

    step.execute(context);

    List<String> viewUuids = esTester.getDocumentFields(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, ViewIndexDefinition.FIELD_UUID);
    assertThat(viewUuids).containsOnly("ABCD", "BCDE");
  }

  @Test
  public void only_support_views() throws Exception {
    assertThat(step.supportedProjectQualifiers()).containsOnly(Qualifiers.VIEW);
  }

}
