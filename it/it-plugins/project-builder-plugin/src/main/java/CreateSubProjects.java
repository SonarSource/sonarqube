import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

import java.io.File;

/**
 * This plugin relates to projects/project-builder sample
 */
public final class CreateSubProjects extends ProjectBuilder {

  private Settings settings;

  public CreateSubProjects(ProjectReactor reactor, Settings settings) {
    super(reactor);

    // A real implementation should for example use the configuration
    this.settings = settings;
  }

  @Override
  protected void build(ProjectReactor reactor) {
    if (!settings.getBoolean("sonar.enableProjectBuilder")) {
      return;
    }
    System.out.println("---> Creating sub-projects");
    ProjectDefinition root = reactor.getRoot();

    // add two modules
    createSubProjectWithSourceDir(root);
    createSubProjectWithSourceFiles(root);
  }

  private ProjectDefinition createSubProjectWithSourceDir(ProjectDefinition root) {
    File baseDir = new File(root.getBaseDir(), "module_a");
    ProjectDefinition subProject = ProjectDefinition.create();
    subProject.setBaseDir(baseDir).setWorkDir(new File(baseDir, "target/.sonar"));
    subProject.setKey("com.sonarsource.it.projects.batch:project-builder-module-a");
    subProject.setVersion(root.getVersion());
    subProject.setName("Module A");
    subProject.setSourceDirs("src");
    root.addSubProject(subProject);
    return subProject;
  }

  private ProjectDefinition createSubProjectWithSourceFiles(ProjectDefinition root) {
    File baseDir = new File(root.getBaseDir(), "module_b");
    ProjectDefinition subProject = ProjectDefinition.create();
    subProject.setBaseDir(baseDir).setWorkDir(new File(baseDir, "target/.sonar"));
    subProject.setKey("com.sonarsource.it.projects.batch:project-builder-module-b");
    subProject.setVersion(root.getVersion());
    subProject.setName("Module B");
    subProject.addSourceFiles("src/HelloB.java");
    root.addSubProject(subProject);
    return subProject;
  }
}
