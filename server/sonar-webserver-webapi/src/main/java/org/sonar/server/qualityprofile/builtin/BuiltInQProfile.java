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
package org.sonar.server.qualityprofile.builtin;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static java.util.Collections.unmodifiableList;
import static org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import static org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.OverriddenParam;

/**
 * Represent a Quality Profile as computed from {@link BuiltInQualityProfilesDefinition} provided by installed plugins.
 */
@Immutable
public final class BuiltInQProfile {
  private final QProfileName qProfileName;
  private final boolean isDefault;
  private final List<ActiveRule> activeRules;

  private BuiltInQProfile(Builder builder) {
    this.qProfileName = new QProfileName(builder.language, builder.name);
    this.isDefault = builder.declaredDefault || builder.computedDefault;
    this.activeRules = unmodifiableList(builder.activeRules);
  }

  public String getName() {
    return qProfileName.getName();
  }

  public String getLanguage() {
    return qProfileName.getLanguage();
  }

  public QProfileName getQProfileName() {
    return qProfileName;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public List<ActiveRule> getActiveRules() {
    return activeRules;
  }

  public static final class ActiveRule {
    private final String ruleUuid;
    private final RuleKey ruleKey;
    private final String severity;
    private final List<OverriddenParam> params;

    public ActiveRule(String ruleUuid, BuiltInActiveRule builtIn) {
      this(ruleUuid, RuleKey.of(builtIn.repoKey(), builtIn.ruleKey()), builtIn.overriddenSeverity(), builtIn.overriddenParams());
    }

    ActiveRule(String ruleUuid, RuleKey ruleKey, @Nullable String severity, List<OverriddenParam> params) {
      this.ruleUuid = ruleUuid;
      this.ruleKey = ruleKey;
      this.severity = severity;
      this.params = params;
    }

    public String getRuleUuid() {
      return ruleUuid;
    }

    public RuleKey getRuleKey() {
      return ruleKey;
    }

    @CheckForNull
    public String getSeverity() {
      return severity;
    }

    public List<OverriddenParam> getParams() {
      return params;
    }
  }

  static final class Builder {
    private String language;
    private String name;
    private boolean declaredDefault;
    private boolean computedDefault;
    private final List<ActiveRule> activeRules = new ArrayList<>();

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    Builder setName(String name) {
      this.name = name;
      return this;
    }

    String getName() {
      return name;
    }

    Builder setDeclaredDefault(boolean declaredDefault) {
      this.declaredDefault = declaredDefault;
      return this;
    }

    boolean isDeclaredDefault() {
      return declaredDefault;
    }

    Builder setComputedDefault(boolean flag) {
      computedDefault = flag;
      return this;
    }

    Builder addRule(ActiveRule activeRule) {
      this.activeRules.add(activeRule);
      return this;
    }

    BuiltInQProfile build() {
      return new BuiltInQProfile(this);
    }
  }
}
