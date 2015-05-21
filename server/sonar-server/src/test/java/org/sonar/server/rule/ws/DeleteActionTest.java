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

package org.sonar.server.rule.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.rule.RuleService;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeleteActionTest {

  WsTester tester;

  @Mock
  RuleService ruleService;

  @Before
  public void setUp() {
    tester = new WsTester(new RulesWs(new DeleteAction(ruleService)));
  }

  @Test
  public void delete_custom_rule() throws Exception {
    WsTester.TestRequest request = tester.newPostRequest("api/rules", "delete").setParam("key", "squid:XPath_1402065390816");
    request.execute();

    verify(ruleService).delete(RuleKey.of("squid", "XPath_1402065390816"));
  }
}
