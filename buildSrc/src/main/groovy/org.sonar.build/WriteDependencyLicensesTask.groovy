package org.sonar.build

import groovy.json.JsonOutput
import groovy.xml.XmlSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Resolves a named project Configuration and writes a `dependency-license.json` matching
 * the legacy hierynomus shape: `{dependencies: [{name: "g:a:v", licenses: [{name, url?}]}]}`.
 * Walks the resolved dependency graph. Project (intra-build) dependencies are not listed
 * as third-party entries, but the walk traverses through them so external transitives they
 * pull in are still reported. For each external module, parses the POM for `<licenses>`;
 * if absent, walks the parent chain up to 5 levels. Only POM metadata is read — no JAR
 * extraction, so this avoids the case-insensitive filesystem bug that breaks
 * com.github.jk1.dependency-license-report on Windows NTFS and default macOS APFS.
 */
abstract class WriteDependencyLicensesTask extends DefaultTask {

  @Input
  abstract Property<String> getConfigurationName()

  @OutputDirectory
  abstract DirectoryProperty getOutputDir()

  @TaskAction
  void run() {
    def conf = project.configurations.getByName(configurationName.get())

    // Step 1: walk the dependency graph to identify external module components. Skip project
    // (intra-build) deps so internal modules don't appear in the report, but still walk through
    // them so their external transitives are reachable.
    def externalKeys = new HashSet<String>()
    def visited = new HashSet<String>()
    def queue = new ArrayDeque<>()
    queue.addAll(conf.incoming.resolutionResult.root.dependencies)
    while (!queue.isEmpty()) {
      def edge = queue.poll()
      if (!(edge instanceof ResolvedDependencyResult)) continue
      def component = edge.selected
      def id = component.id
      def displayKey = id.displayName
      if (!visited.add(displayKey)) continue
      if (id instanceof ModuleComponentIdentifier) {
        def mv = component.moduleVersion
        externalKeys.add("${mv.group}:${mv.name}:${mv.version}".toString())
      }
      queue.addAll(component.dependencies)
    }

    // Step 2: emit one entry per resolved artifact for components in the external set. This
    // matches hierynomus's behavior of emitting separate entries per classifier (e.g. one for
    // `netty-...linux-x86_64.jar` and one for `netty-...4.2.2.Final.jar`). Components that
    // appear in the graph but produce no real JAR (Kotlin Multiplatform umbrellas like
    // `com.squareup.okhttp3:okhttp`, BOMs, platforms) are naturally excluded because they
    // don't show up in resolvedArtifacts.
    def pomCache = new HashMap<String, Map>()
    def deps = []
    conf.resolvedConfiguration.resolvedArtifacts.each { artifact ->
      def id = artifact.moduleVersion.id
      def key = "${id.group}:${id.name}:${id.version}".toString()
      if (!externalKeys.contains(key)) return
      def pomData = pomCache.computeIfAbsent(key) { k -> readPom(id.group, id.name, id.version, 5) }
      if (pomData.packaging == 'pom') return
      // Match hierynomus: when no licenses are declared anywhere, emit a placeholder entry.
      def licenses = pomData.licenses.isEmpty() ? [[name: 'No license found', url: null]] : pomData.licenses
      deps << [name: key, file: artifact.file.name, licenses: licenses]
    }
    deps.sort { it.name }
    def out = new File(outputDir.get().asFile, 'dependency-license.json')
    out.parentFile.mkdirs()
    out.text = JsonOutput.prettyPrint(JsonOutput.toJson([dependencies: deps]))
  }

  Map readPom(String group, String name, String version, int parentDepth) {
    if (parentDepth <= 0) return [packaging: '', licenses: []]
    try {
      def pomConfig = project.configurations.detachedConfiguration(
        project.dependencies.create("${group}:${name}:${version}@pom")
      )
      pomConfig.transitive = false
      def pomFile = pomConfig.singleFile
      def pom = new XmlSlurper().parse(pomFile)
      def packaging = pom.packaging.text().trim()
      def licenses = pom.licenses.license.collect { l ->
        // hierynomus always emits both `name` and `url`; mirror that — use empty string
        // when the POM <url> is missing or blank.
        [name: l.name.text().trim(), url: l.url.text().trim()]
      }
      if (licenses.isEmpty() && pom.parent.size() > 0) {
        def pg = pom.parent.groupId.text().trim()
        def pa = pom.parent.artifactId.text().trim()
        def pv = pom.parent.version.text().trim()
        if (pg && pa && pv) {
          def parentData = readPom(pg, pa, pv, parentDepth - 1)
          licenses = parentData.licenses
        }
      }
      return [packaging: packaging, licenses: licenses]
    } catch (Exception e) {
      logger.warn("Could not read POM for ${group}:${name}:${version}: ${e.message}")
      return [packaging: '', licenses: []]
    }
  }
}
