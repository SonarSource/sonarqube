/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.postjob;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.Issue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.IssueCache;

import javax.annotation.Nullable;

public class DefaultPostJobContext implements PostJobContext {

  private final Settings settings;
  private final AnalysisMode analysisMode;
  private final IssueCache cache;
  private final ResourceCache resourceCache;

  public DefaultPostJobContext(Settings settings, AnalysisMode analysisMode, IssueCache cache, ResourceCache resourceCache) {
    this.settings = settings;
    this.analysisMode = analysisMode;
    this.cache = cache;
    this.resourceCache = resourceCache;
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
  public Iterable<Issue> issues() {
    return Iterables.transform(Iterables.filter(cache.all(), new ResolvedPredicate(false)), new Function<DefaultIssue, Issue>() {
      @Override
      public Issue apply(DefaultIssue input) {
        return new DefaultIssueWrapper(input);
      }
    });
  }

  @Override
  public Iterable<Issue> resolvedIssues() {
    return Iterables.transform(Iterables.filter(cache.all(), new ResolvedPredicate(true)), new Function<DefaultIssue, Issue>() {
      @Override
      public Issue apply(DefaultIssue input) {
        return new DefaultIssueWrapper(input);
      }
    });
  }

  private class DefaultIssueWrapper implements Issue {

    private final DefaultIssue wrapped;

    public DefaultIssueWrapper(DefaultIssue wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public String key() {
      return wrapped.key();
    }

    @Override
    public RuleKey ruleKey() {
      return wrapped.ruleKey();
    }

    @Override
    public String componentKey() {
      return wrapped.componentKey();
    }

    @Override
    public InputPath inputPath() {
      BatchResource component = resourceCache.get(wrapped.componentKey());
      return component != null ? component.inputPath() : null;
    }

    @Override
    public Integer line() {
      return wrapped.line();
    }

    @Override
    public Double effortToFix() {
      return wrapped.effortToFix();
    }

    @Override
    public String message() {
      return wrapped.message();
    }

    @Override
    public Severity severity() {
      String severity = wrapped.severity();
      return severity != null ? Severity.valueOf(severity) : null;
    }

    @Override
    public boolean isNew() {
      return wrapped.isNew();
    }

  }

  private static class ResolvedPredicate implements Predicate<DefaultIssue> {
    private final boolean resolved;

    private ResolvedPredicate(boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public boolean apply(@Nullable DefaultIssue issue) {
      if (issue != null) {
        return resolved ? issue.resolution() != null : issue.resolution() == null;
      }
      return false;
    }
  }

}
