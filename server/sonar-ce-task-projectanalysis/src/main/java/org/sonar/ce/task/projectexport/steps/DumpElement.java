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
package org.sonar.ce.task.projectexport.steps;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import javax.annotation.concurrent.Immutable;

/**
 * File element in the dump report.
 */
@Immutable
public class DumpElement<M extends Message> {
  public static final long NO_DATETIME = 0;

  public static final DumpElement<ProjectDump.Metadata> METADATA = new DumpElement<>("metadata.pb", ProjectDump.Metadata.parser());
  public static final DumpElement<ProjectDump.Component> COMPONENTS = new DumpElement<>("components.pb", ProjectDump.Component.parser());
  public static final DumpElement<ProjectDump.Branch> BRANCHES = new DumpElement<>("branches.pb", ProjectDump.Branch.parser());
  public static final DumpElement<ProjectDump.Analysis> ANALYSES = new DumpElement<>("analyses.pb", ProjectDump.Analysis.parser());
  public static final DumpElement<ProjectDump.Measure> MEASURES = new DumpElement<>("measures.pb", ProjectDump.Measure.parser());
  public static final DumpElement<ProjectDump.LiveMeasure> LIVE_MEASURES = new DumpElement<>("live_measures.pb", ProjectDump.LiveMeasure.parser());
  public static final DumpElement<ProjectDump.Metric> METRICS = new DumpElement<>("metrics.pb", ProjectDump.Metric.parser());
  public static final IssueDumpElement ISSUES = new IssueDumpElement();
  public static final DumpElement<ProjectDump.IssueChange> ISSUES_CHANGELOG = new DumpElement<>("issues_changelog.pb", ProjectDump.IssueChange.parser());
  public static final DumpElement<ProjectDump.AdHocRule> AD_HOC_RULES = new DumpElement<>("ad_hoc_rules.pb", ProjectDump.AdHocRule.parser());
  public static final DumpElement<ProjectDump.Rule> RULES = new DumpElement<>("rules.pb", ProjectDump.Rule.parser());
  public static final DumpElement<ProjectDump.Link> LINKS = new DumpElement<>("links.pb", ProjectDump.Link.parser());
  public static final DumpElement<ProjectDump.Event> EVENTS = new DumpElement<>("events.pb", ProjectDump.Event.parser());
  public static final DumpElement<ProjectDump.Setting> SETTINGS = new DumpElement<>("settings.pb", ProjectDump.Setting.parser());
  public static final DumpElement<ProjectDump.Plugin> PLUGINS = new DumpElement<>("plugins.pb", ProjectDump.Plugin.parser());
  public static final DumpElement<ProjectDump.LineHashes> LINES_HASHES = new DumpElement<>("lines_hashes.pb", ProjectDump.LineHashes.parser());
  public static final DumpElement<ProjectDump.NewCodePeriod> NEW_CODE_PERIODS = new DumpElement<>("new_code_periods.pb", ProjectDump.NewCodePeriod.parser());

  private final String filename;
  private final Parser<M> parser;

  private DumpElement(String filename, Parser<M> parser) {
    this.filename = filename;
    this.parser = parser;
  }

  public String filename() {
    return filename;
  }

  public Parser<M> parser() {
    return parser;
  }

  public static class IssueDumpElement extends DumpElement<ProjectDump.Issue> {
    public static final int NO_LINE = 0;
    public static final double NO_GAP = -1;
    public static final long NO_EFFORT = -1;

    public IssueDumpElement() {
      super("issues.pb", ProjectDump.Issue.parser());
    }
  }
}
