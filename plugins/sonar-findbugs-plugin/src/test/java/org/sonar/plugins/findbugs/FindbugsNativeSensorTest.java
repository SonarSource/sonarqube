package org.sonar.plugins.findbugs;

import org.apache.commons.configuration.Configuration;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.test.IsViolation;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class FindbugsNativeSensorTest extends FindbugsTests {

  @Test
  public void shouldExecuteWhenSomeRulesAreActive() throws Exception {
    FindbugsNativeSensor sensor = new FindbugsNativeSensor(createRulesProfileWithActiveRules(), new FindbugsRuleFinder(), null);
    Project project = createProject();
    assertTrue(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldExecuteWhenReuseExistingRulesConfig() throws Exception {
    FindbugsNativeSensor analyser = new FindbugsNativeSensor(RulesProfile.create(), new FindbugsRuleFinder(), null);
    Project pom = createProject();
    when(pom.getReuseExistingRulesConfig()).thenReturn(true);
    assertTrue(analyser.shouldExecuteOnProject(pom));
  }

  @Test
  public void shouldNotExecuteWhenNoRulesAreActive() throws Exception {
    FindbugsNativeSensor analyser = new FindbugsNativeSensor(RulesProfile.create(), new FindbugsRuleFinder(), null);
    Project pom = createProject();
    assertFalse(analyser.shouldExecuteOnProject(pom));
  }

  @Test
  public void shouldNotExecuteOnEar() {
    Project project = createProject();
    when(project.getPom().getPackaging()).thenReturn("ear");
    FindbugsNativeSensor analyser = new FindbugsNativeSensor(createRulesProfileWithActiveRules(), new FindbugsRuleFinder(), null);
    assertFalse(analyser.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldExecuteFindbugsWhenNoReportProvided() throws Exception {
    Project project = createProject();
    FindbugsExecutor executor = mock(FindbugsExecutor.class);
    SensorContext context = mock(SensorContext.class);
    Configuration conf = mock(Configuration.class);
    File xmlFile = new File(getClass().getResource("/org/sonar/plugins/findbugs/findbugsXml.xml").toURI());
    when(project.getConfiguration()).thenReturn(conf);
    when(executor.execute()).thenReturn(xmlFile);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));

    FindbugsNativeSensor analyser = new FindbugsNativeSensor(createRulesProfileWithActiveRules(), new FindbugsRuleFinder(), executor);
    analyser.analyse(project, context);

    verify(executor).execute();
    verify(context, times(3)).saveViolation(any(Violation.class));

    Violation wanted = new Violation(null, new JavaFile("org.sonar.commons.ZipUtils")).setMessage(
        "Empty zip file entry created in org.sonar.commons.ZipUtils._zip(String, File, ZipOutputStream)").setLineId(107);

    verify(context).saveViolation(argThat(new IsViolation(wanted)));

    wanted = new Violation(null, new JavaFile("org.sonar.commons.resources.MeasuresDao")).setMessage(
        "The class org.sonar.commons.resources.MeasuresDao$1 could be refactored into a named _static_ inner class").setLineId(56);

    verify(context).saveViolation(argThat(new IsViolation(wanted)));
  }

  @Test
  public void shouldReuseReport() throws Exception {
    Project project = createProject();
    FindbugsExecutor executor = mock(FindbugsExecutor.class);
    SensorContext context = mock(SensorContext.class);
    Configuration conf = mock(Configuration.class);
    File xmlFile = new File(getClass().getResource("/org/sonar/plugins/findbugs/findbugsXml.xml").toURI());
    when(conf.getString(CoreProperties.FINDBUGS_REPORT_PATH)).thenReturn(xmlFile.getAbsolutePath());
    when(project.getConfiguration()).thenReturn(conf);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));

    FindbugsNativeSensor analyser = new FindbugsNativeSensor(createRulesProfileWithActiveRules(), new FindbugsRuleFinder(), executor);
    analyser.analyse(project, context);

    verify(executor, never()).execute();
    verify(context, times(3)).saveViolation(any(Violation.class));

    Violation wanted = new Violation(null, new JavaFile("org.sonar.commons.ZipUtils")).setMessage(
        "Empty zip file entry created in org.sonar.commons.ZipUtils._zip(String, File, ZipOutputStream)").setLineId(107);

    verify(context).saveViolation(argThat(new IsViolation(wanted)));

    wanted = new Violation(null, new JavaFile("org.sonar.commons.resources.MeasuresDao")).setMessage(
        "The class org.sonar.commons.resources.MeasuresDao$1 could be refactored into a named _static_ inner class").setLineId(56);

    verify(context).saveViolation(argThat(new IsViolation(wanted)));
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
