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
import jakarta.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.sarif.SarifDeserializationException.Category;
import org.sonar.sarif.pojo.SarifSchema210;

import static java.lang.String.format;

@ScannerSide
@ComputeEngineSide
public class SarifSerializerImpl implements SarifSerializer {
  private static final String SARIF_REPORT_ERROR = "Failed to read SARIF report at '%s': %s";
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
      throw new SarifDeserializationException(Category.MAPPING, e.getMessage(), e);
    } catch (JsonParseException e) {
      throw new SarifDeserializationException(Category.SYNTAX, format(SARIF_REPORT_ERROR, reportPath, e.getMessage()), e);
    } catch (JsonMappingException e) {
      if (e.getMessage() != null && (e.getMessage().contains("out of range") || e.getMessage().contains("overflow"))) {
        throw new SarifDeserializationException(Category.VALUE, format(SARIF_REPORT_ERROR, reportPath, e.getMessage()), e);
      }
      throw new SarifDeserializationException(Category.MAPPING, format(SARIF_REPORT_ERROR, reportPath, e.getMessage()), e);
    } catch (FileNotFoundException e) {
      throw new SarifDeserializationException(Category.FILE_NOT_FOUND, format(SARIF_REPORT_ERROR, reportPath, e.getMessage()), e);
    } catch (IOException e) {
      throw new IllegalStateException(format(SARIF_REPORT_ERROR, reportPath, e.getMessage()), e);
    }
  }

  private static class UnsupportedSarifVersionException extends IOException {

    public UnsupportedSarifVersionException(String message, Throwable t) {
      super(message, t);
    }
  }
}
