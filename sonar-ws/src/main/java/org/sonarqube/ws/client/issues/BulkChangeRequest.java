/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/bulk_change">Further information about this action online (including a response example)</a>
 * @since 3.7
 */
@Generated("sonar-ws-generator")
public class BulkChangeRequest {

  private String addTags;
  private List<String> assign;
  private List<String> comment;
  private String doTransition;
  private List<String> issues;
  private String removeTags;
  private String sendNotifications;
  private List<String> setSeverity;
  private List<String> setType;

  /**
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
   * Example value: "john.smith"
   */
  public BulkChangeRequest setAssign(List<String> assign) {
    this.assign = assign;
    return this;
  }

  public List<String> getAssign() {
    return assign;
  }

  /**
   * Example value: "Here is my comment"
   */
  public BulkChangeRequest setComment(List<String> comment) {
    this.comment = comment;
    return this;
  }

  public List<String> getComment() {
    return comment;
  }

  /**
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
  public BulkChangeRequest setSetSeverity(List<String> setSeverity) {
    this.setSeverity = setSeverity;
    return this;
  }

  public List<String> getSetSeverity() {
    return setSeverity;
  }

  /**
   * Example value: "BUG"
   * Possible values:
   * <ul>
   *   <li>"CODE_SMELL"</li>
   *   <li>"BUG"</li>
   *   <li>"VULNERABILITY"</li>
   * </ul>
   */
  public BulkChangeRequest setSetType(List<String> setType) {
    this.setType = setType;
    return this;
  }

  public List<String> getSetType() {
    return setType;
  }
}
