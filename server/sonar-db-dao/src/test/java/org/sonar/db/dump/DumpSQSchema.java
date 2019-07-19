/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.dump;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import org.apache.commons.io.FileUtils;

public class DumpSQSchema {

  public static void main(String[] args) {
    SQSchemaDumper dumper = new SQSchemaDumper();
    try {
      File targetFile = new File("src/schema/schema-sq.ddl");
      if (!targetFile.exists()) {
        System.out.println("Can not find schema dump file: '" + targetFile + "'");
        System.exit(1);
      }

      Charset charset = Charset.forName("UTF8");
      String oldContent = FileUtils.readFileToString(targetFile, charset);
      String newContent = dumper.dumpToText();
      boolean upToDate = newContent.equals(oldContent);
      FileUtils.write(targetFile, newContent, charset);
      if (!upToDate) {
        System.err.println("SQL Schema dump file has changed. Please review and commit " + targetFile.getAbsolutePath());
        System.exit(137);
      }
    } catch (SQLException | IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

}
