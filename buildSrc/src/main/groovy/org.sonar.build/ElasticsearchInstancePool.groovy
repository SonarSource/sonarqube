package org.sonar.build

import java.util.concurrent.TimeUnit
import org.gradle.api.provider.ListProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent

/**
 * Leases one Elasticsearch instance (by HTTP port) to each test task, so parallel test tasks never share one:
 * EsTester recreates indexes by their real names. A lease is returned when the task-finished event arrives,
 * which can come slightly after the next task starts, hence the short wait in acquire().
 */
abstract class ElasticsearchInstancePool implements BuildService<Params>, OperationCompletionListener {

  interface Params extends BuildServiceParameters {
    ListProperty<Integer> getPorts()
  }

  private final Deque<Integer> freePorts = new ArrayDeque<>()
  private final Map<String, Integer> leasedPorts = [:]

  ElasticsearchInstancePool() {
    freePorts.addAll(parameters.ports.get())
  }

  synchronized int acquire(String taskPath) {
    long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2)
    while (freePorts.isEmpty()) {
      long remaining = deadline - System.nanoTime()
      if (remaining <= 0) {
        throw new IllegalStateException("No free Elasticsearch instance for ${taskPath}, leased: ${leasedPorts}")
      }
      TimeUnit.NANOSECONDS.timedWait(this, remaining)
    }
    int port = freePorts.poll()
    leasedPorts[taskPath] = port
    port
  }

  @Override
  synchronized void onFinish(FinishEvent event) {
    if (event instanceof TaskFinishEvent) {
      Integer port = leasedPorts.remove(event.descriptor.taskPath)
      if (port != null) {
        freePorts.add(port)
        notifyAll()
      }
    }
  }
}
