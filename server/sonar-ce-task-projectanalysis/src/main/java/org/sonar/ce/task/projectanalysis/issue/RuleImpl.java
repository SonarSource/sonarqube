/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.MoreObjects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.db.rule.RuleDto;

import static com.google.common.collect.Sets.union;

@Immutable
public class RuleImpl implements Rule {

  private final int id;
  private final RuleKey key;
  private final String name;
  private final RuleStatus status;
  private final Set<String> tags;
  private final DebtRemediationFunction remediationFunction;
  private final RuleType type;
  private final String pluginKey;
  private final boolean isExternal;
  private final boolean isAdHoc;

  public RuleImpl(RuleDto dto) {
    this.id = dto.getId();
    this.key = dto.getKey();
    this.name = dto.getName();
    this.status = dto.getStatus();
    this.tags = union(dto.getSystemTags(), dto.getTags());
    this.remediationFunction = effectiveRemediationFunction(dto);
    this.type = RuleType.valueOfNullable(dto.getType());
    this.pluginKey = dto.getPluginKey();
    this.isExternal = dto.isExternal();
    this.isAdHoc = dto.isAdHoc();
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public RuleKey getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public RuleStatus getStatus() {
    return status;
  }

  @Override
  public Set<String> getTags() {
    return tags;
  }

  @Override
  public DebtRemediationFunction getRemediationFunction() {
    return remediationFunction;
  }

  @Override
  public RuleType getType() {
    return type;
  }

  @CheckForNull
  @Override
  public String getPluginKey() {
    return pluginKey;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleImpl rule = (RuleImpl) o;
    return key.equals(rule.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("id", id)
      .add("key", key)
      .add("name", name)
      .add("status", status)
      .add("tags", tags)
      .add("pluginKey", pluginKey)
      .toString();
  }

  @CheckForNull
  private static DebtRemediationFunction effectiveRemediationFunction(RuleDto dto) {
    String fn = dto.getRemediationFunction();
    if (fn != null) {
      return new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(fn), dto.getRemediationGapMultiplier(), dto.getRemediationBaseEffort());
    }
    String defaultFn = dto.getDefRemediationFunction();
    if (defaultFn != null) {
      return new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(defaultFn), dto.getDefRemediationGapMultiplier(), dto.getDefRemediationBaseEffort());
    }
    return null;
  }

  @Override
  public boolean isAdHoc() {
    return isAdHoc;
  }

  @Override
  public boolean isExternal() {
    return isExternal;
  }
}
