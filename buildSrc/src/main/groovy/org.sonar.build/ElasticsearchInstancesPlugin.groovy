package org.sonar.build

import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.build.event.BuildEventsListenerRegistry

/**
 * Applied to the root project. Each test task gets its own Elasticsearch instance through ES_PORT, read by EsTester.
 * The ports come from the comma-separated elasticsearchPorts property (default 9200).
 */
abstract class ElasticsearchInstancesPlugin implements Plugin<Project> {

  @Inject
  abstract BuildEventsListenerRegistry getEventsListenerRegistry()

  @Override
  void apply(Project root) {
    List<Integer> ports = (root.findProperty('elasticsearchPorts') ?: '9200').toString()
      .split(',').collect { it.trim() as Integer }
    def pool = root.gradle.sharedServices.registerIfAbsent('elasticsearchInstancePool', ElasticsearchInstancePool) {
      it.parameters.ports.set(ports)
      it.maxParallelUsages.set(ports.size())
    }
    eventsListenerRegistry.onTaskCompletion(pool)

    root.allprojects { project ->
      project.tasks.withType(Test).configureEach { Test test ->
        test.usesService(pool)
        test.doFirst {
          test.environment('ES_PORT', pool.get().acquire(test.path))
        }
      }
    }
  }
}
