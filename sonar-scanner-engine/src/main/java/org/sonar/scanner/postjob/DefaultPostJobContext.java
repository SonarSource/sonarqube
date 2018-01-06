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
package org.sonar.scanner.postjob;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

public class DefaultPostJobContext implements PostJobContext {

  private final Configuration config;
  private final IssueCache cache;
  private final AnalysisMode analysisMode;
  private InputComponentStore inputComponentStore;
  private final Settings mutableSettings;

  public DefaultPostJobContext(Configuration config, Settings mutableSettings, IssueCache cache, InputComponentStore inputComponentStore,
    AnalysisMode analysisMode) {
    this.config = config;
    this.mutableSettings = mutableSettings;
    this.cache = cache;
    this.inputComponentStore = inputComponentStore;
    this.analysisMode = analysisMode;
  }

  @Override
  public Settings settings() {
    return mutableSettings;
  }

  @Override
  public Configuration config() {
    return config;
  }

  @Override
  public AnalysisMode analysisMode() {
    return analysisMode;
  }

  @Override
  public Iterable<PostJobIssue> issues() {
    if (!analysisMode.isIssues()) {
      throw new UnsupportedOperationException("Issues are only available to PostJobs in 'issues' mode.");
    }
    return StreamSupport.stream(cache.all().spliterator(), false)
      .filter(new ResolvedPredicate(false))
      .map(DefaultIssueWrapper::new)
      .collect(Collectors.toList());
  }

  @Override
  public Iterable<PostJobIssue> resolvedIssues() {
    if (!analysisMode.isIssues()) {
      throw new UnsupportedOperationException("Resolved issues are only available to PostJobs in 'issues' mode.");
    }
    return StreamSupport.stream(cache.all().spliterator(), false)
      .filter(new ResolvedPredicate(true))
      .map(DefaultIssueWrapper::new)
      .collect(Collectors.toList());
  }

  private class DefaultIssueWrapper implements PostJobIssue {

    private final TrackedIssue wrapped;

    public DefaultIssueWrapper(TrackedIssue wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public String key() {
      return wrapped.key();
    }

    @Override
    public RuleKey ruleKey() {
      return wrapped.getRuleKey();
    }

    @Override
    public String componentKey() {
      return wrapped.componentKey();
    }

    @Override
    public InputComponent inputComponent() {
      return inputComponentStore.getByKey(wrapped.componentKey());
    }

    @Override
    public Integer line() {
      return wrapped.startLine();
    }

    @Override
    public String message() {
      return wrapped.getMessage();
    }

    @Override
    public Severity severity() {
      return Severity.valueOf(wrapped.severity());
    }

    @Override
    public boolean isNew() {
      return wrapped.isNew();
    }
  }

  private static class ResolvedPredicate implements Predicate<TrackedIssue> {
    private final boolean resolved;

    private ResolvedPredicate(boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public boolean test(@Nullable TrackedIssue issue) {
      if (issue != null) {
        return resolved ? issue.resolution() != null : issue.resolution() == null;
      }
      return false;
    }
  }

}
