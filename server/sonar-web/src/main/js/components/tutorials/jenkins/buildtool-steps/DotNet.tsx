/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import CodeSnippet from '../../../common/CodeSnippet';
import SentenceWithFilename from '../../components/SentenceWithFilename';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';

export interface DotNetProps {
  component: T.Component;
}

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

export default function DotNet({ component }: DotNetProps) {
  return (
    <li className="abs-width-600">
      <SentenceWithFilename
        filename="Jenkinsfile"
        translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.jenkinsfile_step"
      />
      <Alert className="spacer-top" variant="info">
        <p className="text-middle">
          <SentenceWithHighlights
            highlightKeys={['default_msbuild', 'default_scanner', 'in_jenkins']}
            translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.step2.replace"
          />
          <HelpTooltip
            className="little-spacer-left"
            overlay={
              <>
                <p className="spacer-bottom">
                  <SentenceWithHighlights
                    highlightKeys={['path']}
                    translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.step2.help1"
                  />
                </p>
                <p className="spacer-bottom">
                  <SentenceWithHighlights
                    highlightKeys={['path', 'name']}
                    translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.step2.help2"
                  />
                </p>
                <p>
                  <SentenceWithHighlights
                    highlightKeys={['path', 'name']}
                    translationKey="onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.step2.help3"
                  />
                </p>
              </>
            }
          />
        </p>
      </Alert>
      <CodeSnippet snippet={jenkinsfileSnippet(component.key)} />
    </li>
  );
}
