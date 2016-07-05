/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;

public class DefaultPostJobContext implements PostJobContext {

  private final Settings settings;
  private final IssueCache cache;
  private final BatchComponentCache resourceCache;
  private final AnalysisMode analysisMode;

  public DefaultPostJobContext(Settings settings, IssueCache cache, BatchComponentCache resourceCache, AnalysisMode analysisMode) {
    this.settings = settings;
    this.cache = cache;
    this.resourceCache = resourceCache;
    this.analysisMode = analysisMode;
  }

  @Override
  public Settings settings() {
    return settings;
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
    return Iterables.transform(Iterables.filter(cache.all(), new ResolvedPredicate(false)), new IssueTransformer());
  }

  @Override
  public Iterable<PostJobIssue> resolvedIssues() {
    if (!analysisMode.isIssues()) {
      throw new UnsupportedOperationException("Resolved issues are only available to PostJobs in 'issues' mode.");
    }
    return Iterables.transform(Iterables.filter(cache.all(), new ResolvedPredicate(true)), new IssueTransformer());
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
      BatchComponent component = resourceCache.get(wrapped.componentKey());
      return component != null ? component.inputComponent() : null;
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

  private class IssueTransformer implements Function<TrackedIssue, PostJobIssue> {
    @Override
    public PostJobIssue apply(TrackedIssue input) {
      return new DefaultIssueWrapper(input);
    }
  }

  private static class ResolvedPredicate implements Predicate<TrackedIssue> {
    private final boolean resolved;

    private ResolvedPredicate(boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public boolean apply(@Nullable TrackedIssue issue) {
      if (issue != null) {
        return resolved ? issue.resolution() != null : issue.resolution() == null;
      }
      return false;
    }
  }

}
