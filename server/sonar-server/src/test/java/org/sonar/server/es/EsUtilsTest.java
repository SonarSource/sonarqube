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
package org.sonar.server.es;

import com.google.common.base.Function;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.search.BaseDoc;
import org.sonar.test.TestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsUtilsTest {

  @Test
  public void convertToDocs_empty() {
    SearchHits hits = mock(SearchHits.class, Mockito.RETURNS_MOCKS);
    List<BaseDoc> docs = EsUtils.convertToDocs(hits, new Function<Map<String, Object>, BaseDoc>() {
      @Override
      public BaseDoc apply(Map<String, Object> input) {
        return new IssueDoc(input);
      }
    });
    assertThat(docs).isEmpty();
  }

  @Test
  public void convertToDocs() {
    SearchHits hits = mock(SearchHits.class, Mockito.RETURNS_MOCKS);
    when(hits.getHits()).thenReturn(new SearchHit[]{mock(SearchHit.class)});
    List<BaseDoc> docs = EsUtils.convertToDocs(hits, new Function<Map<String, Object>, BaseDoc>() {
      @Override
      public BaseDoc apply(Map<String, Object> input) {
        return new IssueDoc(input);
      }
    });
    assertThat(docs).hasSize(1);
  }

  @Test
  public void util_class() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(EsUtils.class)).isTrue();
  }

  @Test
  public void es_date_format() {
    assertThat(EsUtils.formatDateTime(new Date(1_500_000_000_000L))).startsWith("2017-07-");
    assertThat(EsUtils.formatDateTime(null)).isNull();

    assertThat(EsUtils.parseDateTime("2017-07-14T04:40:00.000+02:00").getTime()).isEqualTo(1_500_000_000_000L);
    assertThat(EsUtils.parseDateTime(null)).isNull();
  }
}
