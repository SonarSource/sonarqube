/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.test.ws;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.server.test.CoverageService;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CoverageShowActionTest {

  @Mock
  CoverageService coverageService;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new CoverageWs(new CoverageShowAction(coverageService)));
  }

  @Test
  public void show_coverage_for_unit_test() throws Exception {
    String fileKey = "src/Foo.java";
    when(coverageService.getHits(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(1, 1, 2, 1, 3, 0, 4, 1, 5 , 1));
    when(coverageService.getTestCases(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(4, 8, 1, 2));
    when(coverageService.getConditions(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(2, 3, 3, 2));
    when(coverageService.getCoveredConditions(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(2, 1, 3, 2));

    WsTester.TestRequest request = tester.newGetRequest("api/coverage", "show").setParam("key", fileKey).setParam("type", "UT");

    request.execute().assertJson(getClass(), "show_coverage.json");
  }

  @Test
  public void show_coverage_for_unit_test_with_from_and_to() throws Exception {
    String fileKey = "src/Foo.java";
    when(coverageService.getHits(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(1, 1, 2, 1, 3, 0, 4, 1, 5 , 1));
    when(coverageService.getTestCases(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(4, 8, 1, 2));
    when(coverageService.getConditions(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(2, 3, 3, 2));
    when(coverageService.getCoveredConditions(fileKey, CoverageService.TYPE.UT)).thenReturn(ImmutableMap.of(2, 1, 3, 2));

    WsTester.TestRequest request = tester.newGetRequest("api/coverage", "show").setParam("key", fileKey).setParam("from", "3").setParam("to", "4").setParam("type", "UT");

    request.execute().assertJson(getClass(), "show_coverage_with_from_and_to.json");
  }

  @Test
  public void show_coverage_for_integration_test() throws Exception {
    String fileKey = "src/Foo.java";
    when(coverageService.getHits(fileKey, CoverageService.TYPE.IT)).thenReturn(ImmutableMap.of(1, 1, 2, 1, 3, 0, 4, 1, 5 , 1));
    when(coverageService.getConditions(fileKey, CoverageService.TYPE.IT)).thenReturn(ImmutableMap.of(2, 3, 3, 2));
    when(coverageService.getCoveredConditions(fileKey, CoverageService.TYPE.IT)).thenReturn(ImmutableMap.of(2, 1, 3, 2));

    WsTester.TestRequest request = tester.newGetRequest("api/coverage", "show").setParam("key", fileKey).setParam("type", "IT");

    request.execute().assertJson(getClass(), "show_coverage_for_integration_test.json");
  }

  @Test
  public void show_coverage_for_overall_test() throws Exception {
    String fileKey = "src/Foo.java";
    when(coverageService.getHits(fileKey, CoverageService.TYPE.OVERALL)).thenReturn(ImmutableMap.of(1, 1, 2, 1, 3, 0, 4, 1, 5 , 1));
    when(coverageService.getConditions(fileKey, CoverageService.TYPE.OVERALL)).thenReturn(ImmutableMap.of(2, 3, 3, 2));
    when(coverageService.getCoveredConditions(fileKey, CoverageService.TYPE.OVERALL)).thenReturn(ImmutableMap.of(2, 1, 3, 2));

    WsTester.TestRequest request = tester.newGetRequest("api/coverage", "show").setParam("key", fileKey).setParam("type", "OVERALL");

    request.execute().assertJson(getClass(), "show_coverage_for_overall_test.json");
  }

}
