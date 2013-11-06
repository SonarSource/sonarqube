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

package org.sonar.server.search;

import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchIndexTest {

  private SearchNode searchNode;

  private Client client;

  private SearchIndex searchIndex;

  @Before
  public void setUp() {
    searchNode = mock(SearchNode.class);
    client = mock(Client.class);
    when(searchNode.client()).thenReturn(client);

    searchIndex = new SearchIndex(searchNode);
  }

  @Test
  public void should_start_and_stop_properly() {
    searchIndex.start();

    verify(searchNode).client();

    searchIndex.stop();

    verify(client).close();
  }
}
