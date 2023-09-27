/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.externalissue;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sonar.api.scanner.ScannerSide;

@ScannerSide
public class ExternalIssueReportParser {
  private final Gson gson = new Gson();
  private final ExternalIssueReportValidator externalIssueReportValidator;

  public ExternalIssueReportParser(ExternalIssueReportValidator externalIssueReportValidator) {
    this.externalIssueReportValidator = externalIssueReportValidator;
  }

  public ExternalIssueReport parse(Path reportPath) {
    try (Reader reader = Files.newBufferedReader(reportPath, StandardCharsets.UTF_8)) {
      ExternalIssueReport report = gson.fromJson(reader, ExternalIssueReport.class);
      externalIssueReportValidator.validate(report, reportPath);
      return report;
    } catch (JsonIOException | IOException e) {
      throw new IllegalStateException("Failed to read external issues report '" + reportPath + "'", e);
    } catch (JsonSyntaxException e) {
      throw new IllegalStateException("Failed to read external issues report '" + reportPath + "': invalid JSON syntax", e);
    }
  }


}
