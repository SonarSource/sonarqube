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
package org.sonarqube.ws.client.rules;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/search">Further information about this action online (including a response example)</a>
 * @since 4.4
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private String activation;
  private List<String> activeSeverities;
  private String asc;
  private String availableSince;
  private String compareToProfile;
  private List<String> f;
  private List<String> facets;
  private List<String> inheritance;
  private String isTemplate;
  private List<String> languages;
  private String organization;
  private String p;
  private String ps;
  private String q;
  private String qprofile;
  private List<String> repositories;
  private String ruleKey;
  private String s;
  private List<String> severities;
  private List<String> statuses;
  private List<String> tags;
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
  public SearchRequest setActivation(String activation) {
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
  public SearchRequest setActiveSeverities(List<String> activeSeverities) {
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
  public SearchRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * Example value: "2014-06-22"
   */
  public SearchRequest setAvailableSince(String availableSince) {
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
  public SearchRequest setCompareToProfile(String compareToProfile) {
    this.compareToProfile = compareToProfile;
    return this;
  }

  public String getCompareToProfile() {
    return compareToProfile;
  }

  /**
   * Example value: "repo,name"
   * Possible values:
   * <ul>
   *   <li>"actives"</li>
   *   <li>"createdAt"</li>
   *   <li>"debtOverloaded"</li>
   *   <li>"debtRemFn"</li>
   *   <li>"defaultDebtRemFn"</li>
   *   <li>"defaultRemFn"</li>
   *   <li>"effortToFixDescription"</li>
   *   <li>"gapDescription"</li>
   *   <li>"htmlDesc"</li>
   *   <li>"htmlNote"</li>
   *   <li>"internalKey"</li>
   *   <li>"isTemplate"</li>
   *   <li>"lang"</li>
   *   <li>"langName"</li>
   *   <li>"mdDesc"</li>
   *   <li>"mdNote"</li>
   *   <li>"name"</li>
   *   <li>"noteLogin"</li>
   *   <li>"params"</li>
   *   <li>"remFn"</li>
   *   <li>"remFnOverloaded"</li>
   *   <li>"repo"</li>
   *   <li>"severity"</li>
   *   <li>"status"</li>
   *   <li>"sysTags"</li>
   *   <li>"tags"</li>
   *   <li>"templateKey"</li>
   * </ul>
   */
  public SearchRequest setF(List<String> f) {
    this.f = f;
    return this;
  }

  public List<String> getF() {
    return f;
  }

  /**
   * Example value: "languages,repositories"
   * Possible values:
   * <ul>
   *   <li>"languages"</li>
   *   <li>"repositories"</li>
   *   <li>"tags"</li>
   *   <li>"severities"</li>
   *   <li>"active_severities"</li>
   *   <li>"statuses"</li>
   *   <li>"types"</li>
   *   <li>"true"</li>
   * </ul>
   */
  public SearchRequest setFacets(List<String> facets) {
    this.facets = facets;
    return this;
  }

  public List<String> getFacets() {
    return facets;
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
  public SearchRequest setInheritance(List<String> inheritance) {
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
  public SearchRequest setIsTemplate(String isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  public String getIsTemplate() {
    return isTemplate;
  }

  /**
   * Example value: "java,js"
   */
  public SearchRequest setLanguages(List<String> languages) {
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
  public SearchRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "42"
   */
  public SearchRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public SearchRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "xpath"
   */
  public SearchRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public SearchRequest setQprofile(String qprofile) {
    this.qprofile = qprofile;
    return this;
  }

  public String getQprofile() {
    return qprofile;
  }

  /**
   * Example value: "checkstyle,findbugs"
   */
  public SearchRequest setRepositories(List<String> repositories) {
    this.repositories = repositories;
    return this;
  }

  public List<String> getRepositories() {
    return repositories;
  }

  /**
   * Example value: "squid:S001"
   */
  public SearchRequest setRuleKey(String ruleKey) {
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
  public SearchRequest setS(String s) {
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
  public SearchRequest setSeverities(List<String> severities) {
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
  public SearchRequest setStatuses(List<String> statuses) {
    this.statuses = statuses;
    return this;
  }

  public List<String> getStatuses() {
    return statuses;
  }

  /**
   * Example value: "security,java8"
   */
  public SearchRequest setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }

  /**
   * Example value: "java:S001"
   */
  public SearchRequest setTemplateKey(String templateKey) {
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
  public SearchRequest setTypes(List<String> types) {
    this.types = types;
    return this;
  }

  public List<String> getTypes() {
    return types;
  }
}
