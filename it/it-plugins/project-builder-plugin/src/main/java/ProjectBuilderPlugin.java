import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class ProjectBuilderPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(CreateSubProjects.class, RenameProject.class);
  }
}
