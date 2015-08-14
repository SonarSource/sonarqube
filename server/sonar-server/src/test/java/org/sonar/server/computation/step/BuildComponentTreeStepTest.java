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
package org.sonar.server.computation.step;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Metadata;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.MutableTreeRootHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.batch.protocol.Constants.ComponentType.DIRECTORY;
import static org.sonar.batch.protocol.Constants.ComponentType.FILE;
import static org.sonar.batch.protocol.Constants.ComponentType.MODULE;
import static org.sonar.batch.protocol.Constants.ComponentType.PROJECT;

@RunWith(DataProviderRunner.class)
public class BuildComponentTreeStepTest {
  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 2;
  private static final int DIR_REF_1 = 3;
  private static final int FILE_1_REF = 4;
  private static final int FILE_2_REF = 5;
  private static final int DIR_REF_2 = 6;
  private static final int FILE_3_REF = 7;

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  private Date someDate = new Date();

  private BuildComponentTreeStep underTest = new BuildComponentTreeStep(reportReader, treeRootHolder, analysisMetadataHolder);

  @Before
  public void setUp() {
    reportReader.setMetadata(Metadata.newBuilder().setRootComponentRef(ROOT_REF).setAnalysisDate(someDate.getTime()).build());
  }

  @Test(expected = NullPointerException.class)
  public void fails_if_root_component_does_not_exist_in_reportReader() {
    underTest.execute();
  }

  @DataProvider
  public static Object[][] allComponentTypes() {
    Object[][] res = new Object[Constants.ComponentType.values().length][1];
    int i = 0;
    for (Constants.ComponentType componentType : Constants.ComponentType.values()) {
      res[i][0] = componentType;
      i++;
    }
    return res;
  }

  @Test
  @UseDataProvider("allComponentTypes")
  public void verify_ref_and_type(Constants.ComponentType componentType) {
    int componentRef = 1;
    reportReader.putComponent(component(componentRef, componentType));

    underTest.execute();

    Component root = treeRootHolder.getRoot();
    assertThat(root).isNotNull();
    assertThat(root.getType()).isEqualTo(Component.Type.valueOf(componentType.name()));
    assertThat(root.getReportAttributes().getRef()).isEqualTo(ROOT_REF);
    assertThat(root.getChildren()).isEmpty();

    assertThat(analysisMetadataHolder.getAnalysisDate().getTime()).isEqualTo(someDate.getTime());
  }

  @Test
  public void verify_tree_is_correctly_built() {
    reportReader.putComponent(component(ROOT_REF, PROJECT, MODULE_REF));
    reportReader.putComponent(component(MODULE_REF, MODULE, DIR_REF_1, DIR_REF_2));
    reportReader.putComponent(component(DIR_REF_1, DIRECTORY, FILE_1_REF, FILE_2_REF));
    reportReader.putComponent(component(FILE_1_REF, FILE));
    reportReader.putComponent(component(FILE_2_REF, FILE));
    reportReader.putComponent(component(DIR_REF_2, DIRECTORY, FILE_3_REF));
    reportReader.putComponent(component(FILE_3_REF, FILE));

    underTest.execute();

    Component root = treeRootHolder.getRoot();
    assertThat(root).isNotNull();
    verifyComponent(root, Component.Type.PROJECT, ROOT_REF, 1);
    Component module = root.getChildren().iterator().next();
    verifyComponent(module, Component.Type.MODULE, MODULE_REF, 2);
    Component dir1 = module.getChildren().get(0);
    verifyComponent(dir1, Component.Type.DIRECTORY, DIR_REF_1, 2);
    verifyComponent(dir1.getChildren().get(0), Component.Type.FILE, FILE_1_REF, 0);
    verifyComponent(dir1.getChildren().get(1), Component.Type.FILE, FILE_2_REF, 0);
    Component dir2 = module.getChildren().get(1);
    verifyComponent(dir2, Component.Type.DIRECTORY, DIR_REF_2, 1);
    verifyComponent(dir2.getChildren().iterator().next(), Component.Type.FILE, FILE_3_REF, 0);

    assertThat(analysisMetadataHolder.getAnalysisDate().getTime()).isEqualTo(someDate.getTime());
  }

  private void verifyComponent(Component component, Component.Type type, int componentRef, int size) {
    assertThat(component.getType()).isEqualTo(type);
    assertThat(component.getReportAttributes().getRef()).isEqualTo(componentRef);
    assertThat(component.getChildren()).hasSize(size);
  }

  private static BatchReport.Component component(int componentRef, Constants.ComponentType componentType, int... children) {
    BatchReport.Component.Builder builder = BatchReport.Component.newBuilder().setType(componentType).setRef(componentRef);
    for (int child : children) {
      builder.addChildRef(child);
    }
    return builder.build();
  }

}
