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

package org.sonar.server.qualityprofile;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.NotFoundException;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileLookupTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  QualityProfileDao dao;

  QProfileLookup search;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);
    search = new QProfileLookup(myBatis, dao);
  }

  @Test
  public void search_profile_from_id() throws Exception {
    when(dao.selectById(1)).thenReturn(
      new QualityProfileDto().setId(1).setName("Sonar Way with Findbugs").setLanguage("java").setParent("Sonar Way").setVersion(1).setUsed(false)
    );

    QProfile qProfile = search.profile(1);
    assertThat(qProfile.id()).isEqualTo(1);
    assertThat(qProfile.name()).isEqualTo("Sonar Way with Findbugs");
    assertThat(qProfile.language()).isEqualTo("java");
    assertThat(qProfile.parent()).isEqualTo("Sonar Way");
    assertThat(qProfile.version()).isEqualTo(1);
    assertThat(qProfile.used()).isFalse();
  }

  @Test
  public void fail_to_search_profile_from_id_if_not_found() throws Exception {
    when(dao.selectById(1)).thenReturn(null);

    try {
      search.profile(1);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void search_profiles() throws Exception {
    when(dao.selectAll()).thenReturn(newArrayList(
      new QualityProfileDto().setId(1).setName("Sonar Way with Findbugs").setLanguage("java").setParent("Sonar Way").setVersion(1).setUsed(false)
    ));

    List<QProfile> result = search.allProfiles();
    assertThat(result).hasSize(1);

    QProfile qProfile = result.get(0);
    assertThat(qProfile.id()).isEqualTo(1);
    assertThat(qProfile.name()).isEqualTo("Sonar Way with Findbugs");
    assertThat(qProfile.language()).isEqualTo("java");
    assertThat(qProfile.parent()).isEqualTo("Sonar Way");
    assertThat(qProfile.version()).isEqualTo(1);
    assertThat(qProfile.used()).isFalse();
  }

  @Test
  public void search_profiles_by_language() throws Exception {
    search.profiles("java");
    verify(dao).selectByLanguage("java");
  }

  @Test
  public void search_children_profiles() throws Exception {
    search.children(new QProfile().setName("Sonar Way").setLanguage("java"));
    verify(dao).selectChildren("Sonar Way", "java");
  }

  @Test
  public void count_children_profiles() throws Exception {
    search.countChildren(new QProfile().setName("Sonar Way").setLanguage("java"));
    verify(dao).countChildren("Sonar Way", "java");
  }

  @Test
  public void default_profile() throws Exception {
    when(dao.selectDefaultProfile("java", "sonar.profile.java")).thenReturn(
      new QualityProfileDto().setId(1).setName("Sonar Way with Findbugs").setLanguage("java").setParent("Sonar Way").setVersion(1).setUsed(false)
    );

    assertThat(search.defaultProfile("java")).isNotNull();
  }

  @Test
  public void not_find_default_profile() throws Exception {
    when(dao.selectDefaultProfile("java", "sonar.profile.java")).thenReturn(null);

    assertThat(search.defaultProfile("java")).isNull();
  }

  @Test
  public void search_ancestors() throws Exception {
    when(dao.selectParent(eq(1), eq(session))).thenReturn(null);
    when(dao.selectParent(eq(2), eq(session))).thenReturn(new QualityProfileDto().setId(1).setName("Parent").setLanguage("java"));
    when(dao.selectParent(eq(3), eq(session))).thenReturn(new QualityProfileDto().setId(2).setName("Child").setLanguage("java").setParent("Parent"));

    List<QProfile> result = search.ancestors(new QProfile().setId(3).setName("Grandchild").setLanguage("java").setParent("Child"));
    assertThat(result).hasSize(2);
  }

  @Test
  public void fail_to_get_ancestors_if_parent_cannot_be_found() throws Exception {
    when(dao.selectParent(3)).thenReturn(null);

    try {
      search.ancestors(new QProfile().setId(3).setName("Grandchild").setLanguage("java").setParent("Child"));
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

}
