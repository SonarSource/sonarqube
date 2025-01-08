/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.issue.IssueChangeDto.TYPE_COMMENT;
import static org.sonar.db.issue.IssueChangeDto.TYPE_FIELD_CHANGE;
import static org.sonar.server.issue.IssueFieldsSetter.FILE;
import static org.sonar.server.issue.IssueFieldsSetter.TECHNICAL_DEBT;

public class IssueChangeWSSupport {
  private static final String EFFORT_CHANGELOG_KEY = "effort";
  private final DbClient dbClient;
  private final AvatarResolver avatarFactory;
  private final UserSession userSession;

  public IssueChangeWSSupport(DbClient dbClient, AvatarResolver avatarFactory, UserSession userSession) {
    this.dbClient = dbClient;
    this.avatarFactory = avatarFactory;
    this.userSession = userSession;
  }

  public enum Load {
    CHANGE_LOG, COMMENTS, ALL
  }

  public interface FormattingContext {

    List<FieldDiffs> getChanges(IssueDto dto);

    List<IssueChangeDto> getComments(IssueDto dto);

    Set<UserDto> getUsers();

    Optional<UserDto> getUserByUuid(@Nullable String uuid);

    Optional<ComponentDto> getFileByUuid(@Nullable String uuid);

    boolean isUpdatableComment(IssueChangeDto comment);
  }

  public FormattingContext newFormattingContext(DbSession dbSession, Set<IssueDto> dtos, Load load) {
    return newFormattingContext(dbSession, dtos, load, Set.of(), Set.of());
  }

  public FormattingContext newFormattingContext(DbSession dbSession, Set<IssueDto> dtos, Load load, Set<UserDto> preloadedUsers, Set<ComponentDto> preloadedComponents) {
    Set<String> issueKeys = dtos.stream().map(IssueDto::getKey).collect(Collectors.toSet());

    List<IssueChangeDto> changes = List.of();
    List<IssueChangeDto> comments = List.of();
    switch (load) {
      case CHANGE_LOG:
        changes = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbSession, issueKeys, TYPE_FIELD_CHANGE);
        break;
      case COMMENTS:
        comments = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbSession, issueKeys, TYPE_COMMENT);
        break;
      case ALL:
        List<IssueChangeDto> all = dbClient.issueChangeDao().selectByIssueKeys(dbSession, issueKeys);
        changes = all.stream()
          .filter(t -> TYPE_FIELD_CHANGE.equals(t.getChangeType()))
          .toList();
        comments = all.stream()
          .filter(t -> TYPE_COMMENT.equals(t.getChangeType()))
          .toList();
        break;
      default:
        throw new IllegalStateException("Unsupported Load value:" + load);
    }

    Map<String, List<FieldDiffs>> changesByRuleKey = indexAndSort(changes, IssueChangeDto::toFieldDiffs, Comparator.comparing(FieldDiffs::creationDate));
    Map<String, List<IssueChangeDto>> commentsByIssueKey = indexAndSort(comments, t -> t, Comparator.comparing(IssueChangeDto::getIssueChangeCreationDate));
    Map<String, UserDto> usersByUuid = loadUsers(dbSession, changesByRuleKey, commentsByIssueKey, preloadedUsers);
    Map<String, ComponentDto> filesByUuid = loadFiles(dbSession, changesByRuleKey, preloadedComponents);
    Map<String, Boolean> updatableCommentByKey = loadUpdatableFlag(commentsByIssueKey);
    return new FormattingContextImpl(changesByRuleKey, commentsByIssueKey, usersByUuid, filesByUuid, updatableCommentByKey);
  }

  private static <T> Map<String, List<T>> indexAndSort(List<IssueChangeDto> changes, Function<IssueChangeDto, T> transform, Comparator<T> sortingComparator) {
    Multimap<String, IssueChangeDto> unordered = changes.stream()
      .collect(MoreCollectors.index(IssueChangeDto::getIssueKey, t -> t));
    return unordered.asMap().entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue().stream()
        .map(transform)
        .sorted(sortingComparator)
        .toList()));
  }

  private Map<String, UserDto> loadUsers(DbSession dbSession, Map<String, List<FieldDiffs>> changesByRuleKey,
    Map<String, List<IssueChangeDto>> commentsByIssueKey, Set<UserDto> preloadedUsers) {
    Set<String> userUuids = Stream.concat(
        changesByRuleKey.values().stream()
          .flatMap(Collection::stream)
          .map(FieldDiffs::userUuid)
          .filter(Optional::isPresent)
          .map(Optional::get),
        commentsByIssueKey.values().stream()
          .flatMap(Collection::stream)
          .map(IssueChangeDto::getUserUuid)
      )
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    if (userUuids.isEmpty()) {
      return emptyMap();
    }

    Set<String> preloadedUserUuids = preloadedUsers.stream().map(UserDto::getUuid).collect(Collectors.toSet());
    Set<String> missingUsersUuids = Sets.difference(userUuids, preloadedUserUuids).immutableCopy();
    if (missingUsersUuids.isEmpty()) {
      return preloadedUsers.stream()
        .filter(t -> userUuids.contains(t.getUuid()))
        .collect(Collectors.toMap(UserDto::getUuid, Function.identity()));
    }

    return Stream.concat(
        preloadedUsers.stream(),
        dbClient.userDao().selectByUuids(dbSession, missingUsersUuids).stream())
      .filter(t -> userUuids.contains(t.getUuid()))
      .collect(Collectors.toMap(UserDto::getUuid, Function.identity()));
  }

  private Map<String, ComponentDto> loadFiles(DbSession dbSession, Map<String, List<FieldDiffs>> changesByRuleKey, Set<ComponentDto> preloadedComponents) {
    Set<String> fileUuids = changesByRuleKey.values().stream()
      .flatMap(Collection::stream)
      .flatMap(diffs -> {
        FieldDiffs.Diff diff = diffs.get(FILE);
        if (diff == null) {
          return Stream.empty();
        }
        return Stream.of(toString(diff.newValue()), toString(diff.oldValue()));
      })
      .map(Strings::emptyToNull)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    if (fileUuids.isEmpty()) {
      return emptyMap();
    }

    Set<String> preloadedFileUuids = preloadedComponents.stream().map(ComponentDto::uuid).collect(Collectors.toSet());
    Set<String> missingFileUuids = Sets.difference(fileUuids, preloadedFileUuids).immutableCopy();
    if (missingFileUuids.isEmpty()) {
      return preloadedComponents.stream()
        .filter(t -> fileUuids.contains(t.uuid()))
        .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
    }

    return Stream.concat(
        preloadedComponents.stream(),
        dbClient.componentDao().selectByUuids(dbSession, missingFileUuids).stream())
      .filter(t -> fileUuids.contains(t.uuid()))
      .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
  }

  private Map<String, Boolean> loadUpdatableFlag(Map<String, List<IssueChangeDto>> commentsByIssueKey) {
    if (!userSession.isLoggedIn()) {
      return emptyMap();
    }
    String userUuid = userSession.getUuid();
    if (userUuid == null) {
      return emptyMap();
    }

    return commentsByIssueKey.values().stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(IssueChangeDto::getKey, t -> userUuid.equals(t.getUserUuid())));
  }

  public Stream<Common.Changelog> formatChangelog(IssueDto dto, FormattingContext formattingContext) {
    return formattingContext.getChanges(dto).stream()
      .map(toWsChangelog(formattingContext));
  }

  private Function<FieldDiffs, Common.Changelog> toWsChangelog(FormattingContext formattingContext) {
    return change -> {
      Common.Changelog.Builder changelogBuilder = Common.Changelog.newBuilder();
      changelogBuilder.setCreationDate(formatDateTime(change.creationDate()));
      change.userUuid().flatMap(formattingContext::getUserByUuid)
        .ifPresent(user -> {
          changelogBuilder.setUser(user.getLogin());
          changelogBuilder.setIsUserActive(user.isActive());
          ofNullable(user.getName()).ifPresent(changelogBuilder::setUserName);
          ofNullable(emptyToNull(user.getEmail())).ifPresent(email -> changelogBuilder.setAvatar(avatarFactory.create(user)));
        });
      change.externalUser().ifPresent(changelogBuilder::setExternalUser);
      change.webhookSource().ifPresent(changelogBuilder::setWebhookSource);
      change.diffs().entrySet().stream()
        .map(toWsDiff(formattingContext))
        .forEach(changelogBuilder::addDiffs);
      return changelogBuilder.build();
    };
  }

  private static Function<Map.Entry<String, FieldDiffs.Diff>, Common.Changelog.Diff> toWsDiff(FormattingContext formattingContext) {
    return diff -> {
      FieldDiffs.Diff value = diff.getValue();
      Common.Changelog.Diff.Builder diffBuilder = Common.Changelog.Diff.newBuilder();
      String key = diff.getKey();
      String oldValue = emptyToNull(toString(value.oldValue()));
      String newValue = emptyToNull(toString(value.newValue()));
      if (key.equals(FILE)) {
        diffBuilder.setKey(key);
        formattingContext.getFileByUuid(newValue).map(ComponentDto::longName).ifPresent(diffBuilder::setNewValue);
        formattingContext.getFileByUuid(oldValue).map(ComponentDto::longName).ifPresent(diffBuilder::setOldValue);
      } else {
        diffBuilder.setKey(key.equals(TECHNICAL_DEBT) ? EFFORT_CHANGELOG_KEY : key);
        ofNullable(newValue).ifPresent(diffBuilder::setNewValue);
        ofNullable(oldValue).ifPresent(diffBuilder::setOldValue);
      }
      return diffBuilder.build();
    };
  }

  public Stream<Common.Comment> formatComments(IssueDto dto, Common.Comment.Builder commentBuilder, FormattingContext formattingContext) {
    return formattingContext.getComments(dto).stream()
      .map(comment -> {
        commentBuilder
          .clear()
          .setKey(comment.getKey())
          .setUpdatable(formattingContext.isUpdatableComment(comment))
          .setCreatedAt(DateUtils.formatDateTime(new Date(comment.getIssueChangeCreationDate())));
        String markdown = comment.getChangeData();
        formattingContext.getUserByUuid(comment.getUserUuid()).ifPresent(user -> commentBuilder.setLogin(user.getLogin()));
        if (markdown != null) {
          commentBuilder
            .setHtmlText(Markdown.convertToHtml(markdown))
            .setMarkdown(markdown);
        }
        return commentBuilder.build();
      });
  }

  private static String toString(@Nullable Serializable serializable) {
    if (serializable != null) {
      return serializable.toString();
    }
    return null;
  }

  @Immutable
  public static final class FormattingContextImpl implements FormattingContext {
    private final Map<String, List<FieldDiffs>> changesByIssueKey;
    private final Map<String, List<IssueChangeDto>> commentsByIssueKey;
    private final Map<String, UserDto> usersByUuid;
    private final Map<String, ComponentDto> filesByUuid;
    private final Map<String, Boolean> updatableCommentByKey;

    private FormattingContextImpl(Map<String, List<FieldDiffs>> changesByIssueKey,
      Map<String, List<IssueChangeDto>> commentsByIssueKey,
      Map<String, UserDto> usersByUuid, Map<String, ComponentDto> filesByUuid,
      Map<String, Boolean> updatableCommentByKey) {
      this.changesByIssueKey = changesByIssueKey;
      this.commentsByIssueKey = commentsByIssueKey;
      this.usersByUuid = usersByUuid;
      this.filesByUuid = filesByUuid;
      this.updatableCommentByKey = updatableCommentByKey;
    }

    @Override
    public List<FieldDiffs> getChanges(IssueDto dto) {
      List<FieldDiffs> fieldDiffs = changesByIssueKey.get(dto.getKey());
      if (fieldDiffs == null) {
        return List.of();
      }
      return List.copyOf(fieldDiffs);
    }

    @Override
    public List<IssueChangeDto> getComments(IssueDto dto) {
      List<IssueChangeDto> comments = commentsByIssueKey.get(dto.getKey());
      if (comments == null) {
        return List.of();
      }
      return List.copyOf(comments);
    }

    @Override
    public Set<UserDto> getUsers() {
      return ImmutableSet.copyOf(usersByUuid.values());
    }

    @Override
    public Optional<UserDto> getUserByUuid(@Nullable String uuid) {
      if (uuid == null) {
        return empty();
      }
      return Optional.ofNullable(usersByUuid.get(uuid));
    }

    @Override
    public Optional<ComponentDto> getFileByUuid(@Nullable String uuid) {
      if (uuid == null) {
        return empty();
      }
      return Optional.ofNullable(filesByUuid.get(uuid));
    }

    @Override
    public boolean isUpdatableComment(IssueChangeDto comment) {
      Boolean flag = updatableCommentByKey.get(comment.getKey());
      return flag != null && flag;
    }
  }

}
