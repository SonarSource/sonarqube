/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.rules;

import java.util.List;
import javax.annotation.Generated;

/**
 * Update an existing rule.<br>Requires the 'Administer Quality Profiles' permission
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/update">Further information about this action online (including a response example)</a>
 * @since 4.4
 */
@Generated("sonar-ws-generator")
public class UpdateRequest {

  private String debtRemediationFnOffset;
  private String debtRemediationFnType;
  private String debtRemediationFyCoeff;
  private String debtSubCharacteristic;
  private String key;
  private String markdownDescription;
  private String markdownNote;
  private String name;
  private String organization;
  private String params;
  private String remediationFnBaseEffort;
  private String remediationFnType;
  private String remediationFyGapMultiplier;
  private String severity;
  private String status;
  private List<String> tags;

  /**
   * @deprecated since 5.5
   */
  @Deprecated
  public UpdateRequest setDebtRemediationFnOffset(String debtRemediationFnOffset) {
    this.debtRemediationFnOffset = debtRemediationFnOffset;
    return this;
  }

  public String getDebtRemediationFnOffset() {
    return debtRemediationFnOffset;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"LINEAR"</li>
   *   <li>"LINEAR_OFFSET"</li>
   *   <li>"CONSTANT_ISSUE"</li>
   * </ul>
   * @deprecated since 5.5
   */
  @Deprecated
  public UpdateRequest setDebtRemediationFnType(String debtRemediationFnType) {
    this.debtRemediationFnType = debtRemediationFnType;
    return this;
  }

  public String getDebtRemediationFnType() {
    return debtRemediationFnType;
  }

  /**
   * @deprecated since 5.5
   */
  @Deprecated
  public UpdateRequest setDebtRemediationFyCoeff(String debtRemediationFyCoeff) {
    this.debtRemediationFyCoeff = debtRemediationFyCoeff;
    return this;
  }

  public String getDebtRemediationFyCoeff() {
    return debtRemediationFyCoeff;
  }

  /**
   * Debt characteristics are no more supported. This parameter is ignored.
   *
   * @deprecated since 5.5
   */
  @Deprecated
  public UpdateRequest setDebtSubCharacteristic(String debtSubCharacteristic) {
    this.debtSubCharacteristic = debtSubCharacteristic;
    return this;
  }

  public String getDebtSubCharacteristic() {
    return debtSubCharacteristic;
  }

  /**
   * Key of the rule to update
   *
   * This is a mandatory parameter.
   * Example value: "javascript:NullCheck"
   */
  public UpdateRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Rule description (mandatory for custom rule and manual rule)
   *
   * Example value: "Description of my custom rule"
   */
  public UpdateRequest setMarkdownDescription(String markdownDescription) {
    this.markdownDescription = markdownDescription;
    return this;
  }

  public String getMarkdownDescription() {
    return markdownDescription;
  }

  /**
   * Optional note in markdown format. Use empty value to remove current note. Note is not changedif the parameter is not set.
   *
   * Example value: "my *note*"
   */
  public UpdateRequest setMarkdownNote(String markdownNote) {
    this.markdownNote = markdownNote;
    return this;
  }

  public String getMarkdownNote() {
    return markdownNote;
  }

  /**
   * Rule name (mandatory for custom rule)
   *
   * Example value: "My custom rule"
   */
  public UpdateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * Organization key
   *
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public UpdateRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Parameters as semi-colon list of <key>=<value>, for example 'params=key1=v1;key2=v2' (Only when updating a custom rule)
   *
   */
  public UpdateRequest setParams(String params) {
    this.params = params;
    return this;
  }

  public String getParams() {
    return params;
  }

  /**
   * Base effort of the remediation function of the rule
   *
   * Example value: "1d"
   */
  public UpdateRequest setRemediationFnBaseEffort(String remediationFnBaseEffort) {
    this.remediationFnBaseEffort = remediationFnBaseEffort;
    return this;
  }

  public String getRemediationFnBaseEffort() {
    return remediationFnBaseEffort;
  }

  /**
   * Type of the remediation function of the rule
   *
   * Possible values:
   * <ul>
   *   <li>"LINEAR"</li>
   *   <li>"LINEAR_OFFSET"</li>
   *   <li>"CONSTANT_ISSUE"</li>
   * </ul>
   */
  public UpdateRequest setRemediationFnType(String remediationFnType) {
    this.remediationFnType = remediationFnType;
    return this;
  }

  public String getRemediationFnType() {
    return remediationFnType;
  }

  /**
   * Gap multiplier of the remediation function of the rule
   *
   * Example value: "3min"
   */
  public UpdateRequest setRemediationFyGapMultiplier(String remediationFyGapMultiplier) {
    this.remediationFyGapMultiplier = remediationFyGapMultiplier;
    return this;
  }

  public String getRemediationFyGapMultiplier() {
    return remediationFyGapMultiplier;
  }

  /**
   * Rule severity (Only when updating a custom rule)
   *
   * Possible values:
   * <ul>
   *   <li>"INFO"</li>
   *   <li>"MINOR"</li>
   *   <li>"MAJOR"</li>
   *   <li>"CRITICAL"</li>
   *   <li>"BLOCKER"</li>
   * </ul>
   */
  public UpdateRequest setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  /**
   * Rule status (Only when updating a custom rule)
   *
   * Possible values:
   * <ul>
   *   <li>"BETA"</li>
   *   <li>"DEPRECATED"</li>
   *   <li>"READY"</li>
   *   <li>"REMOVED"</li>
   * </ul>
   */
  public UpdateRequest setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getStatus() {
    return status;
  }

  /**
   * Optional comma-separated list of tags to set. Use blank value to remove current tags. Tags are not changed if the parameter is not set.
   *
   * Example value: "java8,security"
   */
  public UpdateRequest setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }
}
