/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;

public class IssueDbTester {
  private static final RuleType[] RULE_TYPES_EXCEPT_HOTSPOTS = Arrays.stream(RuleType.values())
    .filter(ruleType -> SECURITY_HOTSPOT != ruleType).toArray(RuleType[]::new);

  private final DbTester db;

  public IssueDbTester(DbTester db) {
    this.db = db;
  }

  /**
   * Inserts an issue or a security hotspot.
   */
  @SafeVarargs
  public final IssueDto insert(RuleDefinitionDto rule, ComponentDto project, ComponentDto file, Consumer<IssueDto>... populators) {
    IssueDto issue = newIssue(rule, project, file);
    stream(populators).forEach(p -> p.accept(issue));
    return insert(issue);
  }

  /**
   * Inserts an issue or a security hotspot.
   */
  @SafeVarargs
  public final IssueDto insert(OrganizationDto organizationDto, Consumer<IssueDto>... populators) {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertPublicProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue = newIssue(rule, project, file);
    stream(populators).forEach(p -> p.accept(issue));
    return insert(issue);
  }

  /**
   * Inserts an issue or a security hotspot.
   */
  public IssueDto insert(IssueDto issue) {
    db.getDbClient().issueDao().insert(db.getSession(), issue);
    db.commit();
    return issue;
  }

  /**
   * Inserts an issue.
   *
   * @throws AssertionError if rule is a Security Hotspot
   */
  @SafeVarargs
  public final IssueDto insertIssue(RuleDefinitionDto rule, ComponentDto project, ComponentDto file, Consumer<IssueDto>... populators) {
    assertThat(rule.getType())
      .describedAs("rule must not be a Security Hotspot type")
      .isNotEqualTo(SECURITY_HOTSPOT.getDbConstant());
    IssueDto issue = newIssue(rule, project, file)
      .setType(RULE_TYPES_EXCEPT_HOTSPOTS[new Random().nextInt(RULE_TYPES_EXCEPT_HOTSPOTS.length)]);
    stream(populators).forEach(p -> p.accept(issue));
    return insertIssue(issue);
  }

  @SafeVarargs
  public final IssueDto insert(RuleDefinitionDto rule, ProjectDto project, ComponentDto file, Consumer<IssueDto>... populators) {
    IssueDto issue = newIssue(rule, project, file);
    stream(populators).forEach(p -> p.accept(issue));
    return insert(issue);
  }

  /**
   * Inserts an issue.
   *
   * @throws AssertionError if issueDto is a Security Hotspot
   */
  public IssueDto insertIssue(IssueDto issueDto) {
    assertThat(issueDto.getType())
      .describedAs("Issue must not be a Security Hotspot")
      .isNotEqualTo(SECURITY_HOTSPOT.getDbConstant());
    return insert(issueDto);
  }

  /**
   * Inserts an issue.
   */
  @SafeVarargs
  public final IssueDto insertIssue(Consumer<IssueDto>... populateIssueDto) {
    return insertIssue(db.getDefaultOrganization(), populateIssueDto);
  }

  /**
   * Inserts an issue.
   *
   * @throws AssertionError if rule is not Security Hotspot
   */
  @SafeVarargs
  public final IssueDto insertIssue(OrganizationDto organizationDto, Consumer<IssueDto>... populators) {
    RuleDefinitionDto rule = db.rules().insertIssueRule();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue = newIssue(rule, project, file)
      .setType(RULE_TYPES_EXCEPT_HOTSPOTS[new Random().nextInt(RULE_TYPES_EXCEPT_HOTSPOTS.length)]);
    stream(populators).forEach(p -> p.accept(issue));
    return insertIssue(issue);
  }

  /**
   * Inserts a Security Hotspot.
   *
   * @throws AssertionError if rule is not Security Hotspot
   */
  @SafeVarargs
  public final IssueDto insertHotspot(RuleDefinitionDto rule, ComponentDto project, ComponentDto file, Consumer<IssueDto>... populators) {
    checkArgument(rule.getType() == RuleType.SECURITY_HOTSPOT.getDbConstant(), "rule must be a hotspot rule");

    IssueDto issue = newIssue(rule, project, file)
      .setType(SECURITY_HOTSPOT)
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setResolution(null);
    stream(populators).forEach(p -> p.accept(issue));
    return insertHotspot(issue);
  }

  /**
   * Inserts a Security Hotspot.
   */
  @SafeVarargs
  public final IssueDto insertHotspot(ComponentDto project, ComponentDto file, Consumer<IssueDto>... populators) {
    RuleDefinitionDto rule = db.rules().insertHotspotRule();
    IssueDto issue = newIssue(rule, project, file)
      .setType(SECURITY_HOTSPOT)
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setResolution(null);
    stream(populators).forEach(p -> p.accept(issue));
    return insertHotspot(issue);
  }

  /**
   * Inserts a Security Hotspot.
   *
   * @throws AssertionError if issueDto is not Security Hotspot
   */
  public IssueDto insertHotspot(IssueDto issueDto) {
    assertThat(issueDto.getType())
      .describedAs("IssueDto must have Security Hotspot type")
      .isEqualTo(SECURITY_HOTSPOT.getDbConstant());
    return insert(issueDto);
  }

  /**
   * Inserts a Security Hotspot.
   */
  @SafeVarargs
  public final IssueDto insertHotspot(Consumer<IssueDto>... populateIssueDto) {
    return insertHotspot(db.getDefaultOrganization(), populateIssueDto);
  }

  /**
   * Inserts a Security Hotspot.
   */
  @SafeVarargs
  public final IssueDto insertHotspot(OrganizationDto organizationDto, Consumer<IssueDto>... populators) {
    RuleDefinitionDto rule = db.rules().insertHotspotRule();
    ComponentDto project = db.components().insertPrivateProject(organizationDto);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue = newIssue(rule, project, file)
      .setType(SECURITY_HOTSPOT)
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setResolution(null);
    stream(populators).forEach(p -> p.accept(issue));
    return insertHotspot(issue);
  }

  @SafeVarargs
  public final IssueChangeDto insertChange(IssueDto issueDto, Consumer<IssueChangeDto>... populators) {
    IssueChangeDto dto = IssueTesting.newIssuechangeDto(issueDto);
    stream(populators).forEach(p -> p.accept(dto));
    return insertChange(dto);
  }

  public IssueChangeDto insertChange(IssueChangeDto issueChangeDto) {
    db.getDbClient().issueChangeDao().insert(db.getSession(), issueChangeDto);
    db.commit();
    return issueChangeDto;
  }

  public IssueChangeDto insertComment(IssueDto issueDto, @Nullable UserDto user, String text) {
    IssueChangeDto issueChangeDto = IssueChangeDto.of(DefaultIssueComment.create(issueDto.getKey(), user == null ? null : user.getUuid(), text));
    return insertChange(issueChangeDto);
  }

  public void insertFieldDiffs(IssueDto issueDto, FieldDiffs... diffs) {
    Arrays.stream(diffs).forEach(diff -> db.getDbClient().issueChangeDao().insert(db.getSession(), IssueChangeDto.of(issueDto.getKey(), diff)));
    db.commit();
  }

}
