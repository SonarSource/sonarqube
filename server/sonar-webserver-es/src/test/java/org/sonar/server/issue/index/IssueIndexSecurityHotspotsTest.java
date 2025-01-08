/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.index;

import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.Severity;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

class IssueIndexSecurityHotspotsTest extends IssueIndexTestCommon {

  @Test
  void filter_by_security_hotspots_type() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(BUG),
      newDoc("I2", project.uuid(), file).setType(CODE_SMELL),
      newDoc("I3", project.uuid(), file).setType(VULNERABILITY),
      newDoc("I4", project.uuid(), file).setType(VULNERABILITY),
      newDoc("I5", project.uuid(), file).setType(SECURITY_HOTSPOT),
      newDoc("I6", project.uuid(), file).setType(SECURITY_HOTSPOT));

    assertThatSearchReturnsOnly(IssueQuery.builder().types(singletonList(SECURITY_HOTSPOT.name())), "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().types(asList(SECURITY_HOTSPOT.name(), VULNERABILITY.name())), "I3", "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1", "I2", "I3", "I4", "I5", "I6");
  }

  @Test
  void filter_by_severities_ignore_hotspots() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setSeverity(Severity.INFO).setType(BUG),
      newDoc("I2", project.uuid(), file).setSeverity(Severity.MAJOR).setType(CODE_SMELL),
      newDoc("I3", project.uuid(), file).setSeverity(Severity.MAJOR).setType(VULNERABILITY),
      newDoc("I4", project.uuid(), file).setSeverity(Severity.CRITICAL).setType(VULNERABILITY),
      // This entry should be ignored
      newDoc("I5", project.uuid(), file).setSeverity(Severity.MAJOR).setType(SECURITY_HOTSPOT));

    assertThatSearchReturnsOnly(IssueQuery.builder().severities(asList(Severity.INFO, Severity.MAJOR)), "I1", "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().severities(asList(Severity.INFO, Severity.MAJOR)).types(singletonList(VULNERABILITY.name())), "I3");
    assertThatSearchReturnsEmpty(IssueQuery.builder().severities(singletonList(Severity.MAJOR)).types(singletonList(SECURITY_HOTSPOT.name())));
  }

  @Test
  void facet_on_severities_ignore_hotspots() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setSeverity(INFO).setType(BUG),
      newDoc("I2", project.uuid(), file).setSeverity(INFO).setType(CODE_SMELL),
      newDoc("I3", project.uuid(), file).setSeverity(INFO).setType(VULNERABILITY),
      newDoc("I4", project.uuid(), file).setSeverity(MAJOR).setType(VULNERABILITY),
      // These 2 entries should be ignored
      newDoc("I5", project.uuid(), file).setSeverity(INFO).setType(SECURITY_HOTSPOT),
      newDoc("I6", project.uuid(), file).setSeverity(MAJOR).setType(SECURITY_HOTSPOT));

    assertThatFacetHasOnly(IssueQuery.builder(), "severities", entry("INFO", 3L), entry("MAJOR", 1L));
    assertThatFacetHasOnly(IssueQuery.builder().types(singletonList(VULNERABILITY.name())), "severities", entry("INFO", 1L), entry("MAJOR", 1L));
    assertThatFacetHasOnly(IssueQuery.builder().types(asList(BUG.name(), CODE_SMELL.name(), VULNERABILITY.name())), "severities", entry("INFO", 3L), entry("MAJOR", 1L));
    assertThatFacetHasOnly(IssueQuery.builder().types(singletonList(SECURITY_HOTSPOT.name())), "severities");
  }

  @SafeVarargs
  private final void assertThatFacetHasOnly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone().toZoneId());
    assertThat(facets.getNames()).containsOnly(facet, "effort");
    assertThat(facets.get(facet)).containsOnly(expectedEntries);
  }

}
