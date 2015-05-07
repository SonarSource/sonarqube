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

package org.sonar.server.activity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.QProfileActivity;
import org.sonar.server.qualityprofile.QProfileActivityQuery;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.search.Result;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RubyQProfileActivityServiceTest {

  @Mock
  QProfileService service;

  @Captor
  ArgumentCaptor<QProfileActivityQuery> activityArgumentCaptor;

  @Captor
  ArgumentCaptor<SearchOptions> queryOptionsArgumentCaptor;

  RubyQProfileActivityService rubyQProfileActivityService;

  @Before
  public void setUp() {
    rubyQProfileActivityService = new RubyQProfileActivityService(service);
  }

  @Test
  public void search() {
    Date since = DateUtils.parseDate("2014-05-19");
    Date to = DateUtils.parseDate("2014-06-19");

    Result<QProfileActivity> result = mock(Result.class);
    when(result.getHits()).thenReturn(Lists.<QProfileActivity>newArrayList());
    when(result.getTotal()).thenReturn(10L);
    when(service.searchActivities(any(QProfileActivityQuery.class), any(SearchOptions.class))).thenReturn(result);

    rubyQProfileActivityService.search(ImmutableMap.<String, Object>of("profileKey", "PROFILE_KEY", "since", since, "to", to));

    verify(service).searchActivities(activityArgumentCaptor.capture(), queryOptionsArgumentCaptor.capture());

    assertThat(queryOptionsArgumentCaptor.getValue().getLimit()).isEqualTo(50);

    assertThat(activityArgumentCaptor.getValue().getQprofileKey()).isEqualTo("PROFILE_KEY");
    assertThat(activityArgumentCaptor.getValue().getTypes()).containsOnly(Activity.Type.QPROFILE.name());
    assertThat(activityArgumentCaptor.getValue().getSince()).isEqualTo(since);
    assertThat(activityArgumentCaptor.getValue().getTo()).isEqualTo(to);
  }

  @Test
  public void search_with_empty_fields() {
    Result<QProfileActivity> result = mock(Result.class);
    when(result.getHits()).thenReturn(Lists.<QProfileActivity>newArrayList());
    when(result.getTotal()).thenReturn(10L);
    when(service.searchActivities(any(QProfileActivityQuery.class), any(SearchOptions.class))).thenReturn(result);

    rubyQProfileActivityService.search(ImmutableMap.<String, Object>of());

    verify(service).searchActivities(activityArgumentCaptor.capture(), queryOptionsArgumentCaptor.capture());

    assertThat(queryOptionsArgumentCaptor.getValue().getLimit()).isEqualTo(50);

    assertThat(activityArgumentCaptor.getValue().getQprofileKey()).isNull();
    assertThat(activityArgumentCaptor.getValue().getSince()).isNull();
    assertThat(activityArgumentCaptor.getValue().getTo()).isNull();
  }
}
