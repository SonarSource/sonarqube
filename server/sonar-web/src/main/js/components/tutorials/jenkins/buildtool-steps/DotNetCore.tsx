/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { CodeSnippet, NumberedListItem } from '~design-system';
import SentenceWithFilename from '../../components/SentenceWithFilename';
import { OSs } from '../../types';
import { DotNetCoreFrameworkProps, OSDotNet } from './DotNet';
import DotNetPrereqsScanner from './DotNetPrereqsScanner';

const OSS_DEP: { [key in OSDotNet]: { pathSeparator: string; shell: string } } = {
  [OSs.Linux]: { shell: 'sh', pathSeparator: '/' },
  [OSs.Windows]: { shell: 'bat', pathSeparator: '\\\\' },
};

const jenkinsfileSnippet = (key: string, shell: OSDotNet) => `node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def scannerHome = tool 'SonarScanner for .NET'
    withSonarQubeEnv() {
      ${OSS_DEP[shell].shell} "dotnet \${scannerHome}${OSS_DEP[shell].pathSeparator}SonarScanner.MSBuild.dll begin /k:\\"${key}\\""
      ${OSS_DEP[shell].shell} "dotnet build"
      ${OSS_DEP[shell].shell} "dotnet \${scannerHome}${OSS_DEP[shell].pathSeparator}SonarScanner.MSBuild.dll end"
    }
  }
}
`;

export default function DotNetCore({ component, os }: DotNetCoreFrameworkProps) {
  return (
    <>
      <DotNetPrereqsScanner />
      <NumberedListItem>
        <SentenceWithFilename
          filename="Jenkinsfile"
          translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.jenkinsfile_step"
        />
        <CodeSnippet
          className="sw-p-6"
          language="groovy"
          snippet={jenkinsfileSnippet(component.key, os)}
        />
      </NumberedListItem>
    </>
  );
}
