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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class IssueFilterServiceTest {

  private IssueFilterService service;

  private IssueFilterDao issueFilterDao;
  private IssueFinder issueFinder;

  private UserSession userSession;

  @Before
  public void before() {
    userSession = mock(UserSession.class);
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.userId()).thenReturn(10);
    when(userSession.login()).thenReturn("john");

    issueFilterDao = mock(IssueFilterDao.class);
    issueFinder = mock(IssueFinder.class);

    service = new IssueFilterService(issueFilterDao, issueFinder);
  }

  @Test
  public void should_find_by_id() {
    IssueFilterDto issueFilterDto = new IssueFilterDto().setId(1L).setName("My Issue").setUserLogin("john");
    when(issueFilterDao.selectById(1L)).thenReturn(issueFilterDto);

    DefaultIssueFilter issueFilter = service.findById(1L, userSession);
    assertThat(issueFilter).isNotNull();
    assertThat(issueFilter.id()).isEqualTo(1L);
  }

  @Test
  public void should_not_find_by_id_on_not_existing_issue() {
    when(issueFilterDao.selectById(1L)).thenReturn(null);
    try {
      service.findById(1L, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Filter not found: 1");
    }
  }

  @Test
  public void should_not_find_by_id_if_not_logged() {
    when(userSession.isLoggedIn()).thenReturn(false);
    try {
      service.findById(1L, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User is not logged in");
    }

    verifyZeroInteractions(issueFilterDao);
  }

  @Test
  public void should_not_find_if_user_is_not_the_owner_of_filter() {
    IssueFilterDto issueFilterDto = new IssueFilterDto().setId(1L).setName("My Issue").setUserLogin("eric");
    when(issueFilterDao.selectById(1L)).thenReturn(issueFilterDto);
    try {
      // John is not authorized to see eric filters
      service.findById(1L, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User is not authorized to get this filter");
    }
  }

  @Test
  public void should_find_by_user() {
    when(issueFilterDao.selectByUser("john")).thenReturn(newArrayList(new IssueFilterDto().setId(1L).setName("My Issue").setUserLogin("john")));

    List<DefaultIssueFilter> issueFilter = service.findByUser(userSession);
    assertThat(issueFilter).hasSize(1);
    assertThat(issueFilter.get(0).id()).isEqualTo(1L);
  }

  @Test
  public void should_find_by_user_return_empty_result_for_not_logged_user() {
    when(userSession.isLoggedIn()).thenReturn(false);

    assertThat(service.findByUser(userSession)).isEmpty();
  }

  @Test
  public void should_save() {
    DefaultIssueFilter issueFilter = new DefaultIssueFilter().setName("My Issue");
    DefaultIssueFilter result = service.save(issueFilter, userSession);
    assertThat(result.name()).isEqualTo("My Issue");
    assertThat(result.user()).isEqualTo("john");

    verify(issueFilterDao).insert(any(IssueFilterDto.class));
  }

  @Test
  public void should_not_save_if_not_logged() {
    when(userSession.isLoggedIn()).thenReturn(false);
    try {
      DefaultIssueFilter issueFilter = new DefaultIssueFilter().setName("My Issue");
      service.save(issueFilter, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("User is not logged in");
    }
    verify(issueFilterDao, never()).insert(any(IssueFilterDto.class));
  }

  @Test
  public void should_not_save_if_name_already_used() {
    when(issueFilterDao.selectByNameAndUser(eq("My Issue"), eq("john"), eq((Long) null))).thenReturn(new IssueFilterDto());
    try {
      DefaultIssueFilter issueFilter = new DefaultIssueFilter().setName("My Issue");
      service.save(issueFilter, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Name already exists");
    }
    verify(issueFilterDao, never()).insert(any(IssueFilterDto.class));
  }

  @Test
  public void should_update() {
    when(issueFilterDao.selectById(1L)).thenReturn(new IssueFilterDto().setId(1L).setName("My Old Filter").setUserLogin("john"));

    DefaultIssueFilter result = service.update(new DefaultIssueFilter().setId(1L).setName("My New Filter"), userSession);
    assertThat(result.name()).isEqualTo("My New Filter");

    verify(issueFilterDao).update(any(IssueFilterDto.class));
  }

  @Test
  public void should_not_update_if_filter_not_found() {
    when(issueFilterDao.selectById(1L)).thenReturn(null);

    try {
      service.update(new DefaultIssueFilter().setId(1L).setName("My New Filter"), userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Filter not found: 1");
    }
    verify(issueFilterDao, never()).update(any(IssueFilterDto.class));
  }

  @Test
  public void should_not_update_if_name_already_used() {
    when(issueFilterDao.selectById(1L)).thenReturn(new IssueFilterDto().setId(1L).setName("My Old Filter").setUserLogin("john"));
    when(issueFilterDao.selectByNameAndUser(eq("My Issue"), eq("john"), eq(1L))).thenReturn(new IssueFilterDto());

    try {
      service.update(new DefaultIssueFilter().setId(1L).setName("My Issue"), userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Name already exists");
    }
    verify(issueFilterDao, never()).update(any(IssueFilterDto.class));
  }

  @Test
  public void should_update_data() {
    when(issueFilterDao.selectById(1L)).thenReturn(new IssueFilterDto().setId(1L).setName("My Old Filter").setUserLogin("john"));

    Map<String, Object> data = newHashMap();
    data.put("componentRoots", "struts");

    DefaultIssueFilter result = service.updateData(1L, data, userSession);
    assertThat(result.data()).isEqualTo("componentRoots=struts");

    verify(issueFilterDao).update(any(IssueFilterDto.class));
  }

  @Test
  public void should_delete() {
    when(issueFilterDao.selectById(1L)).thenReturn(new IssueFilterDto().setId(1L).setName("My Issues").setUserLogin("john"));

    service.delete(1L, userSession);

    verify(issueFilterDao).delete(1L);
  }

  @Test
  public void should_not_delete_if_filter_not_found() {
    when(issueFilterDao.selectById(1L)).thenReturn(null);

    try {
      service.delete(1L, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Filter not found: 1");
    }
    verify(issueFilterDao, never()).delete(anyLong());
  }

  @Test
  public void should_execute_from_issue_query() {
    IssueQuery issueQuery = IssueQuery.builder().build();

    service.execute(issueQuery);

    verify(issueFinder).find(issueQuery);
  }

  @Test
  public void should_execute_from_filter_id() {
    when(issueFilterDao.selectById(1L)).thenReturn(new IssueFilterDto().setId(1L).setName("My Old Filter").setUserLogin("john").setData("componentRoots=struts"));

    ArgumentCaptor<IssueQuery> issueQueryCaptor = ArgumentCaptor.forClass(IssueQuery.class);

    service.execute(1L, userSession);
    verify(issueFinder).find(issueQueryCaptor.capture());

    IssueQuery issueQuery = issueQueryCaptor.getValue();
    assertThat(issueQuery.componentRoots()).contains("struts");
  }

}
