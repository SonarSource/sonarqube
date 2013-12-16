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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileSearchTest {

  @Mock
  QualityProfileDao dao;

  QProfileSearch search;

  @Before
  public void setUp() throws Exception {
    search = new QProfileSearch(dao);
  }

  @Test
  public void search_profiles() throws Exception {
    when(dao.selectAll()).thenReturn(newArrayList(
      new QualityProfileDto().setId(1).setName("Sonar Way with Findbugs").setLanguage("java").setParent("Sonar Way").setVersion(1).setUsed(false)
    ));

    List<QProfile> result = search.searchProfiles();
    assertThat(result).hasSize(1);

    QProfile qProfile = result.get(0);
    assertThat(qProfile.id()).isEqualTo(1);
    assertThat(qProfile.name()).isEqualTo("Sonar Way with Findbugs");
    assertThat(qProfile.language()).isEqualTo("java");
    assertThat(qProfile.parent()).isEqualTo("Sonar Way");
    assertThat(qProfile.version()).isEqualTo(1);
    assertThat(qProfile.used()).isFalse();
  }
}
