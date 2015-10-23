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
package it.issue;

import com.sonar.orchestrator.selenium.Selenese;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import util.selenium.SeleneseTest;

public class ManualRulesTest extends AbstractIssueTest {

  @BeforeClass
  public static void setup() throws Exception {
    ORCHESTRATOR.resetData();
    deleteManualRules();
  }

  @AfterClass
  public static void purgeManualRules() {
    deleteManualRules();
  }

  @Test
  public void testManualRules() {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("manual-rules",
        "/issue/ManualRulesTest/create_edit_delete_manual_rule.html"
      ).build();
    new SeleneseTest(selenese).runOn(ORCHESTRATOR);
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
