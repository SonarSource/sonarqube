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
package org.sonarqube.ws.client.qualityprofiles;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/deactivate_rules">Further information about this action online (including a response example)</a>
 * @since 4.4
 */
@Generated("sonar-ws-generator")
public class DeactivateRulesRequest {

  private String activation;
  private List<String> activeSeverities;
  private String asc;
  private String availableSince;
  private String compareToProfile;
  private List<String> inheritance;
  private String isTemplate;
  private List<String> languages;
  private String organization;
  private String q;
  private String qprofile;
  private List<String> repositories;
  private String ruleKey;
  private String s;
  private List<String> severities;
  private List<String> statuses;
  private List<String> tags;
  private String targetKey;
  private String templateKey;
  private List<String> types;

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public DeactivateRulesRequest setActivation(String activation) {
    this.activation = activation;
    return this;
  }

  public String getActivation() {
    return activation;
  }

  /**
   * Example value: "CRITICAL,BLOCKER"
   * Possible values:
   * <ul>
   *   <li>"INFO"</li>
   *   <li>"MINOR"</li>
   *   <li>"MAJOR"</li>
   *   <li>"CRITICAL"</li>
   *   <li>"BLOCKER"</li>
   * </ul>
   */
  public DeactivateRulesRequest setActiveSeverities(List<String> activeSeverities) {
    this.activeSeverities = activeSeverities;
    return this;
  }

  public List<String> getActiveSeverities() {
    return activeSeverities;
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
  public DeactivateRulesRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * Example value: "2014-06-22"
   */
  public DeactivateRulesRequest setAvailableSince(String availableSince) {
    this.availableSince = availableSince;
    return this;
  }

  public String getAvailableSince() {
    return availableSince;
  }

  /**
   * This is part of the internal API.
   * Example value: "AU-TpxcA-iU5OvuD2FLz"
   */
  public DeactivateRulesRequest setCompareToProfile(String compareToProfile) {
    this.compareToProfile = compareToProfile;
    return this;
  }

  public String getCompareToProfile() {
    return compareToProfile;
  }

  /**
   * Example value: "INHERITED,OVERRIDES"
   * Possible values:
   * <ul>
   *   <li>"NONE"</li>
   *   <li>"INHERITED"</li>
   *   <li>"OVERRIDES"</li>
   * </ul>
   */
  public DeactivateRulesRequest setInheritance(List<String> inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  public List<String> getInheritance() {
    return inheritance;
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
  public DeactivateRulesRequest setIsTemplate(String isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  public String getIsTemplate() {
    return isTemplate;
  }

  /**
   * Example value: "java,js"
   */
  public DeactivateRulesRequest setLanguages(List<String> languages) {
    this.languages = languages;
    return this;
  }

  public List<String> getLanguages() {
    return languages;
  }

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public DeactivateRulesRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "xpath"
   */
  public DeactivateRulesRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public DeactivateRulesRequest setQprofile(String qprofile) {
    this.qprofile = qprofile;
    return this;
  }

  public String getQprofile() {
    return qprofile;
  }

  /**
   * Example value: "checkstyle,findbugs"
   */
  public DeactivateRulesRequest setRepositories(List<String> repositories) {
    this.repositories = repositories;
    return this;
  }

  public List<String> getRepositories() {
    return repositories;
  }

  /**
   * Example value: "squid:S001"
   */
  public DeactivateRulesRequest setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  /**
   * Example value: "name"
   * Possible values:
   * <ul>
   *   <li>"name"</li>
   *   <li>"updatedAt"</li>
   *   <li>"createdAt"</li>
   *   <li>"key"</li>
   * </ul>
   */
  public DeactivateRulesRequest setS(String s) {
    this.s = s;
    return this;
  }

  public String getS() {
    return s;
  }

  /**
   * Example value: "CRITICAL,BLOCKER"
   * Possible values:
   * <ul>
   *   <li>"INFO"</li>
   *   <li>"MINOR"</li>
   *   <li>"MAJOR"</li>
   *   <li>"CRITICAL"</li>
   *   <li>"BLOCKER"</li>
   * </ul>
   */
  public DeactivateRulesRequest setSeverities(List<String> severities) {
    this.severities = severities;
    return this;
  }

  public List<String> getSeverities() {
    return severities;
  }

  /**
   * Example value: "READY"
   * Possible values:
   * <ul>
   *   <li>"BETA"</li>
   *   <li>"DEPRECATED"</li>
   *   <li>"READY"</li>
   *   <li>"REMOVED"</li>
   * </ul>
   */
  public DeactivateRulesRequest setStatuses(List<String> statuses) {
    this.statuses = statuses;
    return this;
  }

  public List<String> getStatuses() {
    return statuses;
  }

  /**
   * Example value: "security,java8"
   */
  public DeactivateRulesRequest setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "AU-TpxcA-iU5OvuD2FL1"
   */
  public DeactivateRulesRequest setTargetKey(String targetKey) {
    this.targetKey = targetKey;
    return this;
  }

  public String getTargetKey() {
    return targetKey;
  }

  /**
   * Example value: "java:S001"
   */
  public DeactivateRulesRequest setTemplateKey(String templateKey) {
    this.templateKey = templateKey;
    return this;
  }

  public String getTemplateKey() {
    return templateKey;
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
  public DeactivateRulesRequest setTypes(List<String> types) {
    this.types = types;
    return this;
  }

  public List<String> getTypes() {
    return types;
  }
}
