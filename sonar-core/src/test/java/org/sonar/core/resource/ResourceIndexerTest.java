/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.resource;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;

import static org.mockito.Mockito.*;

public class ResourceIndexerTest {

  @Test
  public void shouldNotIndexDirectories() {
    ResourceIndexerDao dao = mock(ResourceIndexerDao.class);
    ResourceIndexer indexer = new ResourceIndexer(dao);
    indexer.index("org.foo", Qualifiers.DIRECTORY, 12, 9);

    verifyZeroInteractions(dao);
  }

  @Test
  public void shouldIndexResource() {
    ResourceIndexerDao dao = mock(ResourceIndexerDao.class);
    ResourceIndexer indexer = new ResourceIndexer(dao);
    indexer.index("org.foo.Bar", Qualifiers.FILE, 12, 9);

    verify(dao).index("org.foo.Bar", Qualifiers.FILE, 12, 9);
  }

  @Test
  public void shouldIndexAll() {
    ResourceIndexerDao dao = mock(ResourceIndexerDao.class);
    ResourceIndexer indexer = new ResourceIndexer(dao);
    indexer.indexAll();

    verify(dao).index(argThat(new BaseMatcher<ResourceIndexerFilter>() {
      public boolean matches(Object o) {
        ResourceIndexerFilter filter = (ResourceIndexerFilter) o;
        return filter.isEnabled() && filter.getScopes().length == 2 && filter.getQualifiers().length == ResourceIndexer.INDEXABLE_QUALIFIERS.length;
      }

      public void describeTo(Description description) {
      }
    }));
  }
}
