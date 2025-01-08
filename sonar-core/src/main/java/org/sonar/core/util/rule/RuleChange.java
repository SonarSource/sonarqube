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
package org.sonar.core.util.rule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.util.ParamChange;

public class RuleChange implements Serializable {
  private String key;
  private String language;
  private String templateKey;
  private String severity;
  private ParamChange[] params = new ParamChange[0];
  private final List<Impact> impacts = new ArrayList<>();

  public String getKey() {
    return key;
  }

  public RuleChange setKey(String key) {
    this.key = key;
    return this;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public RuleChange setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public RuleChange setTemplateKey(String templateKey) {
    this.templateKey = templateKey;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public RuleChange setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  public ParamChange[] getParams() {
    return params;
  }

  public RuleChange setParams(ParamChange[] params) {
    this.params = params;
    return this;
  }

  public List<Impact> getImpacts() {
    return impacts;
  }

  public void addImpact(SoftwareQuality softwareQuality, Severity severity) {
    impacts.add(new Impact(softwareQuality, severity));
  }

  static class Impact implements Serializable {
    private final SoftwareQuality softwareQuality;
    private final Severity severity;

    Impact(SoftwareQuality softwareQuality, Severity severity) {
      this.softwareQuality = softwareQuality;
      this.severity = severity;
    }

    public SoftwareQuality getSoftwareQuality() {
      return softwareQuality;
    }

    public Severity getSeverity() {
      return severity;
    }
  }
}
