/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
  private List<String> cwe;
  private List<String> inheritance;
  private String isTemplate;
  private List<String> languages;
  private List<String> owaspTop10;
  private String q;
  private String qprofile;
  private List<String> repositories;
  private String ruleKey;
  private String s;
  private List<String> sansTop25;
  private List<String> severities;
  private List<String> sonarsourceSecurity;
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
   * Example value: "12,125,unknown"
   */
  public DeactivateRulesRequest setCwe(List<String> cwe) {
    this.cwe = cwe;
    return this;
  }

  public List<String> getCwe() {
    return cwe;
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
   * Possible values:
   * <ul>
   *   <li>"a1"</li>
   *   <li>"a2"</li>
   *   <li>"a3"</li>
   *   <li>"a4"</li>
   *   <li>"a5"</li>
   *   <li>"a6"</li>
   *   <li>"a7"</li>
   *   <li>"a8"</li>
   *   <li>"a9"</li>
   *   <li>"a10"</li>
   * </ul>
   */
  public DeactivateRulesRequest setOwaspTop10(List<String> owaspTop10) {
    this.owaspTop10 = owaspTop10;
    return this;
  }

  public List<String> getOwaspTop10() {
    return owaspTop10;
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
   * Example value: "java:S001"
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
   * Possible values:
   * <ul>
   *   <li>"insecure-interaction"</li>
   *   <li>"risky-resource"</li>
   *   <li>"porous-defenses"</li>
   * </ul>
   */
  public DeactivateRulesRequest setSansTop25(List<String> sansTop25) {
    this.sansTop25 = sansTop25;
    return this;
  }

  public List<String> getSansTop25() {
    return sansTop25;
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
   * Example value: "sql-injection,command-injection,others"
   * Possible values:
   * <ul>
   *   <li>"sql-injection"</li>
   *   <li>"command-injection"</li>
   *   <li>"path-traversal-injection"</li>
   *   <li>"ldap-injection"</li>
   *   <li>"xpath-injection"</li>
   *   <li>"rce"</li>
   *   <li>"dos"</li>
   *   <li>"ssrf"</li>
   *   <li>"csrf"</li>
   *   <li>"xss"</li>
   *   <li>"log-injection"</li>
   *   <li>"http-response-splitting"</li>
   *   <li>"open-redirect"</li>
   *   <li>"xxe"</li>
   *   <li>"object-injection"</li>
   *   <li>"weak-cryptography"</li>
   *   <li>"auth"</li>
   *   <li>"insecure-conf"</li>
   *   <li>"file-manipulation"</li>
   *   <li>"others"</li>
   * </ul>
   */
  public DeactivateRulesRequest setSonarsourceSecurity(List<String> sonarsourceSecurity) {
    this.sonarsourceSecurity = sonarsourceSecurity;
    return this;
  }

  public List<String> getSonarsourceSecurity() {
    return sonarsourceSecurity;
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
   *   <li>"SECURITY_HOTSPOT"</li>
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
