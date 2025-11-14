/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class ActiveRuleRestReponse {

  public record ActiveRule(
    RuleKey ruleKey,
    String name,
    String severity,
    String createdAt,
    String updatedAt,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable String internalKey,
    String language,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable String templateRuleKey,
    String qProfileKey,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<RuleKey> deprecatedKeys,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Param> params,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<SoftwareQuality, Severity> impacts) {
  }

  public record RuleKey(String repository, String rule) {
  }

  public record Param(String key, String value) {
  }

  public static class Builder {
    private RuleKey ruleKey;
    private String name;
    private String severity;
    private String createdAt;
    private String updatedAt;
    private String internalKey;
    private String language;
    private String templateRuleKey;
    private String qProfilKey;
    private final List<RuleKey> deprecatedKeys = new ArrayList<>();
    private final List<Param> params = new ArrayList<>();
    private final Map<SoftwareQuality, Severity> impacts = new EnumMap<>(SoftwareQuality.class);

    public void setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setSeverity(String severity) {
      this.severity = severity;
    }

    public void setCreatedAt(String createdAt) {
      this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
      this.updatedAt = updatedAt;
    }

    public void setInternalKey(String internalKey) {
      this.internalKey = internalKey;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public void setTemplateRuleKey(String templateRuleKey) {
      this.templateRuleKey = templateRuleKey;
    }

    public void setQProfilKey(String qProfilKey) {
      this.qProfilKey = qProfilKey;
    }

    public void setParams(List<Param> params) {
      this.params.clear();
      this.params.addAll(params);
    }

    public void addAllDeprecatedKeys(List<RuleKey> deprecatedKeys) {
      this.deprecatedKeys.addAll(deprecatedKeys);
    }

    public void setImpacts(Map<SoftwareQuality, Severity> impacts) {
      this.impacts.clear();
      this.impacts.putAll(impacts);
    }

    public ActiveRule build() {
      return new ActiveRule(ruleKey, name, severity, createdAt, updatedAt, internalKey, language, templateRuleKey, qProfilKey, deprecatedKeys, params, impacts);
    }
  }

}
