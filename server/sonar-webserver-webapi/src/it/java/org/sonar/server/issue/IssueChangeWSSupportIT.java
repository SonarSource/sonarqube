/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.issue.IssueChangeWSSupport.FormattingContext;
import org.sonar.server.issue.IssueChangeWSSupport.Load;
import org.sonar.server.tester.UserSessionRule;
import org.sonarqube.ws.Common.Changelog;
import org.sonarqube.ws.Common.Comment;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;
import static org.sonar.db.issue.IssueChangeDto.TYPE_FIELD_CHANGE;

@RunWith(DataProviderRunner.class)
public class IssueChangeWSSupportIT {
  private static final UuidFactoryFast UUID_FACTORY = UuidFactoryFast.getInstance();
  private static final Random RANDOM = new Random();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final DbClient dbClient = dbTester.getDbClient();
  private final AvatarResolverImpl avatarResolver = new AvatarResolverImpl();
  private final IssueChangeWSSupport underTest = new IssueChangeWSSupport(dbClient, avatarResolver, userSessionRule);

  @Test
  public void newFormattingContext_with_Load_CHANGE_LOG_loads_only_changelog() {
    IssueDto issue = dbTester.issues().insertIssue();
    List<IssueChangeDto> comments = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newComment(issue).setKey("comment_" + i))
      .toList();
    List<IssueChangeDto> fieldChanges = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newFieldChange(issue)
        .setChangeData(new FieldDiffs()
          .setDiff("f_change_" + i, null, null)
          .toEncodedString()))
      .toList();
    insertInRandomOrder(comments, fieldChanges);

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.CHANGE_LOG);

    assertThat(formattingContext.getChanges(issue))
      .extracting(FieldDiffs::toEncodedString)
      .containsExactlyInAnyOrder(fieldChanges.stream().map(t -> t.toFieldDiffs().toEncodedString()).toArray(String[]::new));
    assertThat(formattingContext.getComments(issue)).isEmpty();
  }

  @Test
  public void newFormattingContext_sorts_changes() {
    IssueDto issue = dbTester.issues().insertIssue();
    insertFieldChange(issue, c -> c.setCreatedAt(3L));
    insertFieldChange(issue, c -> c.setIssueChangeCreationDate(1L));
    insertFieldChange(issue, c -> c.setCreatedAt(2L));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.CHANGE_LOG);

    assertThat(formattingContext.getChanges(issue))
      .extracting(FieldDiffs::creationDate)
      .containsExactly(new Date(1L), new Date(2L), new Date(3L));
  }

  private void insertFieldChange(IssueDto issue, Consumer<IssueChangeDto> consumer ) {
    IssueChangeDto change = new IssueChangeDto()
      .setUuid(UUID_FACTORY.create())
      .setProjectUuid(UUID_FACTORY.create())
      .setChangeType(TYPE_FIELD_CHANGE)
      .setIssueKey(issue.getKey());
    consumer.accept(change);
    dbTester.issues().insertChange(change);
    dbTester.getSession().commit();
  }

  @Test
  public void newFormattingContext_with_Load_COMMENTS_loads_only_comments() {
    IssueDto issue = dbTester.issues().insertIssue();
    List<IssueChangeDto> comments = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newComment(issue).setKey("comment_" + i))
      .toList();
    List<IssueChangeDto> fieldChanges = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newFieldChange(issue)
        .setChangeData(new FieldDiffs()
          .setDiff("f_change_" + i, null, null)
          .toEncodedString()))
      .toList();
    insertInRandomOrder(comments, fieldChanges);

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.COMMENTS);

    assertThat(formattingContext.getComments(issue))
      .extracting(IssueChangeDto::getKey)
      .containsExactlyInAnyOrder(comments.stream().map(IssueChangeDto::getKey).toArray(String[]::new));
    assertThat(formattingContext.getChanges(issue)).isEmpty();
  }

  @Test
  public void newFormattingContext_with_Load_ALL_loads_changelog_and_comments() {
    IssueDto issue = dbTester.issues().insertIssue();
    List<IssueChangeDto> comments = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newComment(issue).setKey("comment_" + i))
      .toList();
    List<IssueChangeDto> fieldChanges = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newFieldChange(issue)
        .setChangeData(new FieldDiffs()
          .setDiff("f_change_" + i, null, null)
          .toEncodedString()))
      .toList();
    insertInRandomOrder(comments, fieldChanges);

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.ALL);

    assertThat(formattingContext.getComments(issue))
      .extracting(IssueChangeDto::getKey)
      .containsExactlyInAnyOrder(comments.stream().map(IssueChangeDto::getKey).toArray(String[]::new));
    assertThat(formattingContext.getComments(issue))
      .extracting(IssueChangeDto::getKey)
      .containsExactlyInAnyOrder(comments.stream().map(IssueChangeDto::getKey).toArray(String[]::new));
  }

  @Test
  public void newFormattingContext_with_load_CHANGE_LOG_loads_users_of_field_changes() {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto fieldChangeUser1 = newFieldChange(issue)
      .setUserUuid(user1.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_1", null, null).toEncodedString());
    IssueChangeDto fieldChangeUser2a = newFieldChange(issue)
      .setUserUuid(user2.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_2a", null, null).toEncodedString());
    IssueChangeDto fieldChangeUser2b = newFieldChange(issue)
      .setUserUuid(user2.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_2b", null, null).toEncodedString());
    IssueChangeDto fieldChangeNonExistingUser = newFieldChange(issue)
      .setUserUuid(uuid)
      .setChangeData(new FieldDiffs().setDiff("f_change_user_unknown", null, null).toEncodedString());
    insertInRandomOrder(Arrays.asList(fieldChangeUser1, fieldChangeUser2a, fieldChangeUser2b, fieldChangeNonExistingUser));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.CHANGE_LOG);

    assertThat(formattingContext.getUsers())
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid());
    assertThat(formattingContext.getUserByUuid(user1.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(user2.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(user3.getUuid())).isEmpty();
    assertThat(formattingContext.getUserByUuid(uuid)).isEmpty();
  }

  @Test
  public void newFormattingContext_with_load_COMMENTS_loads_users_of_comments() {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto issueChangeUser1 = newComment(issue).setUserUuid(user1.getUuid());
    IssueChangeDto issueChangeUser2a = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeUser2b = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeNonExistingUser = newComment(issue).setUserUuid(uuid);
    insertInRandomOrder(Arrays.asList(issueChangeUser1, issueChangeUser2a, issueChangeUser2b, issueChangeNonExistingUser));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.COMMENTS);

    assertThat(formattingContext.getUsers())
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid());
    assertThat(formattingContext.getUserByUuid(user1.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(user2.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(user3.getUuid())).isEmpty();
    assertThat(formattingContext.getUserByUuid(uuid)).isEmpty();
  }

  @Test
  public void newFormattingContext_with_load_ALL_loads_users_of_fieldChanges_and_comments() {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    UserDto user4 = dbTester.users().insertUser();
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto issueChangeUser1 = newComment(issue).setUserUuid(user1.getUuid());
    IssueChangeDto issueChangeUser2a = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeUser2b = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeNonExistingUser = newComment(issue).setUserUuid(uuid);
    IssueChangeDto fieldChangeUser1 = newFieldChange(issue)
      .setUserUuid(user1.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_1", null, null).toEncodedString());
    IssueChangeDto fieldChangeUser4 = newFieldChange(issue)
      .setUserUuid(user4.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_4", null, null).toEncodedString());
    IssueChangeDto fieldChangeNonExistingUser = newFieldChange(issue)
      .setUserUuid(uuid)
      .setChangeData(new FieldDiffs().setDiff("f_change_user_unknown", null, null).toEncodedString());
    insertInRandomOrder(Arrays.asList(issueChangeUser1, issueChangeUser2a, issueChangeUser2b, issueChangeNonExistingUser),
      Arrays.asList(fieldChangeUser1, fieldChangeUser4, fieldChangeNonExistingUser));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.ALL);

    assertThat(formattingContext.getUsers())
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), user4.getUuid());
    assertThat(formattingContext.getUserByUuid(user1.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(user2.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(user3.getUuid())).isEmpty();
    assertThat(formattingContext.getUserByUuid(user4.getUuid())).isNotEmpty();
    assertThat(formattingContext.getUserByUuid(uuid)).isEmpty();
  }

  @Test
  public void newFormattingContext_with_load_CHANGE_LOG_loads_files_of_file_fieldChanges() {
    newFormattingContext_loads_files_of_file_fieldChanges(Load.CHANGE_LOG);
  }

  @Test
  public void newFormattingContext_with_load_ALL_loads_files_of_file_fieldChanges() {
    newFormattingContext_loads_files_of_file_fieldChanges(Load.ALL);
  }

  private void newFormattingContext_loads_files_of_file_fieldChanges(Load load) {
    IssueDto issue = dbTester.issues().insertIssue();
    ComponentDto file1 = insertFile();
    ComponentDto file2 = insertFile();
    ComponentDto file3 = insertFile();
    ComponentDto file4 = insertFile();
    ComponentDto file5 = insertFile();
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto fileChangeFile1 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", file1.uuid(), null).toEncodedString());
    IssueChangeDto fileChangeFile2 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", file2.uuid(), null).toEncodedString());
    IssueChangeDto fileChangeFile3 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", null, file3.uuid()).toEncodedString());
    IssueChangeDto fileChangeFile4 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", file4.uuid(), file4.uuid()).toEncodedString());
    IssueChangeDto fileChangeNotExistingFile = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", uuid, uuid).toEncodedString());
    insertInRandomOrder(Arrays.asList(fileChangeFile1, fileChangeFile2, fileChangeFile3, fileChangeFile4, fileChangeNotExistingFile));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load);

    assertThat(formattingContext.getFileByUuid(file1.uuid())).isNotEmpty();
    assertThat(formattingContext.getFileByUuid(file2.uuid())).isNotEmpty();
    assertThat(formattingContext.getFileByUuid(file3.uuid())).isNotEmpty();
    assertThat(formattingContext.getFileByUuid(file4.uuid())).isNotEmpty();
    assertThat(formattingContext.getFileByUuid(file5.uuid())).isEmpty();
    assertThat(formattingContext.getFileByUuid(uuid)).isEmpty();
  }

  @Test
  public void newFormattingContext_does_not_load_preloaded_users_from_DB() {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    UserDto user4 = dbTester.users().insertUser();
    IssueChangeDto issueChangeUser1 = newComment(issue).setUserUuid(user1.getUuid());
    IssueChangeDto issueChangeUser2 = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto fieldChangeUser1 = newFieldChange(issue)
      .setUserUuid(user1.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_1", null, null).toEncodedString());
    IssueChangeDto fieldChangeUser3 = newFieldChange(issue)
      .setUserUuid(user3.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_3", null, null).toEncodedString());
    IssueChangeDto fieldChangeUser4 = newFieldChange(issue)
      .setUserUuid(user4.getUuid())
      .setChangeData(new FieldDiffs().setDiff("f_change_user_4", null, null).toEncodedString());
    insertInRandomOrder(Arrays.asList(issueChangeUser1, issueChangeUser2),
      Arrays.asList(fieldChangeUser1, fieldChangeUser3, fieldChangeUser4));
    user1.setEmail("post_insert_changed" + user1.getUuid());
    user2.setEmail("post_insert_changed" + user2.getUuid());
    user3.setEmail("post_insert_changed" + user3.getUuid());
    user4.setEmail("post_insert_changed" + user4.getUuid());

    // no users are preloaded
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.ALL,
      emptySet(), emptySet());
    assertThat(formattingContext.getUsers())
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
    assertThat(formattingContext.getUserByUuid(user1.getUuid()).get().getEmail()).isNotEqualTo(user1.getEmail());
    assertThat(formattingContext.getUserByUuid(user2.getUuid()).get().getEmail()).isNotEqualTo(user2.getEmail());
    assertThat(formattingContext.getUserByUuid(user3.getUuid()).get().getEmail()).isNotEqualTo(user3.getEmail());
    assertThat(formattingContext.getUserByUuid(user4.getUuid()).get().getEmail()).isNotEqualTo(user4.getEmail());

    // some users are preloaded
    formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.ALL,
      ImmutableSet.of(user1, user4), emptySet());
    assertThat(formattingContext.getUsers())
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
    assertThat(formattingContext.getUserByUuid(user1.getUuid()).get().getEmail()).isEqualTo(user1.getEmail());
    assertThat(formattingContext.getUserByUuid(user2.getUuid()).get().getEmail()).isNotEqualTo(user2.getEmail());
    assertThat(formattingContext.getUserByUuid(user3.getUuid()).get().getEmail()).isNotEqualTo(user3.getEmail());
    assertThat(formattingContext.getUserByUuid(user4.getUuid()).get().getEmail()).isEqualTo(user4.getEmail());

    // all users are preloaded
    formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.ALL,
      ImmutableSet.of(user1, user2, user3, user4), emptySet());
    assertThat(formattingContext.getUsers())
      .extracting(UserDto::getUuid)
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), user3.getUuid(), user4.getUuid());
    assertThat(formattingContext.getUserByUuid(user1.getUuid()).get().getEmail()).isEqualTo(user1.getEmail());
    assertThat(formattingContext.getUserByUuid(user2.getUuid()).get().getEmail()).isEqualTo(user2.getEmail());
    assertThat(formattingContext.getUserByUuid(user3.getUuid()).get().getEmail()).isEqualTo(user3.getEmail());
    assertThat(formattingContext.getUserByUuid(user4.getUuid()).get().getEmail()).isEqualTo(user4.getEmail());
  }

  @Test
  @UseDataProvider("loadAllOrChangelog")
  public void newFormattingContext_does_not_load_preloaded_files_from_DB(Load load) {
    IssueDto issue = dbTester.issues().insertIssue();
    ComponentDto file1 = insertFile();
    ComponentDto file2 = insertFile();
    ComponentDto file3 = insertFile();
    ComponentDto file4 = insertFile();
    IssueChangeDto fileChangeFile1 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", file1.uuid(), null).toEncodedString());
    IssueChangeDto fileChangeFile2 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", file2.uuid(), null).toEncodedString());
    IssueChangeDto fileChangeFile3 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", null, file3.uuid()).toEncodedString());
    IssueChangeDto fileChangeFile4 = newFieldChange(issue)
      .setChangeData(new FieldDiffs().setDiff("file", file4.uuid(), file4.uuid()).toEncodedString());
    insertInRandomOrder(Arrays.asList(fileChangeFile1, fileChangeFile2, fileChangeFile3, fileChangeFile4));
    file1.setName("preloaded_name" + file1.uuid());
    file2.setName("preloaded_name" + file2.uuid());
    file3.setName("preloaded_name" + file3.uuid());
    file4.setName("preloaded_name" + file4.uuid());

    // no files are preloaded
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load,
      emptySet(), emptySet());

    assertThat(formattingContext.getFileByUuid(file1.uuid()).get().name()).isNotEqualTo(file1.name());
    assertThat(formattingContext.getFileByUuid(file2.uuid()).get().name()).isNotEqualTo(file2.name());
    assertThat(formattingContext.getFileByUuid(file3.uuid()).get().name()).isNotEqualTo(file3.name());
    assertThat(formattingContext.getFileByUuid(file4.uuid()).get().name()).isNotEqualTo(file4.name());

    // some files are preloaded
    formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load,
      emptySet(), ImmutableSet.of(file2, file3));

    assertThat(formattingContext.getFileByUuid(file1.uuid()).get().name()).isNotEqualTo(file1.name());
    assertThat(formattingContext.getFileByUuid(file2.uuid()).get().name()).isEqualTo(file2.name());
    assertThat(formattingContext.getFileByUuid(file3.uuid()).get().name()).isEqualTo(file3.name());
    assertThat(formattingContext.getFileByUuid(file4.uuid()).get().name()).isNotEqualTo(file4.name());

    // all files are preloaded
    formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load,
      emptySet(), ImmutableSet.of(file1, file2, file3, file4));

    assertThat(formattingContext.getFileByUuid(file1.uuid()).get().name()).isEqualTo(file1.name());
    assertThat(formattingContext.getFileByUuid(file2.uuid()).get().name()).isEqualTo(file2.name());
    assertThat(formattingContext.getFileByUuid(file3.uuid()).get().name()).isEqualTo(file3.name());
    assertThat(formattingContext.getFileByUuid(file4.uuid()).get().name()).isEqualTo(file4.name());
  }

  @Test
  @UseDataProvider("loadAllOrComments")
  public void newFormattingContext_comments_without_userUuid_or_with_unknown_userUuid_are_not_updatable(Load load) {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto issueChangeUser1 = newComment(issue);
    IssueChangeDto issueChangeUserUnknown = newComment(issue).setUserUuid(uuid);
    insertInRandomOrder(Arrays.asList(issueChangeUser1, issueChangeUserUnknown));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load);

    assertThat(formattingContext.isUpdatableComment(issueChangeUser1)).isFalse();
    assertThat(formattingContext.isUpdatableComment(issueChangeUserUnknown)).isFalse();
  }

  @Test
  @UseDataProvider("loadAllOrComments")
  public void newFormattingContext_comments_with_userUuid_are_not_updatable_if_no_user_is_logged_in(Load load) {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto issueChangeUser1 = newComment(issue).setUserUuid(user1.getUuid());
    IssueChangeDto issueChangeUser2 = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeUserUnknown = newComment(issue).setUserUuid(uuid);
    insertInRandomOrder(Arrays.asList(issueChangeUser1, issueChangeUser2, issueChangeUserUnknown));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load);

    assertThat(formattingContext.isUpdatableComment(issueChangeUser1)).isFalse();
    assertThat(formattingContext.isUpdatableComment(issueChangeUser2)).isFalse();
    assertThat(formattingContext.isUpdatableComment(issueChangeUserUnknown)).isFalse();
  }

  @Test
  @UseDataProvider("loadAllOrComments")
  public void newFormattingContext_only_comments_of_logged_in_user_are_updatable(Load load) {
    IssueDto issue = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    userSessionRule.logIn(user2);
    String uuid = secure().nextAlphabetic(30);
    IssueChangeDto issueChangeUser1a = newComment(issue).setUserUuid(user1.getUuid());
    IssueChangeDto issueChangeUser1b = newComment(issue).setUserUuid(user1.getUuid());
    IssueChangeDto issueChangeUser2a = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeUser2b = newComment(issue).setUserUuid(user2.getUuid());
    IssueChangeDto issueChangeUserUnknown = newComment(issue).setUserUuid(uuid);
    insertInRandomOrder(Arrays.asList(issueChangeUser1a, issueChangeUser1b, issueChangeUser2a, issueChangeUser2b, issueChangeUserUnknown));

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load);

    assertThat(formattingContext.isUpdatableComment(issueChangeUser1a)).isFalse();
    assertThat(formattingContext.isUpdatableComment(issueChangeUser1b)).isFalse();
    assertThat(formattingContext.isUpdatableComment(issueChangeUser2a)).isTrue();
    assertThat(formattingContext.isUpdatableComment(issueChangeUser2b)).isTrue();
    assertThat(formattingContext.isUpdatableComment(issueChangeUserUnknown)).isFalse();
  }

  @Test
  @UseDataProvider("loadAllOrChangelog")
  public void formatChangelog_returns_empty_if_context_has_no_changeLog_for_specified_IssueDto(Load load) {
    IssueDto issue1 = dbTester.issues().insertIssue();
    IssueDto issue2 = dbTester.issues().insertIssue();
    List<IssueChangeDto> comments = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newComment(issue1).setKey("comment_" + i))
      .toList();
    List<IssueChangeDto> fieldChanges = IntStream.range(0, 1 + RANDOM.nextInt(20))
      .mapToObj(i -> newFieldChange(issue1)
        .setChangeData(new FieldDiffs()
          .setDiff("f_change_" + i, null, null)
          .toEncodedString()))
      .toList();
    insertInRandomOrder(comments, fieldChanges);
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue1), Load.CHANGE_LOG);

    assertThat(underTest.formatChangelog(issue2, formattingContext)).isEmpty();
    assertThat(underTest.formatChangelog(issue1, formattingContext)).isNotEmpty();
  }

  @Test
  @UseDataProvider("loadAllOrChangelog")
  public void formatChangelog_returns_field_diff_details(Load load) {
    IssueDto issue1 = dbTester.issues().insertIssue();
    int createdAt = 2_333_999;
    IssueChangeDto issueChangeDto = dbTester.issues().insertChange(newFieldChange(issue1)
      .setIssueChangeCreationDate(createdAt)
      .setChangeData(new FieldDiffs()
        .setDiff("f_change_1", "a", "b")
        .setDiff("f_change_2", "c", null)
        .setDiff("f_change_3", null, null)
        .setDiff("f_change_4", null, "e")
        .toEncodedString()));
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue1), load);

    List<Changelog> wsChangelogList = underTest.formatChangelog(issue1, formattingContext).toList();
    assertThat(wsChangelogList).hasSize(1);
    Changelog wsChangelog = wsChangelogList.iterator().next();
    assertThat(wsChangelog.getCreationDate()).isEqualTo(formatDateTime(createdAt));
    assertThat(wsChangelog.getDiffsList()).hasSize(4);
    assertThat(wsChangelog.getDiffsList().get(0).getKey()).isEqualTo("f_change_1");
    assertThat(wsChangelog.getDiffsList().get(0).getOldValue()).isEqualTo("a");
    assertThat(wsChangelog.getDiffsList().get(0).getNewValue()).isEqualTo("b");
    assertThat(wsChangelog.getDiffsList().get(1).getKey()).isEqualTo("f_change_2");
    assertThat(wsChangelog.getDiffsList().get(1).getOldValue()).isEqualTo("c");
    assertThat(wsChangelog.getDiffsList().get(1).hasNewValue()).isFalse();
    assertThat(wsChangelog.getDiffsList().get(2).getKey()).isEqualTo("f_change_3");
    assertThat(wsChangelog.getDiffsList().get(2).hasOldValue()).isFalse();
    assertThat(wsChangelog.getDiffsList().get(2).hasNewValue()).isFalse();
    assertThat(wsChangelog.getDiffsList().get(3).getKey()).isEqualTo("f_change_4");
    assertThat(wsChangelog.getDiffsList().get(3).hasOldValue()).isFalse();
    assertThat(wsChangelog.getDiffsList().get(3).getNewValue()).isEqualTo("e");
  }

  @Test
  public void formatChangelog_handlesCorrectlyExternalUserAndWebhookSource() {
    IssueDto issue = dbTester.issues().insertIssue();

    IssueChangeDto issueChangeDto = newFieldChange(issue)
      .setChangeData(new FieldDiffs()
        .setDiff("f_change_" + 1, null, null)
        .setExternalUser("toto")
        .setWebhookSource("github")
        .toEncodedString());

    dbTester.issues().insertChange(issueChangeDto);

    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), Load.CHANGE_LOG);

    Changelog changeLog = underTest.formatChangelog(issue, formattingContext).collect(MoreCollectors.onlyElement());
    assertThat(changeLog.getExternalUser()).isEqualTo("toto");
    assertThat(changeLog.getWebhookSource()).isEqualTo("github");
  }

  @Test
  @UseDataProvider("loadAllOrChangelog")
  public void formatChangelog_returns_user_details_if_exists(Load load) {
    IssueDto issue1 = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser(t -> t.setActive(false));
    String uuid = secure().nextAlphabetic(22);
    dbTester.issues().insertChange(newFieldChange(issue1)
      .setUserUuid(user1.getUuid())
      .setChangeData(new FieldDiffs()
        .setDiff("f_change_1", "a", "b")
        .toEncodedString()));
    dbTester.issues().insertChange(newFieldChange(issue1)
      .setUserUuid(uuid)
      .setChangeData(new FieldDiffs()
        .setDiff("f_change_2", "a", "b")
        .toEncodedString()));
    dbTester.issues().insertChange(newFieldChange(issue1)
      .setUserUuid(user2.getUuid())
      .setChangeData(new FieldDiffs()
        .setDiff("f_change_3", "a", "b")
        .toEncodedString()));
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue1), load);

    List<Changelog> wsChangelogList = underTest.formatChangelog(issue1, formattingContext).toList();
    assertThat(wsChangelogList)
      .extracting(Changelog::hasUser, t -> t.getDiffsList().iterator().next().getKey())
      .containsExactlyInAnyOrder(
        tuple(true, "f_change_1"),
        tuple(false, "f_change_2"),
        tuple(true, "f_change_3"));
    assertThat(wsChangelogList.stream().filter(Changelog::hasUser))
      .extracting(Changelog::getUser, Changelog::getUserName, Changelog::getIsUserActive, Changelog::getAvatar)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName(), true, avatarResolver.create(user1)),
        tuple(user2.getLogin(), user2.getName(), false, avatarResolver.create(user2)));
  }

  @Test
  @UseDataProvider("loadAllOrChangelog")
  public void formatChangelog_returns_no_avatar_if_email_is_not_set(Load load) {
    IssueDto issue1 = dbTester.issues().insertIssue();
    UserDto user1 = dbTester.users().insertUser(t -> t.setEmail(null));
    dbTester.issues().insertChange(newFieldChange(issue1)
      .setUserUuid(user1.getUuid())
      .setChangeData(new FieldDiffs()
        .setDiff("f_change_1", "a", "b")
        .toEncodedString()));
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue1), load);

    List<Changelog> wsChangelogList = underTest.formatChangelog(issue1, formattingContext).toList();
    assertThat(wsChangelogList).hasSize(1);
    assertThat(wsChangelogList.iterator().next().hasAvatar()).isFalse();
  }

  @Test
  @UseDataProvider("loadAllOrComments")
  public void formatComments_returns_empty_if_context_has_no_comment_for_specified_IssueDto(Load load) {
    IssueDto issue1 = dbTester.issues().insertIssue();
    IssueDto issue2 = dbTester.issues().insertIssue();
    IntStream.range(0, 1 + RANDOM.nextInt(22))
      .forEach(t -> dbTester.issues().insertChange(newComment(issue1)));
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue1), load);

    assertThat(underTest.formatComments(issue2, Comment.newBuilder(), formattingContext)).isEmpty();
    assertThat(underTest.formatComments(issue1, Comment.newBuilder(), formattingContext)).isNotEmpty();
  }

  @Test
  @UseDataProvider("loadAllOrComments")
  public void formatComments_returns_comment_markdown_and_html_when_available(Load load) {
    IssueDto issue = dbTester.issues().insertIssue();
    IssueChangeDto withText = dbTester.issues().insertChange(newComment(issue).setChangeData("* foo"));
    IssueChangeDto noText = dbTester.issues().insertChange(newComment(issue).setChangeData(null));
    FormattingContext formattingContext = underTest.newFormattingContext(dbTester.getSession(), singleton(issue), load);

    List<Comment> comments = underTest.formatComments(issue, Comment.newBuilder(), formattingContext).toList();
    assertThat(comments)
      .extracting(Comment::getKey, Comment::hasMarkdown, Comment::hasHtmlText)
      .containsExactlyInAnyOrder(
        tuple(withText.getKey(), true, true),
        tuple(noText.getKey(), false, false));
    assertThat(comments.stream().filter(Comment::hasHtmlText))
      .extracting(Comment::getMarkdown, Comment::getHtmlText)
      .containsOnly(tuple(withText.getChangeData(), Markdown.convertToHtml(withText.getChangeData())));
  }

  @DataProvider
  public static Object[][] loadAllOrChangelog() {
    return new Object[][] {
      {Load.ALL},
      {Load.CHANGE_LOG}
    };
  }

  @DataProvider
  public static Object[][] loadAllOrComments() {
    return new Object[][] {
      {Load.ALL},
      {Load.COMMENTS}
    };
  }

  private ComponentDto insertFile() {
    return dbTester.components().insertComponent(ComponentTesting.newFileDto(ComponentTesting.newPublicProjectDto()));
  }

  private static IssueChangeDto newComment(IssueDto issue) {
    return IssueTesting.newIssueChangeDto(issue).setChangeType(TYPE_COMMENT);
  }

  private static IssueChangeDto newFieldChange(IssueDto issue) {
    return IssueTesting.newIssueChangeDto(issue).setChangeType(TYPE_FIELD_CHANGE);
  }

  @SafeVarargs
  private final void insertInRandomOrder(Collection<IssueChangeDto>... changesLists) {
    List<IssueChangeDto> all = new ArrayList<>();
    Arrays.stream(changesLists).forEach(all::addAll);
    Collections.shuffle(all);
    all.forEach(i -> dbTester.issues().insertChange(i));
  }
}
