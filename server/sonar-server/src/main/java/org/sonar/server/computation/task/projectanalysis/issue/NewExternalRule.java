/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;

@Immutable
public class NewExternalRule implements Rule {
  private final RuleKey key;
  private final String name;
  private final String pluginKey;

  private NewExternalRule(Builder builder) {
    Objects.requireNonNull(builder.key, "'key' not expected to be null for an external rule");
    this.key = builder.key;
    this.pluginKey = builder.pluginKey;
    this.name = builder.name;
  }

  @Override
  public int getId() {
    return 0;
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
    return RuleStatus.defaultStatus();
  }

  @Override
  public RuleType getType() {
    return null;
  }

  @Override
  public boolean isExternal() {
    return true;
  }

  @Override
  public Set<String> getTags() {
    return Collections.emptySet();
  }

  @Override
  public DebtRemediationFunction getRemediationFunction() {
    return null;
  }

  @Override
  public String getPluginKey() {
    return pluginKey;
  }

  public static class Builder {
    private RuleKey key;
    private String pluginKey;
    private String name;

    public Builder setKey(RuleKey key) {
      this.key = key;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public String name() {
      return name;
    }

    public NewExternalRule build() {
      return new NewExternalRule(this);
    }

    public Builder setPluginKey(String pluginKey) {
      this.pluginKey = pluginKey;
      return this;
    }
  }
}
