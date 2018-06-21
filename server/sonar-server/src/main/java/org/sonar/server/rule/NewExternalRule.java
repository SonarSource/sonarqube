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
package org.sonar.server.rule;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;

@Immutable
public class NewExternalRule {
  private final RuleKey key;
  private final String name;
  private final String pluginKey;

  private NewExternalRule(Builder builder) {
    Objects.requireNonNull(builder.key, "'key' not expected to be null for an external rule");
    this.key = builder.key;
    this.pluginKey = builder.pluginKey;
    this.name = builder.name;
  }

  public RuleKey getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

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
