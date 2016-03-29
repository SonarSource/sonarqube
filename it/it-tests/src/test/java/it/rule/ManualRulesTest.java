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
package it.rule;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category2Suite;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import util.QaOnly;
import util.selenium.SeleneseTest;

@Category(QaOnly.class)
@Ignore("will be removed with MMF-233")
public class ManualRulesTest {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Category2Suite.ORCHESTRATOR;

  @Before
  public void setup() throws Exception {
    ORCHESTRATOR.resetData();
    deleteManualRules();
  }

  @AfterClass
  public static void purgeManualRules() {
    deleteManualRules();
  }

  @Test
  public void manual_rules() {
    new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("manual-rules",
          "/rule/ManualRulesTest/create_edit_delete_manual_rule.html"
        ).build()
    ).runOn(ORCHESTRATOR);
  }

  protected static void deleteManualRules() {
    try {
      Connection connection = ORCHESTRATOR.getDatabase().openConnection();
      connection.prepareStatement("DELETE FROM rules WHERE rules.plugin_name='manual'").execute();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to remove manual rules", e);
    }
  }

}
