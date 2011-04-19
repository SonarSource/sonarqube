/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.sonar.api.batch.*;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.components.PastViolationsLoader;
import org.sonar.batch.index.ViolationPersister;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
@DependedUpon("ViolationPersisterDecorator") /* temporary workaround - see NewViolationsDecorator */
public class ViolationPersisterDecorator implements Decorator {

  /**
   * Those chars would be ignored during generation of checksums.
   */
  private static final String SPACE_CHARS = "\t\n\r ";

  private RuleFinder ruleFinder;
  private PastViolationsLoader pastViolationsLoader;
  private ViolationPersister violationPersister;

  List<String> checksums = Lists.newArrayList();

  public ViolationPersisterDecorator(RuleFinder ruleFinder, PastViolationsLoader pastViolationsLoader, ViolationPersister violationPersister) {
    this.ruleFinder = ruleFinder;
    this.pastViolationsLoader = pastViolationsLoader;
    this.violationPersister = violationPersister;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (context.getViolations().isEmpty()) {
      return;
    }
    // Load past violations
    List<RuleFailureModel> pastViolations = pastViolationsLoader.getPastViolations(resource);
    // Load current source and calculate checksums
    checksums = getChecksums(pastViolationsLoader.getSource(resource));
    // Save violations
    compareWithPastViolations(context, pastViolations);
    // Clear cache
    checksums.clear();
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
        // remove violation, since would be updated and shouldn't affect other violations anymore
        pastViolationsByRule.remove(violation.getRule(), pastViolation);
      }
      String checksum = getChecksumForLine(checksums, violation.getLineId());
      violationPersister.saveViolation(context.getProject(), violation, pastViolation, checksum);
    }
    violationPersister.commit();
  }

  /**
   * @return checksums, never null
   */
  private List<String> getChecksums(SnapshotSource source) {
    return source == null || source.getData() == null ? Collections.<String>emptyList() : getChecksums(source.getData());
  }

  static List<String> getChecksums(String data) {
    List<String> result = Lists.newArrayList();
    StringInputStream stream = new StringInputStream(data);
    try {
      List<String> lines = IOUtils.readLines(stream);
      for (String line : lines) {
        result.add(getChecksum(line));
      }
    } catch (IOException e) {
      throw new SonarException("Unable to calculate checksums", e);
      
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return result;
  }

  static String getChecksum(String line) {
    String reducedLine = StringUtils.replaceChars(line, SPACE_CHARS, "");
    return DigestUtils.md5Hex(reducedLine);
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
    Collection<RuleFailureModel> pastViolations = pastViolationsByRule.get(violation.getRule());
    if (pastViolations==null || pastViolations.isEmpty()) {
      // skip violation, if there is no past violations with same rule
      return null;
    }
    String dbFormattedMessage = RuleFailureModel.abbreviateMessage(violation.getMessage());
    RuleFailureModel found = selectPastViolationUsingLine(violation, dbFormattedMessage, pastViolations);
    if (found == null) {
      found = selectPastViolationUsingChecksum(violation, dbFormattedMessage, pastViolations);
    }
    return found;
  }

  /**
   * Search for past violation with same message and line.
   */
  private RuleFailureModel selectPastViolationUsingLine(Violation violation, String dbFormattedMessage, Collection<RuleFailureModel> pastViolations) {
    for (RuleFailureModel pastViolation : pastViolations) {
      if (ObjectUtils.equals(violation.getLineId(), pastViolation.getLine()) && StringUtils.equals(dbFormattedMessage, pastViolation.getMessage())) {
        return pastViolation;
      }
    }
    return null;
  }

  /**
   * Search for past violation with same message and checksum.
   */
  private RuleFailureModel selectPastViolationUsingChecksum(Violation violation, String dbFormattedMessage, Collection<RuleFailureModel> pastViolations) {
    String checksum = getChecksumForLine(checksums, violation.getLineId());
    // skip violation, which not attached to line
    if (checksum == null) {
      return null;
    }
    for (RuleFailureModel pastViolation : pastViolations) {
      String pastChecksum = pastViolation.getChecksum();
      if (StringUtils.equals(checksum, pastChecksum) && StringUtils.equals(dbFormattedMessage, pastViolation.getMessage())) {
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
