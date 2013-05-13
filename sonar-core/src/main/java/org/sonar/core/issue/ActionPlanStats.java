/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.issue;

import org.sonar.api.issue.ActionPlan;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class ActionPlanStats implements Serializable {

  private String key;
  private String name;
  private String description;
  private String userLogin;
  private String status;
  private Date deadLine;
  private Date createdAt;
  private Date updatedAt;
  private int totalIssues;
  private int openIssues;

  private ActionPlanStats() {

  }

  public static ActionPlanStats create(String name) {
    ActionPlanStats actionPlan = new ActionPlanStats();
    actionPlan.setKey(UUID.randomUUID().toString());
    Date now = new Date();
    actionPlan.setName(name);
    actionPlan.setStatus(ActionPlan.STATUS_OPEN);
    actionPlan.setCreatedAt(now).setUpdatedAt(now);
    return actionPlan;
  }

  public String key() {
    return key;
  }

  public ActionPlanStats setKey(String key) {
    this.key = key;
    return this;
  }

  public String name() {
    return name;
  }

  public ActionPlanStats setName(String name) {
    this.name = name;
    return this;
  }

  public String description() {
    return description;
  }

  public ActionPlanStats setDescription(String description) {
    this.description = description;
    return this;
  }

  public String userLogin() {
    return userLogin;
  }

  public ActionPlanStats setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String status() {
    return status;
  }

  public ActionPlanStats setStatus(String status) {
    this.status = status;
    return this;
  }

  public Date deadLine() {
    return deadLine;
  }

  public ActionPlanStats setDeadLine(Date deadLine) {
    this.deadLine = deadLine;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public ActionPlanStats setCreatedAt(Date creationDate) {
    this.createdAt = creationDate;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public ActionPlanStats setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public int totalIssues() {
    return totalIssues;
  }

  public ActionPlanStats setTotalIssues(int totalIssues) {
    this.totalIssues = totalIssues;
    return this;
  }

  public int openIssues() {
    return openIssues;
  }

  public ActionPlanStats setOpenIssues(int openIssues) {
    this.openIssues = openIssues;
    return this;
  }

  public int resolvedIssues() {
    return totalIssues - openIssues;
  }

  public boolean isOpen(){
    return ActionPlan.STATUS_OPEN.equals(status);
  }

  public boolean overDue(){
    return status == ActionPlan.STATUS_OPEN && new Date().after(deadLine);
  }
}
