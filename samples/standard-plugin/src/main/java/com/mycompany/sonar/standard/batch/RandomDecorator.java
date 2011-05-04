package com.mycompany.sonar.standard.batch;

import com.mycompany.sonar.standard.SampleMetrics;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

public class RandomDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    // Execute only on Java projects
    return StringUtils.equals(project.getLanguageKey(), Java.KEY);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    // This method is executed on the whole tree of resources.
    // Bottom-up navigation : Java methods -> Java classes -> files -> packages -> modules -> project
    if (Scopes.isBlockUnit(resource)) {
      // Sonar API includes many libraries like commons-lang and google-collections
      double value = RandomUtils.nextDouble();

      // Add a measure to the current Java method
      context.saveMeasure(SampleMetrics.RANDOM, value);

    } else {
      // we sum random values on resources different than method
      context.saveMeasure(SampleMetrics.RANDOM, MeasureUtils.sum(true, context.getChildrenMeasures(SampleMetrics.RANDOM)));
    }
  }
}
