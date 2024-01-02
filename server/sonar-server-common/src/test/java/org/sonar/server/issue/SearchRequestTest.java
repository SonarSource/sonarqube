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
package org.sonar.server.issue;

import java.util.List;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class SearchRequestTest {

  @Test
  public void settersAndGetters() {
    SearchRequest underTest = new SearchRequest()
      .setIssues(singletonList("anIssueKey"))
      .setSeverities(asList("MAJOR", "MINOR"))
      .setStatuses(singletonList("CLOSED"))
      .setResolutions(singletonList("FALSE-POSITIVE"))
      .setResolved(true)
      .setProjectKeys(singletonList("project-a"))
      .setDirectories(singletonList("aDirPath"))
      .setFiles(asList("file-a", "file-b"))
      .setAssigneesUuid(asList("user-a", "user-b"))
      .setScopes(asList("MAIN", "TEST"))
      .setLanguages(singletonList("xoo"))
      .setTags(asList("tag1", "tag2"))
      .setAssigned(true)
      .setCreatedAfter("2013-04-16T09:08:24+0200")
      .setCreatedBefore("2013-04-17T09:08:24+0200")
      .setRules(asList("key-a", "key-b"))
      .setSort("CREATION_DATE")
      .setAsc(true)
      .setInNewCodePeriod(true)
      .setOwaspTop10For2021(asList("a2", "a3"))
      .setOwaspAsvs40(asList("1.1.1", "4.2.2"))
      .setOwaspAsvsLevel(2)
      .setPciDss32(asList("1", "4"))
      .setPciDss40(asList("3", "5"))
      .setCodeVariants(asList("variant1", "variant2"))
      .setCleanCodeAttributesCategories(singletonList("ADAPTABLE"))
      .setImpactSeverities(List.of("HIGH", "LOW"))
      .setImpactSoftwareQualities(List.of("RELIABILITY", "SECURITY"));

    assertThat(underTest.getIssues()).containsOnlyOnce("anIssueKey");
    assertThat(underTest.getSeverities()).containsExactly("MAJOR", "MINOR");
    assertThat(underTest.getStatuses()).containsExactly("CLOSED");
    assertThat(underTest.getResolutions()).containsExactly("FALSE-POSITIVE");
    assertThat(underTest.getResolved()).isTrue();
    assertThat(underTest.getProjectKeys()).containsExactly("project-a");
    assertThat(underTest.getDirectories()).containsExactly("aDirPath");
    assertThat(underTest.getFiles()).containsExactly("file-a", "file-b");
    assertThat(underTest.getAssigneeUuids()).containsExactly("user-a", "user-b");
    assertThat(underTest.getScopes()).containsExactly("MAIN", "TEST");
    assertThat(underTest.getLanguages()).containsExactly("xoo");
    assertThat(underTest.getTags()).containsExactly("tag1", "tag2");
    assertThat(underTest.getAssigned()).isTrue();
    assertThat(underTest.getCreatedAfter()).isEqualTo("2013-04-16T09:08:24+0200");
    assertThat(underTest.getCreatedBefore()).isEqualTo("2013-04-17T09:08:24+0200");
    assertThat(underTest.getRules()).containsExactly("key-a", "key-b");
    assertThat(underTest.getSort()).isEqualTo("CREATION_DATE");
    assertThat(underTest.getAsc()).isTrue();
    assertThat(underTest.getInNewCodePeriod()).isTrue();
    assertOwasp(underTest);
    assertThat(underTest.getPciDss32()).containsExactly("1", "4");
    assertThat(underTest.getPciDss40()).containsExactly("3", "5");
    assertThat(underTest.getCodeVariants()).containsExactly("variant1", "variant2");
    assertCleanCodeInformation(underTest);
  }

  private static void assertOwasp(SearchRequest underTest) {
    assertThat(underTest.getOwaspTop10For2021()).containsExactly("a2", "a3");
    assertThat(underTest.getOwaspAsvs40()).containsExactly("1.1.1", "4.2.2");
    assertThat(underTest.getOwaspAsvsLevel()).isEqualTo(2);
  }

  private static void assertCleanCodeInformation(SearchRequest underTest) {
    assertThat(underTest.getCleanCodeAttributesCategories()).containsExactly("ADAPTABLE");
    assertThat(underTest.getImpactSeverities()).containsExactly("HIGH", "LOW");
    assertThat(underTest.getImpactSoftwareQualities()).containsExactly("RELIABILITY", "SECURITY");
  }

  @Test
  public void setScopesAcceptsNull() {
    SearchRequest underTest = new SearchRequest().setScopes(null);

    assertThat(underTest.getScopes()).isNull();
  }
}
