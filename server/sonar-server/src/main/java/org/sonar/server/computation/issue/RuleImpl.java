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
package org.sonar.server.computation.issue;

import com.google.common.base.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
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
  private final Integer subCharacteristicId;
  private final Set<String> tags;
  private final DebtRemediationFunction remediationFunction;

  public RuleImpl(RuleDto dto) {
    this.id = dto.getId();
    this.key = dto.getKey();
    this.name = dto.getName();
    this.status = dto.getStatus();
    this.subCharacteristicId = effectiveCharacteristicId(dto);
    this.tags = union(dto.getSystemTags(), dto.getTags());
    this.remediationFunction = effectiveRemediationFunction(dto);
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
  public Integer getSubCharacteristicId() {
    return subCharacteristicId;
  }

  @Override
  public DebtRemediationFunction getRemediationFunction() {
    return remediationFunction;
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
    return Objects.toStringHelper(this)
      .add("id", id)
      .add("key", key)
      .add("name", name)
      .add("status", status)
      .add("subCharacteristicId", subCharacteristicId)
      .add("tags", tags)
      .toString();
  }

  @CheckForNull
  private static Integer effectiveCharacteristicId(RuleDto dto) {
    if (isEnabledCharacteristicId(dto.getSubCharacteristicId())) {
      return dto.getSubCharacteristicId();
    }
    if (isEnabledCharacteristicId(dto.getDefaultSubCharacteristicId())) {
      return dto.getDefaultSubCharacteristicId();
    }
    return null;
  }

  private static boolean isEnabledCharacteristicId(@Nullable Integer id) {
    return (id != null) && (id.intValue() != RuleDto.DISABLED_CHARACTERISTIC_ID);
  }

  @CheckForNull
  private static DebtRemediationFunction effectiveRemediationFunction(RuleDto dto) {
    String fn = dto.getRemediationFunction();
    if (fn != null) {
      return new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(fn), dto.getRemediationCoefficient(), dto.getRemediationOffset());
    }
    String defaultFn = dto.getDefaultRemediationFunction();
    if (defaultFn != null) {
      return new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(defaultFn), dto.getDefaultRemediationCoefficient(), dto.getDefaultRemediationOffset());
    }
    return null;
  }
}
