/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.IssueResolution;
import org.sonar.api.batch.sensor.issue.NewIssueResolution;
import org.sonar.api.rule.RuleKey;

import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultIssueResolution extends DefaultStorable implements NewIssueResolution, IssueResolution {

  private InputFile inputFile;
  private TextRange textRange;
  private Status status;
  private final Set<RuleKey> ruleKeys = new HashSet<>();
  private String comment;

  public DefaultIssueResolution(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public NewIssueResolution on(InputFile inputFile) {
    this.inputFile = inputFile;
    return this;
  }

  @Override
  public NewIssueResolution at(TextRange textRange) {
    this.textRange = textRange;
    return this;
  }

  @Override
  public NewIssueResolution status(Status status) {
    this.status = status;
    return this;
  }

  @Override
  public NewIssueResolution forRules(Collection<RuleKey> ruleKeys) {
    this.ruleKeys.addAll(ruleKeys);
    return this;
  }

  @Override
  public NewIssueResolution comment(String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  @CheckForNull
  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  @CheckForNull
  public TextRange textRange() {
    return textRange;
  }

  @Override
  public Status status() {
    return status;
  }

  @Override
  public Set<RuleKey> ruleKeys() {
    return ruleKeys;
  }

  @Override
  public String comment() {
    return comment;
  }

  @Override
  protected void doSave() {
    checkState(inputFile != null, "An input file is required");
    checkState(textRange != null, "A text range is required");
    checkArgument(!ruleKeys.isEmpty(), "At least one rule key is required");
    checkState(comment != null, "A comment is required");
    if (status == null) {
      status = Status.DEFAULT;
    }
    storage.store(this);
  }
}
