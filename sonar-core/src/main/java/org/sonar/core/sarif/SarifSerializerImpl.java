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
package org.sonar.core.sarif;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import javax.inject.Inject;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;

@ScannerSide
@ComputeEngineSide
public class SarifSerializerImpl implements SarifSerializer {
  private static final String SARIF_REPORT_ERROR = "Failed to read SARIF report at '%s'";
  private static final String SARIF_JSON_SYNTAX_ERROR = SARIF_REPORT_ERROR + ": invalid JSON syntax or file is not UTF-8 encoded";

  private final Gson gson;

  @Inject
  public SarifSerializerImpl() {
    this(new Gson());
  }

  @VisibleForTesting
  SarifSerializerImpl(Gson gson) {
    this.gson = gson;
  }

  @Override
  public String serialize(Sarif210 sarif210) {
    return gson.toJson(sarif210);
  }

  @Override
  public Sarif210 deserialize(Path reportPath) throws NoSuchFileException {
    try (Reader reader = newBufferedReader(reportPath, UTF_8)) {
      Sarif210 sarif = gson.fromJson(reader, Sarif210.class);
      SarifVersionValidator.validateSarifVersion(sarif.getVersion());
      return sarif;
    } catch (NoSuchFileException e) {
      throw e;
    } catch (JsonIOException | IOException e) {
      throw new IllegalStateException(format(SARIF_REPORT_ERROR, reportPath), e);
    } catch (JsonSyntaxException e) {
      throw new IllegalStateException(format(SARIF_JSON_SYNTAX_ERROR, reportPath), e);
    }
  }
}
