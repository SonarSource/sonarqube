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
package org.sonar.jpa.session;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JpaDatabaseSessionTest {

  private JpaDatabaseSession session;

  @Before
  public void setUp() {
    session = new JpaDatabaseSession(null);
  }

  @Test(expected = NonUniqueResultException.class)
  public void shouldThrowNonUniqueResultException() {
    Query query = mock(Query.class);
    when(query.getResultList()).thenReturn(Arrays.asList("foo", "bar"));
    session.getSingleResult(query, null);
  }

  @Test
  public void shouldReturnSingleResult() {
    Query query = mock(Query.class);

    when(query.getResultList()).thenReturn(Arrays.asList("foo", "foo"), Arrays.asList("bar"));
    assertThat(session.getSingleResult(query, "default"), is("foo"));
    assertThat(session.getSingleResult(query, "default"), is("bar"));
  }

  @Test
  public void shouldReturnDefaultValue() {
    Query query = mock(Query.class);
    when(query.getResultList()).thenReturn(Collections.emptyList());
    assertThat(session.getSingleResult(query, "default"), is("default"));
  }

  @Test
  public void shouldBuildCriteriasHQL() {
    StringBuilder hql = new StringBuilder();
    Map<String, Object> mappedCriterias = Maps.newLinkedHashMap();
    mappedCriterias.put("foo", "value");
    mappedCriterias.put("bar", null);
    session.buildCriteriasHQL(hql, mappedCriterias);
    assertThat(hql.toString(), is("o.foo=:foo AND o.bar IS NULL"));
  }

}
