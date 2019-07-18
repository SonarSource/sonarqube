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
import * as React from 'react';
import { getSonarcloudAddonYml, getSonarcloudAddonYmlRender } from '../AnalysisCommandTravis';
import { Props } from '../ClangGCC';
import { CommonTravisSonarCloud } from './utils';

export function ClangGCCTravisSonarCloud(props: Props) {
  const command = `${getSonarcloudAddonYml(props.organization)}

script:
  - make clean
  # Wraps the compilation with the Build Wrapper to generate configuration (used
  # later by the SonarQube Scanner) into the "bw-output" folder
  - build-wrapper-linux-x86-64 --out-dir bw-output make all
  # Execute some tests
  - make test
  # And finally run the SonarQube analysis - read the "sonar-project.properties"
  # file to see the specific configuration
  - sonar-scanner`;

  const renderCommand = () => (
    <>
      {getSonarcloudAddonYmlRender(props.organization)}
      <br />
      {`script:
  - make clean
  # Wraps the compilation with the Build Wrapper to generate configuration (used
  # later by the SonarQube Scanner) into the "bw-output" folder
  - build-wrapper-linux-x86-64 --out-dir bw-output make all
  # Execute some tests
  - make test
  # And finally run the SonarQube analysis - read the "sonar-project.properties"
  # file to see the specific configuration
  - sonar-scanner`}
    </>
  );

  return <CommonTravisSonarCloud command={command} renderCommand={renderCommand} />;
}
