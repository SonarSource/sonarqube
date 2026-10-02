package org.sonar.build

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.regex.Pattern
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

/**
 * Fails when a module that ships its own SQL (MyBatis mappers, raw JDBC statements) is not tested against the vendor
 * databases of the DB JUnit job, unless it is explicitly listed as H2-only with a reason. Modules that only go through
 * the sonar-db-dao DAOs don't need it: those DAOs are vendor-tested in sonar-db-dao.
 *
 * A module is vendor-tested when its test task is configured with {@link DbAwareTest#configure}, the DB JUnit job
 * script runs that task, and the job's PR path filter matches every file of the module that contains SQL.
 */
abstract class DbVendorTestCoverageCheck extends DefaultTask {
  private static final Pattern OWN_SQL = ~/\bclass\s+\w+\s+(implements|extends)\s+[\w.]*MyBatisConfExtension\b|\bnewScrollingSelectStatement\(|\.prepareStatement\(|\.createStatement\(/
  private static final Pattern DB_JOB_TEST_TASK = ~/(:[\w.\-:]+):test(?=\s|$)/
  private static final Pattern PATH_FILTER_ENTRY = ~/^\s*-\s*(\S+)\s*$/

  @InputFile
  File dbJobScript

  @InputFile
  File prPathFilters

  @InputFile
  File h2OnlyModules

  @Internal
  File rootDir

  @Internal
  List<Module> modules = []

  static class Module {
    String path
    String dir
    boolean dbAware
    Set<File> mainSourceDirs
    Set<File> mainResourceDirs
  }

  DbVendorTestCoverageCheck() {
    group = 'verification'
    description = 'Checks that modules with their own SQL are tested against every supported database vendor.'
    outputs.upToDateWhen { false }
  }

  /** Must run once all projects are evaluated, so that DbAwareTest.configure calls are visible. */
  void collectModules(Project root) {
    rootDir = root.rootDir
    modules = root.allprojects.sort { it.path }.findResults { Project p ->
      def test = p.tasks.findByName('test')
      def sourceSets = p.extensions.findByType(SourceSetContainer)
      if (!(test instanceof Test) || sourceSets == null) {
        return null
      }
      new Module(
        path: p.path,
        dir: root.rootDir.toPath().relativize(p.projectDir.toPath()).toString().replace('\\', '/'),
        dbAware: DbAwareTest.isConfigured(test),
        mainSourceDirs: sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).allJava.srcDirs,
        mainResourceDirs: sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).resources.srcDirs)
    }
  }

  @TaskAction
  void check() {
    Set<String> runByDbJob = parseDbJobTestTasks()
    List<String> dbPathFilters = parseDbPathFilters()
    Map<String, String> h2Only = parseH2OnlyModules()
    List<String> violations = []

    dbPathFilters.findAll { !new File(rootDir, literalPrefix(it)).exists() }.each {
      violations << "'db' path filter '$it' in ${prPathFilters.name} points to a file or directory that does not exist."
    }
    List<PathMatcher> filterMatchers = dbPathFilters.collect { FileSystems.default.getPathMatcher("glob:$it") }
    Set<String> projectPaths = modules*.path as Set
    h2Only.keySet().findAll { !projectPaths.contains(it) }.each {
      violations << "$it is listed in ${h2OnlyModules.name} but is not a project of this build."
    }
    runByDbJob.findAll { !projectPaths.contains(it) }.each {
      violations << "${dbJobScript.name} runs $it:test but $it is not a project of this build."
    }

    modules.each { Module m ->
      boolean inDbJob = runByDbJob.contains(m.path)
      boolean inPathFilter = dbPathFilters.any { it.startsWith("${m.dir}/") }
      List<String> sqlFiles = (findFiles(m.mainResourceDirs) { it.name.endsWith('Mapper.xml') } +
        findFiles(m.mainSourceDirs) { (it.name.endsWith('.java') || it.name.endsWith('.kt')) && OWN_SQL.matcher(it.text).find() })
        .collect { rootDir.toPath().relativize(it.toPath()).toString().replace('\\', '/') }
      boolean ownSql = !sqlFiles.isEmpty()
      boolean exempted = h2Only.containsKey(m.path)

      if (inDbJob && !m.dbAware) {
        violations << "${m.path} is run by ${dbJobScript.name} but its test task is not configured with DbAwareTest.configure(it), so it runs against H2."
      }
      if (m.dbAware && !inDbJob) {
        violations << "${m.path} is configured with DbAwareTest but ${dbJobScript.name} does not run ${m.path}:test."
      }
      if (m.dbAware) {
        sqlFiles.findAll { file -> !filterMatchers.any { it.matches(Path.of(file)) } }.each {
          violations << "$it has SQL but no 'db' path filter in ${prPathFilters.name} matches it, so PRs changing it skip the DB JUnit job."
        }
      }
      if (m.dbAware && !ownSql && !inPathFilter) {
        violations << "${m.path} is vendor-tested but no 'db' path filter in ${prPathFilters.name} starts with '${m.dir}/', so PRs changing it skip the DB JUnit job."
      }
      if (ownSql && !m.dbAware && !inDbJob && !exempted) {
        violations << "${m.path} ships its own SQL (MyBatis mappers or JDBC statements), but its tests never run against PostgreSQL, SQL Server or Oracle. " +
          "Configure its test task with DbAwareTest.configure(it) and add it to ${dbJobScript.name}, or list it in ${h2OnlyModules.name} with a reason."
      }
      if (exempted && (m.dbAware || !ownSql)) {
        violations << "${m.path} is listed in ${h2OnlyModules.name} but " + (m.dbAware ? 'is vendor-tested' : 'has no SQL of its own') + '. Remove the entry.'
      }
    }

    if (!violations.isEmpty()) {
      throw new GradleException("Database vendor test coverage check failed:\n  - " + violations.join('\n  - '))
    }
  }

  protected Set<String> parseDbJobTestTasks() {
    DB_JOB_TEST_TASK.matcher(dbJobScript.text).findAll().collect { it[1] } as Set
  }

  protected List<String> parseDbPathFilters() {
    List<String> filters = []
    Integer dbIndent = null
    prPathFilters.readLines().each { String line ->
      int indent = line.length() - line.stripLeading().length()
      if (dbIndent == null) {
        if (line.trim() == 'db:') {
          dbIndent = indent
        }
      } else if (dbIndent >= 0) {
        def entry = line =~ PATH_FILTER_ENTRY
        if (indent > dbIndent && entry.matches()) {
          filters << entry.group(1)
        } else if (!line.isBlank()) {
          dbIndent = -1
        }
      }
    }
    if (filters.isEmpty()) {
      throw new GradleException("No 'db:' path filters found in $prPathFilters")
    }
    filters
  }

  protected Map<String, String> parseH2OnlyModules() {
    Map<String, String> entries = [:]
    h2OnlyModules.readLines().collect { it.trim() }.findAll { it && !it.startsWith('#') }.each { String line ->
      def parts = line.split(/\s+/, 2)
      if (parts.length < 2) {
        throw new GradleException("Entry '$line' in $h2OnlyModules must be followed by the reason it is not vendor-tested.")
      }
      entries[parts[0]] = parts[1]
    }
    entries
  }

  protected static String literalPrefix(String glob) {
    String literal = glob.split(/[*?\[{]/, 2)[0]
    literal == glob || literal.endsWith('/') ? literal : literal.substring(0, literal.lastIndexOf('/') + 1)
  }

  protected static List<File> findFiles(Set<File> dirs, Closure<Boolean> predicate) {
    List<File> files = []
    dirs.findAll { it.isDirectory() }.each { dir ->
      dir.traverse(type: groovy.io.FileType.FILES) { File f ->
        if (predicate(f)) {
          files << f
        }
      }
    }
    files
  }
}
