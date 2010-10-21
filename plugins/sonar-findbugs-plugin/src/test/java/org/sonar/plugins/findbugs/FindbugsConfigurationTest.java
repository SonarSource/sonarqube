package org.sonar.plugins.findbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.SimpleProjectFileSystem;

import java.io.File;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private Project project;
  private File findbugsTempDir;

  @Before
  public void setup() {
    project = mock(Project.class);
    findbugsTempDir = tempFolder.newFolder("findbugs");
    when(project.getFileSystem()).thenReturn(new SimpleProjectFileSystem(findbugsTempDir));
  }

  @Test
  public void shouldSaveConfigFiles() throws Exception {
    FindbugsConfiguration conf = new FindbugsConfiguration(project, RulesProfile.create(), new FindbugsProfileExporter(), null);

    conf.saveIncludeConfigXml();
    conf.saveExcludeConfigXml();

    File findbugsIncludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-include.xml");
    File findbugsExcludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-exclude.xml");
    assertThat(findbugsIncludeFile.exists(), is(true));
    assertThat(findbugsExcludeFile.exists(), is(true));
  }

}
