/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarqube.ws.client.issue;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchWsRequestTest {
  private static final ImmutableList<String> LIST_OF_STRINGS = ImmutableList.of("A", "B");
  private static final String SOME_STRING = "some string";
  public static final int SOME_INT = 894352;

  private SearchWsRequest underTest = new SearchWsRequest();

  @Test
  public void getActionPlans_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getActionPlans()).isNull();
  }

  @Test
  public void setActionPlans_accepts_null() {
    underTest.setActionPlans(null);
  }

  @Test
  public void getActionPlans_returns_object_from_setActionPlans() {
    underTest.setActionPlans(LIST_OF_STRINGS);

    assertThat(underTest.getActionPlans()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getAdditionalFields_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getAdditionalFields()).isNull();
  }

  @Test
  public void setAdditionalFields_accepts_null() {
    underTest.setAdditionalFields(null);
  }

  @Test
  public void getAdditionalFields_returns_object_from_setAdditionalFields() {
    underTest.setAdditionalFields(LIST_OF_STRINGS);
    assertThat(underTest.getAdditionalFields()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getAssignees_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getAssignees()).isNull();
  }

  @Test
  public void setAssignees_accepts_null() {
    underTest.setAssignees(null);
  }

  @Test
  public void getAssignees_returns_object_from_setAssignees() {
    underTest.setAssignees(LIST_OF_STRINGS);
    assertThat(underTest.getAssignees()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getAuthors_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getAuthors()).isNull();
  }

  @Test
  public void setAuthors_accepts_null() {
    underTest.setAuthors(null);
  }

  @Test
  public void getAuthors_returns_object_from_setAuthors() {
    underTest.setAuthors(LIST_OF_STRINGS);
    assertThat(underTest.getAuthors()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getComponentKeys_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getComponentKeys()).isNull();
  }

  @Test
  public void setComponentKeys_accepts_null() {
    underTest.setComponentKeys(null);
  }

  @Test
  public void getComponentKeys_returns_object_from_setComponentKeys() {
    underTest.setComponentKeys(LIST_OF_STRINGS);
    assertThat(underTest.getComponentKeys()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getComponentRootUuids_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getComponentRootUuids()).isNull();
  }

  @Test
  public void setComponentRootUuids_accepts_null() {
    underTest.setComponentRootUuids(null);
  }

  @Test
  public void getComponentRootUuids_returns_object_from_setComponentRootUuids() {
    underTest.setComponentRootUuids(LIST_OF_STRINGS);
    assertThat(underTest.getComponentRootUuids()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getComponentRoots_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getComponentRoots()).isNull();
  }

  @Test
  public void setComponentRoots_accepts_null() {
    underTest.setComponentRoots(null);
  }

  @Test
  public void getComponentRoots_returns_object_from_setComponentRoots() {
    underTest.setComponentRoots(LIST_OF_STRINGS);
    assertThat(underTest.getComponentRoots()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getComponentUuids_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getComponentUuids()).isNull();
  }

  @Test
  public void setComponentUuids_accepts_null() {
    underTest.setComponentUuids(null);
  }

  @Test
  public void getComponentUuids_returns_object_from_setComponentUuids() {
    underTest.setComponentUuids(LIST_OF_STRINGS);
    assertThat(underTest.getComponentUuids()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getComponents_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getComponents()).isNull();
  }

  @Test
  public void setComponents_accepts_null() {
    underTest.setComponents(null);
  }

  @Test
  public void getComponents_returns_object_from_setComponents() {
    underTest.setComponents(LIST_OF_STRINGS);
    assertThat(underTest.getComponents()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getDirectories_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getDirectories()).isNull();
  }

  @Test
  public void setDirectories_accepts_null() {
    underTest.setDirectories(null);
  }

  @Test
  public void getDirectories_returns_object_from_setDirectories() {
    underTest.setDirectories(LIST_OF_STRINGS);
    assertThat(underTest.getDirectories()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getFacets_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getFacets()).isNull();
  }

  @Test
  public void setFacets_accepts_null() {
    underTest.setFacets(null);
  }

  @Test
  public void getFacets_returns_object_from_setFacets() {
    underTest.setFacets(LIST_OF_STRINGS);
    assertThat(underTest.getFacets()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getFileUuids_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getFileUuids()).isNull();
  }

  @Test
  public void setFileUuids_accepts_null() {
    underTest.setFileUuids(null);
  }

  @Test
  public void getFileUuids_returns_object_from_setFileUuids() {
    underTest.setFileUuids(LIST_OF_STRINGS);
    assertThat(underTest.getFileUuids()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getIssues_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getIssues()).isNull();
  }

  @Test
  public void setIssues_accepts_null() {
    underTest.setIssues(null);
  }

  @Test
  public void getIssues_returns_object_from_setIssues() {
    underTest.setIssues(LIST_OF_STRINGS);
    assertThat(underTest.getIssues()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getLanguages_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getLanguages()).isNull();
  }

  @Test
  public void setLanguages_accepts_null() {
    underTest.setLanguages(null);
  }

  @Test
  public void getLanguages_returns_object_from_setLanguages() {
    underTest.setLanguages(LIST_OF_STRINGS);
    assertThat(underTest.getLanguages()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getModuleUuids_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getModuleUuids()).isNull();
  }

  @Test
  public void setModuleUuids_accepts_null() {
    underTest.setModuleUuids(null);
  }

  @Test
  public void getModuleUuids_returns_object_from_setModuleUuids() {
    underTest.setModuleUuids(LIST_OF_STRINGS);
    assertThat(underTest.getModuleUuids()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getProjectKeys_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getProjectKeys()).isNull();
  }

  @Test
  public void setProjectKeys_accepts_null() {
    underTest.setProjectKeys(null);
  }

  @Test
  public void getProjectKeys_returns_object_from_setProjectKeys() {
    underTest.setProjectKeys(LIST_OF_STRINGS);
    assertThat(underTest.getProjectKeys()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getProjectUuids_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getProjectUuids()).isNull();
  }

  @Test
  public void setProjectUuids_accepts_null() {
    underTest.setProjectUuids(null);
  }

  @Test
  public void getProjectUuids_returns_object_from_setProjectUuids() {
    underTest.setProjectUuids(LIST_OF_STRINGS);
    assertThat(underTest.getProjectUuids()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getProjects_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getProjects()).isNull();
  }

  @Test
  public void setProjects_accepts_null() {
    underTest.setProjects(null);
  }

  @Test
  public void getProjects_returns_object_from_setProjects() {
    underTest.setProjects(LIST_OF_STRINGS);
    assertThat(underTest.getProjects()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getResolutions_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getResolutions()).isNull();
  }

  @Test
  public void setResolutions_accepts_null() {
    underTest.setResolutions(null);
  }

  @Test
  public void getResolutions_returns_object_from_setResolutions() {
    underTest.setResolutions(LIST_OF_STRINGS);
    assertThat(underTest.getResolutions()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getRules_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getRules()).isNull();
  }

  @Test
  public void setRules_accepts_null() {
    underTest.setRules(null);
  }

  @Test
  public void getRules_returns_object_from_setRules() {
    underTest.setRules(LIST_OF_STRINGS);
    assertThat(underTest.getRules()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getSeverities_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getSeverities()).isNull();
  }

  @Test
  public void setSeverities_accepts_null() {
    underTest.setSeverities(null);
  }

  @Test
  public void getSeverities_returns_object_from_setSeverities() {
    underTest.setSeverities(LIST_OF_STRINGS);
    assertThat(underTest.getSeverities()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getStatuses_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getStatuses()).isNull();
  }

  @Test
  public void setStatuses_accepts_null() {
    underTest.setStatuses(null);
  }

  @Test
  public void getStatuses_returns_object_from_setStatuses() {
    underTest.setStatuses(LIST_OF_STRINGS);
    assertThat(underTest.getStatuses()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getTags_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getTags()).isNull();
  }

  @Test
  public void setTags_accepts_null() {
    underTest.setTags(null);
  }

  @Test
  public void getTags_returns_object_from_setTags() {
    underTest.setTags(LIST_OF_STRINGS);
    assertThat(underTest.getTags()).isSameAs(LIST_OF_STRINGS);
  }

  @Test
  public void getAsc_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getAsc()).isNull();
  }

  @Test
  public void getAsc_returns_boolean_from_setTags() {
    underTest.setAsc(true);
    assertThat(underTest.getAsc()).isTrue();
    underTest.setAsc(false);
    assertThat(underTest.getAsc()).isFalse();
  }

  @Test
  public void getAssigned_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getAssigned()).isNull();
  }

  @Test
  public void setAssigned_accepts_null() {
    underTest.setAssigned(null);
  }

  @Test
  public void getAssigned_returns_boolean_from_setTags() {
    underTest.setAssigned(true);
    assertThat(underTest.getAssigned()).isTrue();
    underTest.setAssigned(false);
    assertThat(underTest.getAssigned()).isFalse();
  }

  @Test
  public void getOnComponentOnly_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getOnComponentOnly()).isNull();
  }

  @Test
  public void setOnComponentOnly_accepts_null() {
    underTest.setOnComponentOnly(null);
  }

  @Test
  public void getOnComponentOnly_returns_boolean_from_setOnComponentOnly() {
    underTest.setOnComponentOnly(true);
    assertThat(underTest.getOnComponentOnly()).isTrue();
    underTest.setOnComponentOnly(false);
    assertThat(underTest.getOnComponentOnly()).isFalse();
  }

  @Test
  public void getResolved_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getResolved()).isNull();
  }

  @Test
  public void setResolved_accepts_null() {
    underTest.setResolved(null);
  }

  @Test
  public void getResolved_returns_boolean_from_setResolved() {
    underTest.setResolved(true);
    assertThat(underTest.getResolved()).isTrue();
    underTest.setResolved(false);
    assertThat(underTest.getResolved()).isFalse();
  }

  @Test
  public void getCreatedAfter_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getCreatedAfter()).isNull();
  }

  @Test
  public void setCreatedAfter_accepts_null() {
    underTest.setCreatedAfter(null);
  }

  @Test
  public void getCreatedAfter_returns_object_from_setCreatedAfter() {
    underTest.setCreatedAfter(SOME_STRING);
    assertThat(underTest.getCreatedAfter()).isEqualTo(SOME_STRING);
  }

  @Test
  public void getCreatedAt_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getCreatedAt()).isNull();
  }

  @Test
  public void setCreatedAt_accepts_null() {
    underTest.setCreatedAt(null);
  }

  @Test
  public void getCreatedAt_returns_object_from_setCreatedAt() {
    underTest.setCreatedAt(SOME_STRING);
    assertThat(underTest.getCreatedAt()).isEqualTo(SOME_STRING);
  }

  @Test
  public void getCreatedBefore_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getCreatedBefore()).isNull();
  }

  @Test
  public void setCreatedBefore_accepts_null() {
    underTest.setCreatedBefore(null);
  }

  @Test
  public void getCreatedBefore_returns_object_from_setCreatedBefore() {
    underTest.setCreatedBefore(SOME_STRING);
    assertThat(underTest.getCreatedBefore()).isEqualTo(SOME_STRING);
  }

  @Test
  public void getCreatedInLast_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getCreatedInLast()).isNull();
  }

  @Test
  public void setCreatedInLast_accepts_null() {
    underTest.setCreatedInLast(null);
  }

  @Test
  public void getCreatedInLast_returns_object_from_setCreatedInLast() {
    underTest.setCreatedInLast(SOME_STRING);
    assertThat(underTest.getCreatedInLast()).isEqualTo(SOME_STRING);
  }

  @Test
  public void getFacetMode_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getFacetMode()).isNull();
  }

  @Test
  public void setFacetMode_accepts_null() {
    underTest.setFacetMode(null);
  }

  @Test
  public void getFacetMode_returns_object_from_setFacetMode() {
    underTest.setFacetMode(SOME_STRING);
    assertThat(underTest.getFacetMode()).isEqualTo(SOME_STRING);
  }

  @Test
  public void getSort_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getSort()).isNull();
  }

  @Test
  public void setSort_accepts_null() {
    underTest.setSort(null);
  }

  @Test
  public void getSort_returns_object_from_setSort() {
    underTest.setSort(SOME_STRING);
    assertThat(underTest.getSort()).isEqualTo(SOME_STRING);
  }

  @Test
  public void getPage_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getPage()).isNull();
  }

  @Test
  public void getPage_returns_object_from_setPage() {
    underTest.setPage(SOME_INT);
    assertThat(underTest.getPage()).isEqualTo(SOME_INT);
  }

  @Test
  public void getPageSize_returns_null_when_SearchWsRequest_has_just_been_instantiated() {
    assertThat(underTest.getPageSize()).isNull();
  }

  @Test
  public void getPageSize_returns_object_from_setPageSize() {
    underTest.setPageSize(SOME_INT);
    assertThat(underTest.getPageSize()).isEqualTo(SOME_INT);
  }

}
