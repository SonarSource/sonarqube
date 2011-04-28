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
  private Long[] projects;
  private Long[] resources;
  private Long[] authors;
  private Long[] assignees;
  private Boolean html;

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
   * @return the projects
   */
  public Long[] getProjects() {
    return projects;
  }

  /**
   * Specify the IDs of the projects.
   * 
   * @param projects
   *          the project IDs to set
   */
  public ReviewQuery setProjects(Long... projects) {
    this.projects = projects;
    return this;
  }

  /**
   * @return the resources
   */
  public Long[] getResources() {
    return resources;
  }

  /**
   * Specify the IDs of the resources.
   * 
   * @param resources
   *          the resource IDs to set
   */
  public ReviewQuery setResources(Long... resources) {
    this.resources = resources;
    return this;
  }

  /**
   * @return the authors
   */
  public Long[] getAuthors() {
    return authors;
  }

  /**
   * Specify the IDs of the authors.
   * 
   * @param authors
   *          the author IDs to set
   */
  public ReviewQuery setAuthors(Long... authors) {
    this.authors = authors;
    return this;
  }

  /**
   * @return the assignees
   */
  public Long[] getAssignees() {
    return assignees;
  }

  /**
   * Specify the IDs of the assignees.
   * 
   * @param assignees
   *          the assignee IDs to set
   */
  public ReviewQuery setAssignees(Long... assignees) {
    this.assignees = assignees;
    return this;
  }
  
  /**
   * @return the html
   */
  public Boolean getHtml() {
    return html;
  }
  
  /**
   * If true, the comments will be generated in HTML. Otherwise, they will be in raw text.
   * 
   * @param html the html to set
   */
  public ReviewQuery setHtml(Boolean html) {
    this.html = html;
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
    appendUrlParameter(url, "projects", projects);
    appendUrlParameter(url, "resources", resources);
    appendUrlParameter(url, "authors", authors);
    appendUrlParameter(url, "assignees", assignees);
    appendUrlParameter(url, "html", html);

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
