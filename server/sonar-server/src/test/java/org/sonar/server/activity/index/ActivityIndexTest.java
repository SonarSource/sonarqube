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
package org.sonar.server.activity.index;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.activity.Activity;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;

import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ActivityIndexTest {

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new ActivityIndexDefinition(new Settings()));

  ActivityIndex underTest;

  @Before
  public void before() {
    underTest = new ActivityIndex(es.client());
  }

  @Test
  public void search_all() throws Exception {
    es.putDocuments("activities", "activity", newDoc(1, 1_500_000_000_000L), newDoc(2, 1_600_000_000_000L));

    SearchResult<ActivityDoc> results = underTest.search(new ActivityQuery(), new SearchOptions());
    assertThat(results.getTotal()).isEqualTo(2L);
    assertThat(results.getDocs()).hasSize(2);
    assertThat(results.getDocs()).extracting("message").containsOnly("THE_MSG 1", "THE_MSG 2");
  }

  @Test
  public void search_by_type() throws Exception {
    es.putDocuments("activities", "activity", newDoc(1, 1_500_000_000_000L), newDoc(2, 1_600_000_000_000L));

    ActivityQuery query = new ActivityQuery();
    query.setTypes(Arrays.asList("ANALYSIS_REPORT"));
    assertThat(underTest.search(query, new SearchOptions()).getTotal()).isEqualTo(2L);

    query = new ActivityQuery();
    query.setTypes(Arrays.asList("OTHER", "TYPES"));
    assertThat(underTest.search(query, new SearchOptions()).getTotal()).isEqualTo(0L);
  }

  @Test
  public void search_by_data() throws Exception {
    es.putDocuments("activities", "activity", newDoc(1, 1_500_000_000_000L), newDoc(2, 1_600_000_000_000L));

    ActivityQuery query = new ActivityQuery();
    query.addDataOrFilter("foo", "bar2");
    SearchResult<ActivityDoc> results = underTest.search(query, new SearchOptions());
    assertThat(results.getDocs()).hasSize(1);
    assertThat(results.getDocs().get(0).getKey()).isEqualTo("UUID2");
  }

  @Test
  public void search_by_date() throws Exception {
    es.putDocuments("activities", "activity", newDoc(1, 1_500_000_000_000L), newDoc(2, 1_600_000_000_000L));

    ActivityQuery query = new ActivityQuery();
    query.setSince(new Date(1_550_000_000_000L));
    SearchResult<ActivityDoc> results = underTest.search(query, new SearchOptions());
    assertThat(results.getDocs()).hasSize(1);
    assertThat(results.getDocs().get(0).getKey()).isEqualTo("UUID2");

    query = new ActivityQuery();
    query.setTo(new Date(1_550_000_000_000L));
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getDocs()).hasSize(1);
    assertThat(results.getDocs().get(0).getKey()).isEqualTo("UUID1");
  }

  ActivityDoc newDoc(int id, long date) {
    ActivityDoc doc = new ActivityDoc();
    doc.setKey("UUID" + id);
    doc.setType(Activity.Type.ANALYSIS_REPORT.name());
    doc.setAction("THE_ACTION " + id);
    doc.setMessage("THE_MSG " + id);
    doc.setDetails(ImmutableMap.of("foo", "bar" + id));
    doc.setLogin("THE_GUY " + id);
    doc.setCreatedAt(new Date(date));
    return doc;
  }
}
