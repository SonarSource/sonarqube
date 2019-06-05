/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scm;

import java.util.Optional;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.RawScannerProperties;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.fs.InputModuleHierarchy;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.scanner.scan.ScanProperties.SCM_REVISION;

public class ScmRevisionImpl implements ScmRevision {

  private static final Logger LOG = Loggers.get(ScmRevisionImpl.class);

  private final CiConfiguration ciConfiguration;
  private final RawScannerProperties scannerConfiguration;
  private final ScmConfiguration scmConfiguration;
  private final InputModuleHierarchy moduleHierarchy;

  public ScmRevisionImpl(CiConfiguration ciConfiguration, RawScannerProperties scannerConfiguration, ScmConfiguration scmConfiguration, InputModuleHierarchy moduleHierarchy) {
    this.ciConfiguration = ciConfiguration;
    this.scannerConfiguration = scannerConfiguration;
    this.scmConfiguration = scmConfiguration;
    this.moduleHierarchy = moduleHierarchy;
  }

  @Override
  public Optional<String> get() {
    Optional<String> revision = Optional.ofNullable(scannerConfiguration.property(SCM_REVISION));
    if (isSet(revision)) {
      return revision;
    }
    revision = ciConfiguration.getScmRevision();
    if (isSet(revision)) {
      return revision;
    }
    ScmProvider scmProvider = scmConfiguration.provider();
    if (scmProvider != null) {
      try {
        revision = Optional.ofNullable(scmProvider.revisionId(moduleHierarchy.root().getBaseDir()));
      } catch (UnsupportedOperationException e) {
        LOG.debug(e.getMessage());
        revision = Optional.empty();
      }
    }
    if (isSet(revision)) {
      return revision;
    }
    return Optional.empty();
  }

  private static boolean isSet(Optional<String> opt) {
    return opt.isPresent() && !isBlank(opt.get());
  }
}
