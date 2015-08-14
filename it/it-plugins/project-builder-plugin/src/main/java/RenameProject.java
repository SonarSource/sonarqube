import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

/**
 * This plugin relates to projects/project-builder sample
 */
public final class RenameProject extends ProjectBuilder {

  private Settings settings;
  
  public RenameProject(ProjectReactor reactor, Settings settings) {
    super(reactor);
    this.settings = settings;
  }

  @Override
  protected void build(ProjectReactor reactor) {
    if (!settings.getBoolean("sonar.enableProjectBuilder")) {
      return;
    }
    System.out.println("---> Renaming project");
    // change name of root project
    ProjectDefinition root = reactor.getRoot();
    root.setName("Name changed by plugin");
  }
}
