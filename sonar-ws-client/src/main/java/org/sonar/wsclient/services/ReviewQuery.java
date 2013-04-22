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
package org.sonar.wsclient.services;

/**
 * @since 2.8
 */
public class ReviewQuery extends Query<Review> {

  public static final String BASE_URL = "/api/reviews";

  public static final String OUTPUT_PLAIN = "PLAIN";
  public static final String OUTPUT_HTML = "HTML";

  /**
   * @deprecated since 2.9, but kept for backward compatibility
   */
  @Deprecated
  private String reviewType;
  private Long id;
  private Long[] ids;
  private String[] statuses;
  private String[] severities;
  private String[] projectKeysOrIds;
  private String[] resourceKeysOrIds;
  private String[] authorLogins;
  private String[] assigneeLogins;
  private String output;
  private String[] resolutions;

  public ReviewQuery() {
  }

  /**
   * @deprecated since 2.9
   * @return NULL
   */
  @Deprecated
  public String getReviewType() {
    return reviewType;
  }

  /**
   * @deprecated since 2.9
   * @param reviewType
   *          the reviewType to set
   */
  @Deprecated
  public ReviewQuery setReviewType(String reviewType) {
    this.reviewType = reviewType;
    return this;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public ReviewQuery setId(Long id) {
    this.id = id;
    return this;
  }

  /**
   * @return the ids
   */
  public Long[] getIds() {
    return ids;
  }

  /**
   * @param ids
   *          the ids to set
   */
  public ReviewQuery setIds(Long... ids) {
    this.ids = ids;
    return this;
  }

  /**
   * @return the statuses
   */
  public String[] getStatuses() {
    return statuses;
  }

  /**
   * @param statuses
   *          the statuses to set
   */
  public ReviewQuery setStatuses(String... statuses) {
    this.statuses = statuses;
    return this;
  }

  /**
   * @return the severities
   */
  public String[] getSeverities() {
    return severities;
  }

  /**
   * @param severities
   *          the severities to set
   */
  public ReviewQuery setSeverities(String... severities) {
    this.severities = severities;
    return this;
  }

  /**
   * @return the projectKeysOrIds
   */
  public String[] getProjectKeysOrIds() {
    return projectKeysOrIds;
  }

  /**
   * @param projectKeysOrIds
   *          the projectKeysOrIds to set
   */
  public ReviewQuery setProjectKeysOrIds(String... projectKeysOrIds) {
    this.projectKeysOrIds = projectKeysOrIds;
    return this;
  }

  /**
   * @return the resourceKeysOrIds
   */
  public String[] getResourceKeysOrIds() {
    return resourceKeysOrIds;
  }

  /**
   * @param resourceKeysOrIds
   *          the resourceKeysOrIds to set
   */
  public ReviewQuery setResourceKeysOrIds(String... resourceKeysOrIds) {
    this.resourceKeysOrIds = resourceKeysOrIds;
    return this;
  }

  /**
   * @deprecated since 3.0. Searching by user ID is not possible anymore. Use {@link #getAuthorLogins()} instead.
   */
  @Deprecated
  public String[] getAuthorLoginsOrIds() {
    return authorLogins;
  }

  /**
   * @deprecated since 3.0. Searching by user ID is not possible anymore. Use {@link #setAuthorLogins(String...)} instead.
   */
  @Deprecated
  public ReviewQuery setAuthorLoginsOrIds(String... authorLoginsOrIds) {
    setAuthorLogins(authorLoginsOrIds);
    return this;
  }

  /**
   * @return the authorLogins
   */
  public String[] getAuthorLogins() {
    return authorLogins;
  }

  /**
   * @param authorLogins
   *          the authorLogins to set
   */
  public ReviewQuery setAuthorLogins(String... authorLogins) {
    this.authorLogins = authorLogins;
    return this;
  }

  /**
   * @deprecated since 3.0. Searching by user ID is not possible anymore. Use {@link #getAssigneeLogins()} instead.
   */
  @Deprecated
  public String[] getAssigneeLoginsOrIds() {
    return assigneeLogins;
  }

  /**
   * @deprecated since 3.0. Searching by user ID is not possible anymore. Use {@link #setAssigneeLogins(String...)} instead.
   */
  @Deprecated
  public ReviewQuery setAssigneeLoginsOrIds(String... assigneeLoginsOrIds) {
    setAssigneeLogins(assigneeLoginsOrIds);
    return this;
  }

  /**
   * @return the assigneeLogins
   */
  public String[] getAssigneeLogins() {
    return assigneeLogins;
  }

  /**
   * @param assigneeLogins
   *          the assigneeLogins to set
   */
  public ReviewQuery setAssigneeLogins(String... assigneeLogins) {
    this.assigneeLogins = assigneeLogins;
    return this;
  }

  /**
   * @return the output
   */
  public String getOutput() {
    return output;
  }

  /**
   * 
   * @param output
   *          the output
   */
  public ReviewQuery setOutput(String output) {
    this.output = output;
    return this;
  }

  /**
   * @since 2.9
   */
  public String[] getResolutions() {
    return resolutions;
  }

  /**
   * @since 2.9
   */
  public ReviewQuery setResolutions(String... resolutions) {
    this.resolutions = resolutions;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    if (id != null) {
      appendUrlParameter(url, "ids", id);
    } else if (ids != null) {
      appendUrlParameter(url, "ids", ids);
    }
    appendUrlParameter(url, "statuses", statuses);
    appendUrlParameter(url, "severities", severities);
    appendUrlParameter(url, "projects", projectKeysOrIds);
    appendUrlParameter(url, "resources", resourceKeysOrIds);
    appendUrlParameter(url, "authors", authorLogins);
    appendUrlParameter(url, "assignees", assigneeLogins);
    appendUrlParameter(url, "output", output);
    appendUrlParameter(url, "resolutions", resolutions);
    if (resolutions == null && reviewType != null) {
      // Use of the 2.8 deprecated API: handle backward compatibility
      appendUrlParameter(url, "review_type", reviewType);
    }

    return url.toString();
  }

  @Override
  public Class<Review> getModelClass() {
    return Review.class;
  }

  public static ReviewQuery createForReview(Long id) {
    return new ReviewQuery().setId(id);
  }

  public static ReviewQuery createForResource(Resource resource) {
    return new ReviewQuery().setResourceKeysOrIds(resource.getId().toString());
  }

}
