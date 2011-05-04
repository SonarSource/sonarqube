/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

/**
 * @since 2.8
 */
public class ReviewQuery extends Query<Review> {

  public static final String BASE_URL = "/api/reviews";

  private String reviewType;
  private Long id;
  private Long[] ids;
  private String[] statuses;
  private String[] severities;
  private String[] projectKeysOrIds;
  private String[] resourceKeysOrIds;
  private String[] authorLoginsOrIds;
  private String[] assigneeLoginsOrIds;
  private String output;

  public ReviewQuery() {
  }

  /**
   * @return the reviewType
   */
  public String getReviewType() {
    return reviewType;
  }

  /**
   * @param reviewType
   *          the reviewType to set
   */
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
   * @param projectKeysOrIds the projectKeysOrIds to set
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
   * @param resourceKeysOrIds the resourceKeysOrIds to set
   */
  public ReviewQuery setResourceKeysOrIds(String... resourceKeysOrIds) {
    this.resourceKeysOrIds = resourceKeysOrIds;
    return this;
  }

  
  /**
   * @return the authorLoginsOrIds
   */
  public String[] getAuthorLoginsOrIds() {
    return authorLoginsOrIds;
  }

  
  /**
   * @param authorLoginsOrIds the authorLoginsOrIds to set
   */
  public ReviewQuery setAuthorLoginsOrIds(String... authorLoginsOrIds) {
    this.authorLoginsOrIds = authorLoginsOrIds;
    return this;
  }

  
  /**
   * @return the assigneeLoginsOrIds
   */
  public String[] getAssigneeLoginsOrIds() {
    return assigneeLoginsOrIds;
  }

  
  /**
   * @param assigneeLoginsOrIds the assigneeLoginsOrIds to set
   */
  public ReviewQuery setAssigneeLoginsOrIds(String... assigneeLoginsOrIds) {
    this.assigneeLoginsOrIds = assigneeLoginsOrIds;
    return this;
  }

  /**
   * @return the output
   */
  public String getOutput() {
    return output;
  }

  /**
   * @param output the output to set
   */
  public ReviewQuery setOutput(String output) {
    this.output = output;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    if (id != null) {
      appendUrlParameter(url, "id", id);
    } else if (ids != null) {
      appendUrlParameter(url, "ids", ids);
    }
    appendUrlParameter(url, "review_type", reviewType);
    appendUrlParameter(url, "statuses", statuses);
    appendUrlParameter(url, "severities", severities);
    appendUrlParameter(url, "projects", projectKeysOrIds);
    appendUrlParameter(url, "resources", resourceKeysOrIds);
    appendUrlParameter(url, "authors", authorLoginsOrIds);
    appendUrlParameter(url, "assignees", assigneeLoginsOrIds);
    appendUrlParameter(url, "output", output);

    return url.toString();
  }

  @Override
  public Class<Review> getModelClass() {
    return Review.class;
  }

  public static ReviewQuery createForResource(Resource resource) {
    return new ReviewQuery().setId(new Long(resource.getId()));
  }

}
