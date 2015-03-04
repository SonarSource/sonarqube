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

package org.sonar.server.view.index;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.test.DbTests;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ViewIndexerTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new ViewIndexDefinition(new Settings()));

  ViewIndexer indexer;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    esTester.truncateIndices();
    indexer = new ViewIndexer(new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao()), esTester.client());
    indexer.setEnabled(true);
  }

  @Test
  public void index_nothing() throws Exception {
    indexer.index();
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEqualTo(0L);
  }

  @Test
  public void index() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    indexer.index();

    List<ViewDoc> docs = esTester.getDocuments("views", "view", ViewDoc.class);
    assertThat(docs).hasSize(4);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, new Function<ViewDoc, String>() {
      @Override
      public String apply(ViewDoc doc) {
        return doc.uuid();
      }
    });

    assertThat(viewsByUuid.get("ABCD").projects()).containsOnly("JKLM");
    assertThat(viewsByUuid.get("EFGH").projects()).containsOnly("KLMN", "JKLM");
    assertThat(viewsByUuid.get("FGHI").projects()).containsOnly("JKLM");
    assertThat(viewsByUuid.get("IJKL").projects()).isEmpty();
  }

  @Test
  public void index_only_if_empty_do_nothing_when_index_already_exists() throws Exception {
    // Some views are not in the db
    dbTester.prepareDbUnit(getClass(), "index.xml");
    esTester.putDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW,
      new ViewDoc().setUuid("ABCD").setProjects(newArrayList("BCDE")));

    indexer.index();

    // ... But they shouldn't be indexed
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEqualTo(1L);
  }

  @Test
  public void index_root_view() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    indexer.index("EFGH");

    List<ViewDoc> docs = esTester.getDocuments("views", "view", ViewDoc.class);
    assertThat(docs).hasSize(2);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, new Function<ViewDoc, String>() {
      @Override
      public String apply(ViewDoc doc) {
        return doc.uuid();
      }
    });

    assertThat(viewsByUuid.get("EFGH").projects()).containsOnly("KLMN", "JKLM");
    assertThat(viewsByUuid.get("FGHI").projects()).containsOnly("JKLM");
  }

  @Test
  public void index_view_doc() throws Exception {
    indexer.index(new ViewDoc().setUuid("EFGH").setProjects(newArrayList("KLMN", "JKLM")));

    List<ViewDoc> docs = esTester.getDocuments("views", "view", ViewDoc.class);
    assertThat(docs).hasSize(1);

    ViewDoc view = docs.get(0);
    assertThat(view.uuid()).isEqualTo("EFGH");
    assertThat(view.projects()).containsOnly("KLMN", "JKLM");
  }

}
