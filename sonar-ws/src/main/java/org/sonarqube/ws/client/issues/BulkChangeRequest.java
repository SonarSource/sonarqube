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
package org.sonarqube.ws.client.issues;

import java.util.List;
import javax.annotation.Generated;

/**
 * Bulk change on issues.<br/>Requires authentication.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/bulk_change">Further information about this action online (including a response example)</a>
 * @since 3.7
 */
@Generated("sonar-ws-generator")
public class BulkChangeRequest {

  private String addTags;
  private String assign;
  private String comment;
  private String doTransition;
  private List<String> issues;
  private String plan;
  private String removeTags;
  private String sendNotifications;
  private String setSeverity;
  private String setType;

  /**
   * Add tags
   *
   * Example value: "security,java8"
   */
  public BulkChangeRequest setAddTags(String addTags) {
    this.addTags = addTags;
    return this;
  }

  public String getAddTags() {
    return addTags;
  }

  /**
   * To assign the list of issues to a specific user (login), or un-assign all the issues
   *
   * Example value: "john.smith"
   */
  public BulkChangeRequest setAssign(String assign) {
    this.assign = assign;
    return this;
  }

  public String getAssign() {
    return assign;
  }

  /**
   * To add a comment to a list of issues
   *
   * Example value: "Here is my comment"
   */
  public BulkChangeRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public String getComment() {
    return comment;
  }

  /**
   * Transition
   *
   * Example value: "reopen"
   * Possible values:
   * <ul>
   *   <li>"confirm"</li>
   *   <li>"unconfirm"</li>
   *   <li>"reopen"</li>
   *   <li>"resolve"</li>
   *   <li>"falsepositive"</li>
   *   <li>"wontfix"</li>
   *   <li>"close"</li>
   * </ul>
   */
  public BulkChangeRequest setDoTransition(String doTransition) {
    this.doTransition = doTransition;
    return this;
  }

  public String getDoTransition() {
    return doTransition;
  }

  /**
   * Comma-separated list of issue keys
   *
   * This is a mandatory parameter.
   * Example value: "AU-Tpxb--iU5OvuD2FLy,AU-TpxcA-iU5OvuD2FLz"
   */
  public BulkChangeRequest setIssues(List<String> issues) {
    this.issues = issues;
    return this;
  }

  public List<String> getIssues() {
    return issues;
  }

  /**
   * In 5.5, action plans are dropped. Has no effect. To plan the list of issues to a specific action plan (key), or unlink all the issues from an action plan
   *
   * @deprecated since 5.5
   */
  @Deprecated
  public BulkChangeRequest setPlan(String plan) {
    this.plan = plan;
    return this;
  }

  public String getPlan() {
    return plan;
  }

  /**
   * Remove tags
   *
   * Example value: "security,java8"
   */
  public BulkChangeRequest setRemoveTags(String removeTags) {
    this.removeTags = removeTags;
    return this;
  }

  public String getRemoveTags() {
    return removeTags;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public BulkChangeRequest setSendNotifications(String sendNotifications) {
    this.sendNotifications = sendNotifications;
    return this;
  }

  public String getSendNotifications() {
    return sendNotifications;
  }

  /**
   * To change the severity of the list of issues
   *
   * Example value: "BLOCKER"
   * Possible values:
   * <ul>
   *   <li>"INFO"</li>
   *   <li>"MINOR"</li>
   *   <li>"MAJOR"</li>
   *   <li>"CRITICAL"</li>
   *   <li>"BLOCKER"</li>
   * </ul>
   */
  public BulkChangeRequest setSetSeverity(String setSeverity) {
    this.setSeverity = setSeverity;
    return this;
  }

  public String getSetSeverity() {
    return setSeverity;
  }

  /**
   * To change the type of the list of issues
   *
   * Example value: "BUG"
   * Possible values:
   * <ul>
   *   <li>"CODE_SMELL"</li>
   *   <li>"BUG"</li>
   *   <li>"VULNERABILITY"</li>
   * </ul>
   */
  public BulkChangeRequest setSetType(String setType) {
    this.setType = setType;
    return this;
  }

  public String getSetType() {
    return setType;
  }
}
