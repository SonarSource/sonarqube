/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Common;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.issue.IssueFieldsSetter.FILE;
import static org.sonar.server.issue.IssueFieldsSetter.TECHNICAL_DEBT;

public class IssueChangelog {
  private static final String EFFORT_CHANGELOG_KEY = "effort";

  private final DbClient dbClient;
  private final AvatarResolver avatarFactory;

  public IssueChangelog(DbClient dbClient, AvatarResolver avatarFactory) {
    this.dbClient = dbClient;
    this.avatarFactory = avatarFactory;
  }

  public ChangelogLoadingContext newChangelogLoadingContext(DbSession dbSession, IssueDto dto) {
    return newChangelogLoadingContext(dbSession, dto, ImmutableSet.of(), ImmutableSet.of());
  }

  public ChangelogLoadingContext newChangelogLoadingContext(DbSession dbSession, IssueDto dto, Set<UserDto> preloadedUsers, Set<ComponentDto> preloadedComponents) {
    List<FieldDiffs> changes = dbClient.issueChangeDao().selectChangelogByIssue(dbSession, dto.getKey());
    return new ChangelogLoadingContextImpl(changes, preloadedUsers, preloadedComponents);
  }

  public Stream<Common.Changelog> formatChangelog(DbSession dbSession, ChangelogLoadingContext loadingContext) {
    Map<String, UserDto> usersByUuid = loadUsers(dbSession, loadingContext);
    Map<String, ComponentDto> filesByUuid = loadFiles(dbSession, loadingContext);
    FormatableChangeLog changeLogResults = new FormatableChangeLog(loadingContext.getChanges(), usersByUuid, filesByUuid);

    return changeLogResults.changes.stream()
      .map(toWsChangelog(changeLogResults));
  }

  private Map<String, UserDto> loadUsers(DbSession dbSession, ChangelogLoadingContext loadingContext) {
    List<FieldDiffs> changes = loadingContext.getChanges();
    if (changes.isEmpty()) {
      return emptyMap();
    }

    Set<UserDto> usersByUuid = loadingContext.getPreloadedUsers();

    Set<String> userUuids = changes.stream()
      .filter(change -> change.userUuid() != null)
      .map(FieldDiffs::userUuid)
      .collect(toSet());
    if (userUuids.isEmpty()) {
      return emptyMap();
    }

    Set<String> missingUsersUuids = Sets.difference(userUuids, usersByUuid).immutableCopy();
    if (missingUsersUuids.isEmpty()) {
      return usersByUuid.stream()
        .filter(t -> userUuids.contains(t.getUuid()))
        .collect(uniqueIndex(UserDto::getUuid, userUuids.size()));
    }

    return Stream.concat(
      usersByUuid.stream(),
      dbClient.userDao().selectByUuids(dbSession, missingUsersUuids).stream())
      .filter(t -> userUuids.contains(t.getUuid()))
      .collect(uniqueIndex(UserDto::getUuid, userUuids.size()));
  }

  private Map<String, ComponentDto> loadFiles(DbSession dbSession, ChangelogLoadingContext loadingContext) {
    List<FieldDiffs> changes = loadingContext.getChanges();
    if (changes.isEmpty()) {
      return emptyMap();
    }

    Set<String> fileUuids = changes.stream()
      .filter(diffs -> diffs.diffs().containsKey(FILE))
      .flatMap(diffs -> Stream.of(diffs.get(FILE).newValue().toString(), diffs.get(FILE).oldValue().toString()))
      .collect(toSet());
    if (fileUuids.isEmpty()) {
      return emptyMap();
    }

    Set<ComponentDto> preloadedComponents = loadingContext.getPreloadedComponents();
    Set<String> preloadedComponentUuids = preloadedComponents.stream()
      .map(ComponentDto::uuid)
      .collect(toSet(preloadedComponents.size()));
    Set<String> missingFileUuids = Sets.difference(fileUuids, preloadedComponentUuids).immutableCopy();
    if (missingFileUuids.isEmpty()) {
      return preloadedComponents.stream()
        .filter(t -> fileUuids.contains(t.uuid()))
        .collect(uniqueIndex(ComponentDto::uuid, fileUuids.size()));
    }

    return Stream.concat(
      preloadedComponents.stream(),
      dbClient.componentDao().selectByUuids(dbSession, missingFileUuids).stream())
      .filter(t -> fileUuids.contains(t.uuid()))
      .collect(uniqueIndex(ComponentDto::uuid, fileUuids.size()));
  }

  public interface ChangelogLoadingContext {
    List<FieldDiffs> getChanges();

    Set<UserDto> getPreloadedUsers();

    Set<ComponentDto> getPreloadedComponents();
  }

  @Immutable
  public static final class ChangelogLoadingContextImpl implements ChangelogLoadingContext {
    private final List<FieldDiffs> changes;
    private final Set<UserDto> preloadedUsers;
    private final Set<ComponentDto> preloadedComponents;

    private ChangelogLoadingContextImpl(List<FieldDiffs> changes, Set<UserDto> preloadedUsers, Set<ComponentDto> preloadedComponents) {
      this.changes = ImmutableList.copyOf(changes);
      this.preloadedUsers = ImmutableSet.copyOf(preloadedUsers);
      this.preloadedComponents = ImmutableSet.copyOf(preloadedComponents);
    }

    @Override
    public List<FieldDiffs> getChanges() {
      return changes;
    }

    @Override
    public Set<UserDto> getPreloadedUsers() {
      return preloadedUsers;
    }

    @Override
    public Set<ComponentDto> getPreloadedComponents() {
      return preloadedComponents;
    }
  }

  private Function<FieldDiffs, Common.Changelog> toWsChangelog(FormatableChangeLog results) {
    return change -> {
      String userUUuid = change.userUuid();
      Common.Changelog.Builder changelogBuilder = Common.Changelog.newBuilder();
      changelogBuilder.setCreationDate(formatDateTime(change.creationDate()));
      UserDto user = userUUuid == null ? null : results.users.get(userUUuid);
      if (user != null) {
        changelogBuilder.setUser(user.getLogin());
        changelogBuilder.setIsUserActive(user.isActive());
        ofNullable(user.getName()).ifPresent(changelogBuilder::setUserName);
        ofNullable(emptyToNull(user.getEmail())).ifPresent(email -> changelogBuilder.setAvatar(avatarFactory.create(user)));
      }
      change.diffs().entrySet().stream()
        .map(toWsDiff(results))
        .forEach(changelogBuilder::addDiffs);
      return changelogBuilder.build();
    };
  }

  private static Function<Map.Entry<String, FieldDiffs.Diff>, Common.Changelog.Diff> toWsDiff(FormatableChangeLog results) {
    return diff -> {
      FieldDiffs.Diff value = diff.getValue();
      Common.Changelog.Diff.Builder diffBuilder = Common.Changelog.Diff.newBuilder();
      String key = diff.getKey();
      String oldValue = value.oldValue() != null ? value.oldValue().toString() : null;
      String newValue = value.newValue() != null ? value.newValue().toString() : null;
      if (key.equals(FILE)) {
        diffBuilder.setKey(key);
        ofNullable(results.getFileLongName(emptyToNull(newValue))).ifPresent(diffBuilder::setNewValue);
        ofNullable(results.getFileLongName(emptyToNull(oldValue))).ifPresent(diffBuilder::setOldValue);
      } else {
        diffBuilder.setKey(key.equals(TECHNICAL_DEBT) ? EFFORT_CHANGELOG_KEY : key);
        ofNullable(emptyToNull(newValue)).ifPresent(diffBuilder::setNewValue);
        ofNullable(emptyToNull(oldValue)).ifPresent(diffBuilder::setOldValue);
      }
      return diffBuilder.build();
    };
  }

  private static final class FormatableChangeLog {
    private final List<FieldDiffs> changes;
    private final Map<String, UserDto> users;
    private final Map<String, ComponentDto> files;

    private FormatableChangeLog(List<FieldDiffs> changes, Map<String, UserDto> users, Map<String, ComponentDto> files) {
      this.changes = changes;
      this.users = users;
      this.files = files;
    }

    @CheckForNull
    String getFileLongName(@Nullable String fileUuid) {
      if (fileUuid == null) {
        return null;
      }
      ComponentDto file = files.get(fileUuid);
      return file == null ? null : file.longName();
    }

  }
}
