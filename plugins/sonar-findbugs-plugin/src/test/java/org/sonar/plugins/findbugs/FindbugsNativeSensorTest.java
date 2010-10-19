package org.sonar.plugins.findbugs;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Project;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsNativeSensorTest extends FindbugsTests {

  @Test
  public void shouldExecuteWhenSomeRulesAreActive() throws Exception {
    FindbugsSensor sensor = new FindbugsSensor(createRulesProfileWithActiveRules(), new FindbugsRuleFinder(), null);
    Project project = createProject();
    assertTrue(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldNotExecuteWhenNoRulesAreActive() throws Exception {
    FindbugsSensor analyser = new FindbugsSensor(RulesProfile.create(), new FindbugsRuleFinder(), null);
    Project pom = createProject();
    assertFalse(analyser.shouldExecuteOnProject(pom));
  }

  @Test
  public void shouldNotExecuteOnEar() {
    Project project = createProject();
    when(project.getPom().getPackaging()).thenReturn("ear");
    FindbugsSensor analyser = new FindbugsSensor(createRulesProfileWithActiveRules(), new FindbugsRuleFinder(), null);
    assertFalse(analyser.shouldExecuteOnProject(project));
  }

  private Project createProject() {
    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);
    when(fileSystem.hasJavaSourceFiles()).thenReturn(Boolean.TRUE);

    MavenProject mavenProject = mock(MavenProject.class);
    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(project.getPom()).thenReturn(mavenProject);
    return project;
  }

}
