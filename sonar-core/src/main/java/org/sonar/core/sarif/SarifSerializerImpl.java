/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import jakarta.inject.Inject;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.sarif.pojo.SarifSchema210;

import static java.lang.String.format;

@ScannerSide
@ComputeEngineSide
public class SarifSerializerImpl implements SarifSerializer {
  private static final String SARIF_REPORT_ERROR = "Failed to read SARIF report at '%s'";
  private static final String SARIF_JSON_SYNTAX_ERROR = SARIF_REPORT_ERROR + ": invalid JSON syntax or file is not UTF-8 encoded";
  public static final String UNSUPPORTED_VERSION_MESSAGE_TEMPLATE = "Version [%s] of SARIF is not supported";

  private final ObjectMapper mapper;

  @Inject
  public SarifSerializerImpl() {
    this(new ObjectMapper());
  }

  @VisibleForTesting
  SarifSerializerImpl(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public String serialize(SarifSchema210 sarif210) {
    try {
      return mapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(sarif210);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to serialize SARIF", e);
    }
  }

  @Override
  public SarifSchema210 deserialize(Path reportPath) {
    try {
      return mapper
        .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
        .addHandler(new DeserializationProblemHandler() {
          @Override
          public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass, Object argument, Throwable t) throws IOException {
            if (!instClass.equals(SarifSchema210.Version.class)) {
              return NOT_HANDLED;
            }
            throw new UnsupportedSarifVersionException(format(UNSUPPORTED_VERSION_MESSAGE_TEMPLATE, argument), t);
          }
        })
        .readValue(reportPath.toFile(), SarifSchema210.class);
    } catch (UnsupportedSarifVersionException e) {
      throw new IllegalStateException(e.getMessage(), e);
    } catch (JsonMappingException | JsonParseException e) {
      throw new IllegalStateException(format(SARIF_JSON_SYNTAX_ERROR, reportPath), e);
    } catch (IOException e) {
      throw new IllegalStateException(format(SARIF_REPORT_ERROR, reportPath), e);
    }
  }

  private static class UnsupportedSarifVersionException extends IOException {

    public UnsupportedSarifVersionException(String message, Throwable t) {
      super(message, t);
    }
  }
}
