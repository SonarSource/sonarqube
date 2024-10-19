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
package org.sonar.api.batch.sensor.issue.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;
import org.sonar.api.batch.sensor.issue.fix.QuickFix;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultIssue extends AbstractDefaultIssue<DefaultIssue> implements Issue, NewIssue {
  private final Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> overridenImpacts = new EnumMap<>(SoftwareQuality.class);
  private RuleKey ruleKey;
  private Double gap;
  private Severity overriddenSeverity;
  private boolean quickFixAvailable = false;
  private String ruleDescriptionContextKey;
  private List<String> codeVariants;

  public DefaultIssue(DefaultInputProject project) {
    this(project, null);
  }

  public DefaultIssue(DefaultInputProject project, @Nullable SensorStorage storage) {
    super(project, storage);
  }

  public DefaultIssue forRule(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public RuleKey ruleKey() {
    return this.ruleKey;
  }

  @Override
  public DefaultIssue gap(@Nullable Double gap) {
    checkArgument(gap == null || gap >= 0, format("Gap must be greater than or equal 0 (got %s)", gap));
    this.gap = gap;
    return this;
  }

  @Override
  public DefaultIssue overrideSeverity(@Nullable Severity severity) {
    this.overriddenSeverity = severity;
    return this;
  }

  @Override
  public DefaultIssue overrideImpact(SoftwareQuality softwareQuality, org.sonar.api.issue.impact.Severity severity) {
    overridenImpacts.put(softwareQuality, severity);
    return this;
  }

  @Override
  public DefaultIssue setQuickFixAvailable(boolean quickFixAvailable) {
    this.quickFixAvailable = quickFixAvailable;
    return this;
  }

  @Override
  public NewQuickFix newQuickFix() {
    return new NoOpNewQuickFix();
  }

  @Override
  public NewIssue addQuickFix(NewQuickFix newQuickFix) {
    this.quickFixAvailable = true;
    return this;
  }

  @Override
  public DefaultIssue setRuleDescriptionContextKey(@Nullable String ruleDescriptionContextKey) {
    this.ruleDescriptionContextKey = ruleDescriptionContextKey;
    return this;
  }

  @Override
  public DefaultIssue setCodeVariants(@Nullable Iterable<String> codeVariants) {
    if (codeVariants != null) {
      List<String> codeVariantsList = new ArrayList<>();
      codeVariants.forEach(codeVariantsList::add);
      this.codeVariants = codeVariantsList;
    }
    return this;
  }

  @Override
  public boolean isQuickFixAvailable() {
    return quickFixAvailable;
  }

  @Override
  public Optional<String> ruleDescriptionContextKey() {
    return Optional.ofNullable(ruleDescriptionContextKey);
  }

  @Override
  public List<QuickFix> quickFixes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> codeVariants() {
    return codeVariants;
  }

  @Override
  public Severity overriddenSeverity() {
    return this.overriddenSeverity;
  }

  @Override
  public Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> overridenImpacts() {
    return overridenImpacts;
  }

  @Override
  public Double gap() {
    return this.gap;
  }

  @Override
  public IssueLocation primaryLocation() {
    return primaryLocation;
  }

  @Override
  public void doSave() {
    requireNonNull(this.ruleKey, "ruleKey is mandatory on issue");
    checkState(primaryLocation != null, "Primary location is mandatory on every issue");
    storage.store(this);
  }

}
