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

import DefaultProjectKey from '../../components/DefaultProjectKey';
import { LanguageProps } from '../JenkinsStep';
import CreateJenkinsfileBulletPoint from './CreateJenkinsfileBulletPoint';

const JENKINSFILE_SNIPPET = `node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def scannerHome = tool 'SonarScanner';
    withSonarQubeEnv() {
      sh "\${scannerHome}/bin/sonar-scanner"
    }
  }
}`;

export default function Other(props: LanguageProps) {
  const { component } = props;
  return (
    <>
      <DefaultProjectKey component={component} />
      <CreateJenkinsfileBulletPoint
        alertTranslationKeyPart="onboarding.tutorial.with.jenkins.jenkinsfile.other.step3"
        snippet={JENKINSFILE_SNIPPET}
      />
    </>
  );
}
