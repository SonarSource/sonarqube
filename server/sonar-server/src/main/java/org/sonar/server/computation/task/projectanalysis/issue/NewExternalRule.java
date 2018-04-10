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
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;

@Immutable
public class NewExternalRule implements Rule {
  private final RuleKey key;
  private final String name;
  private final String descriptionUrl;
  private final String severity;
  private final RuleType type;
  private final String pluginKey;

  private NewExternalRule(Builder builder) {
    this.key = checkNotNull(builder.key, "key");
    this.name = checkNotEmpty(builder.name, "name");
    this.descriptionUrl = builder.descriptionUrl;
    this.severity = checkNotEmpty(builder.severity, "severity");
    this.type = checkNotNull(builder.type, "type");
    this.pluginKey = builder.pluginKey;
  }

  private static String checkNotEmpty(String str, String name) {
    if (StringUtils.isEmpty(str)) {
      throw new IllegalStateException("'" + name + "' not expected to be empty for an external rule");
    }
    return str;
  }

  private static <T> T checkNotNull(T obj, String name) {
    if (obj == null) {
      throw new IllegalStateException("'" + name + "' not expected to be null for an external rule");
    }
    return obj;
  }

  @CheckForNull
  public String getDescriptionUrl() {
    return descriptionUrl;
  }

  public String getSeverity() {
    return severity;
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
    return type;
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
    private String name;
    private String descriptionUrl;
    private String severity;
    private RuleType type;
    private String pluginKey;

    public Builder setKey(RuleKey key) {
      this.key = key;
      return this;
    }

    public Builder setName(String name) {
      this.name = StringUtils.trimToNull(name);
      return this;
    }

    public Builder setDescriptionUrl(String descriptionUrl) {
      this.descriptionUrl = StringUtils.trimToNull(descriptionUrl);
      return this;
    }

    public Builder setSeverity(String severity) {
      this.severity = StringUtils.trimToNull(severity);
      return this;
    }

    public Builder setType(RuleType type) {
      this.type = type;
      return this;
    }

    public String name() {
      return name;
    }

    public String descriptionUrl() {
      return descriptionUrl;
    }

    public String severity() {
      return severity;
    }

    public RuleType type() {
      return type;
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
