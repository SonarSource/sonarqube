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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleTagOperationsTest {

  @Mock
  private RuleTagDao ruleTagDao;

  @Mock
  private ESRuleTags esRuleTags;

  UserSession authorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  private RuleTagOperations operations;

  @Before
  public void setUp() {
    operations = new RuleTagOperations(ruleTagDao, esRuleTags);
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_on_unallowed_user() {
    operations.create("polop", unauthorizedUserSession);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_on_bad_tag() {
    operations.create("! ! !", authorizedUserSession);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_on_duplicate_tag() {
    String newTag = "polop";
    when(ruleTagDao.selectId(newTag)).thenReturn(42L);

    operations.create(newTag, authorizedUserSession);
  }

  @Test
  public void should_create_and_index_tag() {
    String newTag = "polop";
    when(ruleTagDao.selectId(newTag)).thenReturn(null);

    operations.create(newTag, authorizedUserSession);

    verify(ruleTagDao).selectId(newTag);
    ArgumentCaptor<RuleTagDto> newTagDto = ArgumentCaptor.forClass(RuleTagDto.class);
    verify(ruleTagDao).insert(newTagDto.capture());
    assertThat(newTagDto.getValue().getTag()).isEqualTo(newTag);
    verify(esRuleTags).put(newTagDto.getValue());
  }

  @Test
  public void should_delete_unused_tags() {
    long tagId = 42L;
    String tagValue = "answerlifeanevrythng";
    RuleTagDto unusedTag = new RuleTagDto().setId(tagId).setTag(tagValue);
    SqlSession session = mock(SqlSession.class);
    when(ruleTagDao.selectUnused(session)).thenReturn(ImmutableList.of(unusedTag));
    operations.deleteUnusedTags(session);

    verify(ruleTagDao).selectUnused(session);
    verify(ruleTagDao).delete(tagId, session);
    verify(esRuleTags).delete(tagValue);
  }

  @Test
  public void should_do_nothing_if_no_unused_tags() {
    SqlSession session = mock(SqlSession.class);
    operations.deleteUnusedTags(session);

    verify(ruleTagDao).selectUnused(session);
    verifyNoMoreInteractions(ruleTagDao, esRuleTags);
  }
}
