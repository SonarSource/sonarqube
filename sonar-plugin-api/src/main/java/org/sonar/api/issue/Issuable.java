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

package org.sonar.api.issue;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.component.Perspective;
import org.sonar.api.rule.RuleKey;

/**
 * This perspective allows to add and get issues related to the selected component. It can be used from
 * {@link org.sonar.api.batch.Sensor}s and {@link org.sonar.api.batch.Decorator}s. Web extensions
 * must use {@link RubyIssueService}.
 * <p/>
 * Example:
 * <pre>
 *   import org.sonar.api.component.ResourcePerspectives;
 *   public class MySensor extends Sensor {
 *     private final ResourcePerspectives perspectives;
 *
 *     public MySensor(ResourcePerspectives p) {
 *       this.perspectives = p;
 *     }
 *
 *     public void analyse(Project project, SensorContext context) {
 *       Resource myResource; // to be set
 *       Issuable issuable = perspectives.as(Issuable.class, myResource);
 *       if (issuable != null) {
 *         // can be used
 *         Issue issue = issuable.newIssueBuilder()
 *           .setRuleKey(RuleKey.of("pmd", "AvoidArrayLoops")
 *           .setLine(10)
 *           .build();
 *         issuable.addIssue(issue);
 *       }
 *     }
 *   }
 * </pre>
 * @since 3.6
 */
public interface Issuable extends Perspective {

  interface IssueBuilder {
    /**
     * The rule key is mandatory. Example: {@code RuleKey.of("pmd", "AvoidArrayLoops")}
     */
    IssueBuilder ruleKey(RuleKey ruleKey);

    /**
     * Optional line index, starting from 1. It must not be zero or negative.
     * @deprecated since 5.2 use {@link #at(NewIssueLocation)}
     */
    @Deprecated
    IssueBuilder line(@Nullable Integer line);

    /**
     * Optional, but recommended, plain-text message.
     * <p/>
     * Formats like Markdown or HTML are not supported. Size must not be greater than {@link Issue#MESSAGE_MAX_SIZE} characters.
     * @deprecated since 5.2 use {@link #at(NewIssueLocation)}
     */
    @Deprecated
    IssueBuilder message(@Nullable String message);

    /**
     * @since 5.2
     * Create a new location for this issue. First registered location is considered as primary location.
     */
    NewIssueLocation newLocation();

    /**
     * @since 5.2
     * Register primary location for this issue.
     */
    IssueBuilder at(NewIssueLocation primaryLocation);

    /**
     * @since 5.2
     * Register a new secondary location for this issue.
     */
    IssueBuilder addLocation(NewIssueLocation location);

    /**
     * @since 5.2
     * Register an execution flow for this issue. An execution flow is an ordered list of issue locations that help to understand the issue.
     * It is usually the path leading to the primary location. Several execution flows can be registered.
     */
    IssueBuilder addExecutionFlow(Iterable<NewIssueLocation> flowLocations);

    /**
     * Overrides the severity declared in Quality profile. Do not execute in standard use-cases.
     * @see org.sonar.api.rule.Severity
     */
    IssueBuilder severity(@Nullable String severity);

    /**
     * Login of the user who reported the issue. Optional.
     */
    IssueBuilder reporter(@Nullable String reporter);

    IssueBuilder effortToFix(@Nullable Double d);

    IssueBuilder attribute(String key, @Nullable String value);

    Issue build();
  }

  /**
   * Builder is used to create the issue to be passed to {@link #addIssue(Issue)}
   */
  IssueBuilder newIssueBuilder();

  /**
   * Register an issue created with {@link #newIssueBuilder()}.
   * <p/>
   * This method is usually called from {@link org.sonar.api.batch.Sensor}s. {@link org.sonar.api.batch.Decorator}s calling this
   * method must be annotated with {@code @DependedUpon(DecoratorBarriers.ISSUES_ADDED)}.
   *
   * @return true if the new issue is registered, false if the related rule does not exist or is disabled in the Quality profile.
   */
  boolean addIssue(Issue issue);

  /**
   * @deprecated since 5.2 no more decorators on batch side
   */
  @Deprecated
  List<Issue> issues();

  /**
   * @deprecated since 5.2 no more decorators on batch side
   */
  @Deprecated
  List<Issue> resolvedIssues();
}
