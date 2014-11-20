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

import com.google.common.collect.Iterators;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.issue.index.IssueDoc;

import static org.fest.assertions.Assertions.assertThat;

public class IssueIndexerTest {

  @Rule
  public EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  @Test
  public void index_nothing() throws Exception {
    IssueIndexer indexer = new IssueIndexer(null, esTester.client());
    indexer.index(indexer.createBulkIndexer(false), Iterators.<IssueDoc>emptyIterator());
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX_ISSUES, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0L);
  }

}
