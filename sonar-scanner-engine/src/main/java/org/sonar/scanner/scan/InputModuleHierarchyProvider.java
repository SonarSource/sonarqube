package org.sonar.scanner.scan;

import java.util.HashMap;
import java.util.Map;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.scan.filesystem.BatchIdGenerator;

public class InputModuleHierarchyProvider extends ProviderAdapter {

  private DefaultInputModuleHierarchy hierarchy = null;

  public DefaultInputModuleHierarchy provide(ProjectBuildersExecutor projectBuildersExecutor, ProjectReactorValidator validator,
    ProjectReactor projectReactor, BatchIdGenerator batchIdGenerator) {
    if (hierarchy == null) {
      // 1 Apply project builders
      projectBuildersExecutor.execute(projectReactor);

      // 2 Validate final reactor
      validator.validate(projectReactor);

      // 3 Create module and its hierarchy
      DefaultInputModule root = new DefaultInputModule(projectReactor.getRoot(), batchIdGenerator.get());
      Map<DefaultInputModule, DefaultInputModule> parents = createChildren(root, batchIdGenerator);
      if (parents.isEmpty()) {
        hierarchy = new DefaultInputModuleHierarchy(root);
      } else {
        hierarchy = new DefaultInputModuleHierarchy(parents);
      }
    }
    return hierarchy;
  }

  private Map<DefaultInputModule, DefaultInputModule> createChildren(DefaultInputModule parent, BatchIdGenerator batchIdGenerator) {
    Map<DefaultInputModule, DefaultInputModule> parents = new HashMap<>();

    for (ProjectDefinition def : parent.definition().getSubProjects()) {
      DefaultInputModule child = new DefaultInputModule(def, batchIdGenerator.get());
      parents.put(child, parent);
    }
    return parents;
  }
}
