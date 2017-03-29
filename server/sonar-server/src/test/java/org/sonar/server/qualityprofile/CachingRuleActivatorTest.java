/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.qualityprofile;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CachingRuleActivatorTest {
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private QualityProfileDao qualityProfileDao = mock(QualityProfileDao.class);
  private CachingRuleActivator underTest = new CachingRuleActivator(null, dbClient, null, null, null, null, null);

  @Before
  public void wire_mocks() throws Exception {
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbClient.qualityProfileDao()).thenReturn(qualityProfileDao);
  }

  @Test
  public void getChildren_caches_that_qp_has_no_children() {
    mockSelectChildrenForKey("no_children");

    assertThat(underTest.getChildren(dbSession, "no_children"))
      .isEmpty();
    assertThat(underTest.getChildren(dbSession, "no_children"))
      .isEmpty();
    assertThat(underTest.getChildren(dbSession, "no_children"))
      .isEmpty();
    verify(qualityProfileDao, times(1)).selectChildren(eq(dbSession), anyString());
  }

  @Test
  public void getChildren_caches_that_sq_has_one_or_more_children() {
    mockSelectChildrenForKey("0", "1");
    mockSelectChildrenForKey("1", "2", "3");

    assertThat(underTest.getChildren(dbSession, "0"))
      .extracting(QualityProfileDto::getKey)
      .containsExactly("1");
    assertThat(underTest.getChildren(dbSession, "0"))
      .extracting(QualityProfileDto::getKey)
      .containsExactly("1");
    assertThat(underTest.getChildren(dbSession, "0"))
      .extracting(QualityProfileDto::getKey)
      .containsExactly("1");
    assertThat(underTest.getChildren(dbSession, "1"))
        .extracting(QualityProfileDto::getKey)
        .containsExactly("2", "3");
    assertThat(underTest.getChildren(dbSession, "1"))
        .extracting(QualityProfileDto::getKey)
        .containsExactly("2", "3");
    assertThat(underTest.getChildren(dbSession, "1"))
        .extracting(QualityProfileDto::getKey)
        .containsExactly("2", "3");
    verify(qualityProfileDao, times(1)).selectChildren(dbSession, "0");
    verify(qualityProfileDao, times(1)).selectChildren(dbSession, "1");
    verifyNoMoreInteractions(qualityProfileDao);
  }

  private void mockSelectChildrenForKey(String key, String... children) {
    when(qualityProfileDao.selectChildren(dbSession, key))
      .thenReturn(Arrays.stream(children).map(this::dto).collect(Collectors.toList()))
      .thenThrow(new IllegalStateException("selectChildren should be called only once for key " + key));
  }

  private QualityProfileDto dto(String key) {
    return new QualityProfileDto() {
      @Override
      public String toString() {
        return getKey();
      }
    }.setKey(key);
  }
}
