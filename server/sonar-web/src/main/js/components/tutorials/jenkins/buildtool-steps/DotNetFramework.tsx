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
import { CodeSnippet, NumberedListItem } from 'design-system';
import * as React from 'react';
import SentenceWithFilename from '../../components/SentenceWithFilename';
import { DotNetCoreFrameworkProps } from './DotNet';
import DotNetPrereqsMSBuild from './DotNetPrereqsMSBuild';
import DotNetPrereqsScanner from './DotNetPrereqsScanner';

const jenkinsfileSnippet = (key: string) => `node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def msbuildHome = tool 'Default MSBuild'
    def scannerHome = tool 'SonarScanner for MSBuild'
    withSonarQubeEnv() {
      bat "\\"\${scannerHome}\\\\SonarScanner.MSBuild.exe\\" begin /k:\\"${key}\\""
      bat "\\"\${msbuildHome}\\\\MSBuild.exe\\" /t:Rebuild"
      bat "\\"\${scannerHome}\\\\SonarScanner.MSBuild.exe\\" end"
    }
  }
}
`;

export default function DotNetFramework({ component }: DotNetCoreFrameworkProps) {
  return (
    <>
      <DotNetPrereqsScanner />
      <DotNetPrereqsMSBuild />
      <NumberedListItem>
        <SentenceWithFilename
          filename="Jenkinsfile"
          translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.jenkinsfile_step"
        />
        <CodeSnippet
          className="sw-ml-8 sw-p-6"
          language="groovy"
          snippet={jenkinsfileSnippet(component.key)}
        />
      </NumberedListItem>
    </>
  );
}
