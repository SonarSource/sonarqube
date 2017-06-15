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
package org.sonar.server.qualityprofile.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;

import static java.util.Objects.requireNonNull;

@ServerSide
public class ChangelogLoader {

  private final DbClient dbClient;

  public ChangelogLoader(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * @return non-null list of changes, by descending order of date
   */
  public Changelog load(DbSession dbSession, QProfileChangeQuery query) {
    List<QProfileChangeDto> dtos = dbClient.qProfileChangeDao().selectByQuery(dbSession, query);
    List<Change> changes = dtos.stream()
      .map(Change::from)
      .collect(MoreCollectors.toList(dtos.size()));
    completeUserAndRuleNames(dbSession, changes);

    int total = dbClient.qProfileChangeDao().countForQProfileUuid(dbSession, query.getProfileUuid());
    return new Changelog(total, changes);
  }

  private void completeUserAndRuleNames(DbSession dbSession, List<Change> changes) {
    Set<String> logins = changes.stream().filter(c -> c.userLogin != null).map(c -> c.userLogin).collect(MoreCollectors.toSet());
    Map<String, String> userNamesByLogins = dbClient.userDao()
      .selectByLogins(dbSession, logins)
      .stream()
      .collect(java.util.stream.Collectors.toMap(UserDto::getLogin, UserDto::getName));

    Set<RuleKey> ruleKeys = changes.stream().filter(c -> c.ruleKey != null).map(c -> c.ruleKey).collect(MoreCollectors.toSet());
    Map<RuleKey, String> ruleNamesByKeys = dbClient.ruleDao()
      .selectDefinitionByKeys(dbSession, Lists.newArrayList(ruleKeys))
      .stream()
      .collect(java.util.stream.Collectors.toMap(RuleDefinitionDto::getKey, RuleDefinitionDto::getName));

    changes.forEach(c -> {
      c.userName = userNamesByLogins.get(c.userLogin);
      c.ruleName = ruleNamesByKeys.get(c.ruleKey);
    });
  }

  static class Change {
    private String key;
    private String type;
    private long at;
    private String severity;
    private String userLogin;
    private String userName;
    private String inheritance;
    private RuleKey ruleKey;
    private String ruleName;
    private final Map<String, String> params = new HashMap<>();

    private Change() {
    }

    @VisibleForTesting
    Change(String key, String type, long at, @Nullable String severity, @Nullable String userLogin,
      @Nullable String userName, @Nullable String inheritance, @Nullable RuleKey ruleKey, @Nullable String ruleName) {
      this.key = key;
      this.type = type;
      this.at = at;
      this.severity = severity;
      this.userLogin = userLogin;
      this.userName = userName;
      this.inheritance = inheritance;
      this.ruleKey = ruleKey;
      this.ruleName = ruleName;
    }

    public String getKey() {
      return key;
    }

    @CheckForNull
    public String getSeverity() {
      return severity;
    }

    @CheckForNull
    public String getUserLogin() {
      return userLogin;
    }

    @CheckForNull
    public String getUserName() {
      return userName;
    }

    public String getType() {
      return type;
    }

    @CheckForNull
    public String getInheritance() {
      return inheritance;
    }

    public RuleKey getRuleKey() {
      return ruleKey;
    }

    @CheckForNull
    public String getRuleName() {
      return ruleName;
    }

    public long getCreatedAt() {
      return at;
    }

    public Map<String, String> getParams() {
      return params;
    }

    private static Change from(QProfileChangeDto dto) {
      Map<String, String> data = dto.getDataAsMap();
      Change change = new Change();
      change.key = dto.getUuid();
      change.userLogin = dto.getLogin();
      change.type = dto.getChangeType();
      change.at = dto.getCreatedAt();
      // see content of data in class org.sonar.server.qualityprofile.ActiveRuleChange
      change.severity = data.get("severity");
      String ruleKey = data.get("ruleKey");
      if (ruleKey != null) {
        change.ruleKey = RuleKey.parse(ruleKey);
      }
      change.inheritance = data.get("inheritance");
      data.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("param_"))
        .forEach(entry -> change.params.put(entry.getKey().replace("param_", ""), entry.getValue()));
      return change;
    }
  }

  static class Changelog {
    private final int total;
    private final List<Change> changes;

    Changelog(int total, List<Change> changes) {
      this.total = total;
      this.changes = requireNonNull(changes);
    }

    public int getTotal() {
      return total;
    }

    public List<Change> getChanges() {
      return changes;
    }
  }
}
