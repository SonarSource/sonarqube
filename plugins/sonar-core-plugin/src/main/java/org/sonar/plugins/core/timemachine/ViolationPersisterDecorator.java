package org.sonar.plugins.core.timemachine;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ViolationPersister;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class ViolationPersisterDecorator implements Decorator {

  /**
   * Those chars would be ignored during generation of checksums.
   */
  private static final String SPACE_CHARS = "\t\n\r ";

  private RuleFinder ruleFinder;
  private PastViolationsLoader pastViolationsLoader;
  private ViolationPersister violationPersister;

  List<String> checksums = Lists.newArrayList();
  List<String> pastChecksums = Lists.newArrayList();

  public ViolationPersisterDecorator(RuleFinder ruleFinder, PastViolationsLoader pastViolationsLoader, ViolationPersister violationPersister) {
    this.ruleFinder = ruleFinder;
    this.pastViolationsLoader = pastViolationsLoader;
    this.violationPersister = violationPersister;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Snapshot previousLastSnapshot = pastViolationsLoader.getPreviousLastSnapshot(resource);
    // Load past violations
    List<RuleFailureModel> pastViolations = pastViolationsLoader.getPastViolations(previousLastSnapshot);
    // Load past source and calculate checksums
    pastChecksums = getChecksums(pastViolationsLoader.getPastSource(previousLastSnapshot));
    // Load current source and calculate checksums
    checksums = getChecksums(pastViolationsLoader.getSource(resource));
    // Save violations
    compareWithPastViolations(context, pastViolations);
    // Clear caches
    checksums.clear();
    pastChecksums.clear();
  }

  private void compareWithPastViolations(DecoratorContext context, List<RuleFailureModel> pastViolations) {
    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    for (RuleFailureModel pastViolation : pastViolations) {
      Rule rule = ruleFinder.findById(pastViolation.getRuleId());
      pastViolationsByRule.put(rule, pastViolation);
    }
    // for each violation, search equivalent past violation
    for (Violation violation : context.getViolations()) {
      RuleFailureModel pastViolation = selectPastViolation(violation, pastViolationsByRule);
      if (pastViolation != null) {
        // remove violation from past, since would be updated and shouldn't affect other violations anymore
        pastViolationsByRule.remove(violation.getRule(), pastViolation);
      }
      violationPersister.saveOrUpdateViolation(context.getProject(), violation, pastViolation);
    }
  }

  /**
   * @return checksums, never null
   */
  private List<String> getChecksums(SnapshotSource source) {
    return source == null || source.getData() == null ? Collections.<String> emptyList() : getChecksums(source.getData());
  }

  static List<String> getChecksums(String data) {
    List<String> result = Lists.newArrayList();
    try {
      List<String> lines = IOUtils.readLines(new StringInputStream(data));
      for (String line : lines) {
        String reducedLine = StringUtils.replaceChars(line, SPACE_CHARS, "");
        result.add(DigestUtils.md5Hex(reducedLine));
      }
    } catch (IOException e) {
      throw new SonarException("Unable to calculate checksums", e);
    }
    return result;
  }

  /**
   * @return checksum or null if checksum not exists for line
   */
  private String getChecksumForLine(List<String> checksums, Integer line) {
    if (line == null || line < 1 || line > checksums.size()) {
      return null;
    }
    return checksums.get(line - 1);
  }

  /**
   * Search for past violation.
   */
  RuleFailureModel selectPastViolation(Violation violation, Multimap<Rule, RuleFailureModel> pastViolationsByRule) {
    // skip violation, if there is no past violations with same rule
    if (!pastViolationsByRule.containsKey(violation.getRule())) {
      return null;
    }
    Collection<RuleFailureModel> pastViolations = pastViolationsByRule.get(violation.getRule());
    RuleFailureModel found = selectPastViolationUsingLine(violation, pastViolations);
    if (found == null) {
      found = selectPastViolationUsingChecksum(violation, pastViolations);
    }
    return found;
  }

  /**
   * Search for past violation with same message and line.
   */
  private RuleFailureModel selectPastViolationUsingLine(Violation violation, Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (violation.getLineId() == pastViolation.getLine() && StringUtils.equals(violation.getMessage(), pastViolation.getMessage())) {
        return pastViolation;
      }
    }
    return null;
  }

  /**
   * Search for past violation with same message and checksum.
   */
  private RuleFailureModel selectPastViolationUsingChecksum(Violation violation, Collection<RuleFailureModel> pastViolations) {
    String checksum = getChecksumForLine(checksums, violation.getLineId());
    // skip violation, which not attached to line
    if (checksum == null) {
      return null;
    }
    for (RuleFailureModel pastViolation : pastViolations) {
      String pastChecksum = getChecksumForLine(pastChecksums, pastViolation.getLine());
      if (StringUtils.equals(checksum, pastChecksum) && StringUtils.equals(violation.getMessage(), pastViolation.getMessage())) {
        return pastViolation;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
