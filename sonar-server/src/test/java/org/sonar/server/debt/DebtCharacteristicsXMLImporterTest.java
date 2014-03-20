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

package org.sonar.server.debt;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;
import org.sonar.api.server.debt.DebtCharacteristic;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DebtCharacteristicsXMLImporterTest {

  @Test
  public void import_characteristics() {
    String xml = getFileContent("import_characteristics.xml");

    DebtModel debtModel = new DebtCharacteristicsXMLImporter().importXML(xml);
    List<DebtCharacteristic> rootCharacteristics = debtModel.rootCharacteristics();

    assertThat(rootCharacteristics).hasSize(2);
    assertThat(rootCharacteristics.get(0).key()).isEqualTo("PORTABILITY");
    assertThat(rootCharacteristics.get(0).name()).isEqualTo("Portability");
    assertThat(rootCharacteristics.get(0).order()).isEqualTo(1);

    assertThat(rootCharacteristics.get(1).key()).isEqualTo("MAINTAINABILITY");
    assertThat(rootCharacteristics.get(1).name()).isEqualTo("Maintainability");
    assertThat(rootCharacteristics.get(1).order()).isEqualTo(2);

    List<DebtCharacteristic> portabilitySubCharacteristics = debtModel.subCharacteristics("PORTABILITY");
    assertThat(portabilitySubCharacteristics).hasSize(2);
    assertThat(portabilitySubCharacteristics.get(0).key()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(portabilitySubCharacteristics.get(0).name()).isEqualTo("Compiler related portability");
    assertThat(portabilitySubCharacteristics.get(1).key()).isEqualTo("HARDWARE_RELATED_PORTABILITY");
    assertThat(portabilitySubCharacteristics.get(1).name()).isEqualTo("Hardware related portability");

    List<DebtCharacteristic> maintainabilitySubCharacteristics = debtModel.subCharacteristics("MAINTAINABILITY");
    assertThat(maintainabilitySubCharacteristics).hasSize(1);
    assertThat(maintainabilitySubCharacteristics.get(0).key()).isEqualTo("READABILITY");
    assertThat(maintainabilitySubCharacteristics.get(0).name()).isEqualTo("Readability");
  }

  @Test
  public void import_badly_formatted_xml() {
    String xml = getFileContent("import_badly_formatted_xml.xml");

    DebtModel debtModel = new DebtCharacteristicsXMLImporter().importXML(xml);
    List<DebtCharacteristic> rootCharacteristics = debtModel.rootCharacteristics();

    // characteristics
    assertThat(rootCharacteristics).hasSize(2);
    assertThat(rootCharacteristics.get(0).key()).isEqualTo("USABILITY");
    assertThat(rootCharacteristics.get(0).name()).isEqualTo("Usability");

    assertThat(rootCharacteristics.get(1).key()).isEqualTo("EFFICIENCY");
    assertThat(rootCharacteristics.get(1).name()).isEqualTo("Efficiency");

    // sub-characteristic
    assertThat(debtModel.subCharacteristics("EFFICIENCY")).hasSize(1);
    assertThat(debtModel.subCharacteristics("EFFICIENCY").get(0).key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(debtModel.subCharacteristics("EFFICIENCY").get(0).name()).isEqualTo("Memory use");
  }

  @Test
  public void fail_on_bad_xml() {
    String xml = getFileContent("fail_on_bad_xml.xml");

    try {
      new DebtCharacteristicsXMLImporter().importXML(xml);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

  private String getFileContent(String file) {
    try {
      return Resources.toString(Resources.getResource(DebtCharacteristicsXMLImporterTest.class, "DebtCharacteristicsXMLImporterTest/" + file), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
