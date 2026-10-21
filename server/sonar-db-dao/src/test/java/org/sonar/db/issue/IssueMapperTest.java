/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.issue;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnIssuesFromSonarQubeUpdate.ORACLE_INDEX_EXPRESSION;

class IssueMapperTest {

  @Test
  void resetFlagFromSonarQubeUpdate_onOracle_shouldMatchIndexExpression() throws IOException {
    Configuration configuration = new Configuration();
    configuration.setDatabaseId(Oracle.ID);
    Properties variables = new Properties();
    variables.setProperty("_false", "0");

    try (Reader mapper = Resources.getResourceAsReader("org/sonar/db/issue/IssueMapper.xml")) {
      XPathParser parser = new XPathParser(mapper, false, variables, new XMLMapperEntityResolver());
      var update = parser.evalNode("/mapper/update[@id='resetFlagFromSonarQubeUpdate']");
      var sqlSource = new XMLLanguageDriver().createSqlSource(configuration, update, Map.class);

      assertThat(sqlSource.getBoundSql(Map.of("now", 0L)).getSql())
        .isEqualToNormalizingWhitespace("UPDATE issues SET from_sonarqube_update = 0, updated_at = ? WHERE " + ORACLE_INDEX_EXPRESSION + " = 1");
    }
  }
}
