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

package org.sonar.server.search;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.server.tester.ServerTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchClientMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  SearchClient searchClient;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    searchClient = tester.get(SearchClient.class);
  }

  @Test
  public void prepare_multi_search_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareMultiSearch();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_update_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareUpdate();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }

    try {
      searchClient.prepareUpdate("index", "type", "id");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_delete_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareDelete();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void delete_by_query_is_not_supported() throws Exception {
    try {
      searchClient.prepareDeleteByQuery();
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Delete by query must not be used. See https://github.com/elastic/elasticsearch/issues/10067. See alternatives in BulkIndexer.");
    }
  }

  @Test
  public void prepare_percolate_is_not_yet_implemented() throws Exception {
    try {
      searchClient.preparePercolate();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_multi_percolate_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareMultiPercolate();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_suggest_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareSuggest("index");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_more_like_this_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareMoreLikeThis("index", "tpye", "id");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_term_vector_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareTermVector("index", "tpye", "id");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_multi_term_vectors_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareMultiTermVectors();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_explain_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareExplain("index", "tpye", "id");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void prepare_clear_scroll_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareClearScroll();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }
}
