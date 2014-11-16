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

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class BulkIndexRequestIteratorTest {

  @Test
  public void iterate_over_requests() throws Exception {
    List<String> input = Arrays.asList("foo", "bar", "3requests", "baz");
    BulkIndexRequestIterator.InputConverter converter = new BulkIndexRequestIterator.InputConverter<String>() {
      @Override
      public List<ActionRequest> convert(String input) {
        if ("3requests".equals(input)) {
          return Collections.nCopies(3, (ActionRequest) new IndexRequest(input));
        }
        return Arrays.asList((ActionRequest) new IndexRequest(input));
      }
    };

    BulkIndexRequestIterator<String> it = new BulkIndexRequestIterator<String>(input, converter);

    assertThat(it.hasNext()).isTrue();
    assertIndex(it.next(), "foo");

    assertThat(it.hasNext()).isTrue();
    assertIndex(it.next(), "bar");

    assertThat(it.hasNext()).isTrue();
    assertIndex(it.next(), "3requests");
    assertThat(it.hasNext()).isTrue();
    assertIndex(it.next(), "3requests");
    assertThat(it.hasNext()).isTrue();
    assertIndex(it.next(), "3requests");

    assertThat(it.hasNext()).isTrue();
    assertIndex(it.next(), "baz");

    assertThat(it.hasNext()).isFalse();
    try {
      it.next();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
  }

  @Test
  public void empty() throws Exception {
    List<String> input = Collections.emptyList();
    BulkIndexRequestIterator.InputConverter converter = mock(BulkIndexRequestIterator.InputConverter.class);

    BulkIndexRequestIterator<String> it = new BulkIndexRequestIterator<String>(input, converter);

    assertThat(it.hasNext()).isFalse();
    verifyZeroInteractions(converter);
  }

  @Test
  public void removal_is_not_supported() throws Exception {
    List<String> input = Arrays.asList("foo");
    BulkIndexRequestIterator.InputConverter converter = mock(BulkIndexRequestIterator.InputConverter.class);

    BulkIndexRequestIterator<String> it = new BulkIndexRequestIterator<String>(input, converter);

    try {
      it.remove();
      fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  private void assertIndex(ActionRequest req, String indexName) {
    assertThat(req).isNotNull();
    assertThat(req).isInstanceOf(IndexRequest.class);
    assertThat(((IndexRequest) req).index()).isEqualTo(indexName);
  }
}
