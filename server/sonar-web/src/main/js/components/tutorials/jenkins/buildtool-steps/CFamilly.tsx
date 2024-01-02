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
import { NumberedListItem } from 'design-system/lib';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { CompilationInfo } from '../../components/CompilationInfo';
import DefaultProjectKey from '../../components/DefaultProjectKey';
import GithubCFamilyExampleRepositories from '../../components/GithubCFamilyExampleRepositories';
import RenderOptions from '../../components/RenderOptions';
import { OSs, TutorialModes } from '../../types';
import { LanguageProps } from '../JenkinsStep';
import CreateJenkinsfileBulletPoint from './CreateJenkinsfileBulletPoint';

const YAML_MAP: Record<OSs, (baseUrl: string) => string> = {
  [OSs.Linux]: (baseUrl) => `node {
  stage('SCM') {
    checkout scm
  }
  stage('Download Build Wrapper') {
    sh "mkdir -p .sonar"
    sh "curl -sSLo build-wrapper-linux-x86.zip ${baseUrl}/static/cpp/build-wrapper-linux-x86.zip"
    sh "unzip -o build-wrapper-linux-x86.zip -d .sonar"
  }
  stage('Build') {
    sh ".sonar/build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir bw-output <your clean build command>"
  }
  stage('SonarQube Analysis') {
    def scannerHome = tool 'SonarScanner';
    withSonarQubeEnv() {
      sh "\${scannerHome}/bin/sonar-scanner -Dsonar.cfamily.build-wrapper-output=bw-output"
    }
  }
}`,
  [OSs.MacOS]: (baseUrl) => `node {
  stage('SCM') {
    checkout scm
  }
  stage('Download Build Wrapper') {
    sh '''
      mkdir -p .sonar
      curl -sSLo build-wrapper-macosx-x86.zip ${baseUrl}/static/cpp/build-wrapper-macosx-x86.zip
      unzip -o build-wrapper-macosx-x86.zip -d .sonar
    '''
  }
  stage('Build') {
    sh '''
      .sonar/build-wrapper-macosx-x86/build-wrapper-macosx-x86 --out-dir bw-output <your clean build command>
    '''
  }
  stage('SonarQube Analysis') {
    def scannerHome = tool 'SonarScanner';
    withSonarQubeEnv() {
      sh "\${scannerHome}/bin/sonar-scanner -Dsonar.cfamily.build-wrapper-output=bw-output"
    }
  }
}`,
  [OSs.Windows]: (baseUrl) => `node {
  stage('SCM') {
    checkout scm
  }
  stage('Download Build Wrapper') {
    powershell '''
      $path = "$HOME/.sonar/build-wrapper-win-x86.zip"
      rm build-wrapper-win-x86 -Recurse -Force -ErrorAction SilentlyContinue
      rm $path -Force -ErrorAction SilentlyContinue
      mkdir $HOME/.sonar
      [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
      (New-Object System.Net.WebClient).DownloadFile(${baseUrl}/static/cpp/build-wrapper-win-x86.zip", $path)
      Add-Type -AssemblyName System.IO.Compression.FileSystem
      [System.IO.Compression.ZipFile]::ExtractToDirectory($path, "$HOME/.sonar")
    '''
  }
  stage('Build') {
    powershell '''
      $env:Path += ";$HOME/.sonar/build-wrapper-win-x86"
      build-wrapper-win-x86-64 --out-dir bw-output <your clean build command>
    '''
  }
  stage('SonarQube Analysis') {
    def scannerHome = tool 'SonarScanner';
    withSonarQubeEnv() {
      powershell "\${scannerHome}/bin/sonar-scanner -Dsonar.cfamily.build-wrapper-output=bw-output"
    }
  }
}`,
};

export default function CFamilly(props: LanguageProps) {
  const { baseUrl, component, onDone } = props;
  const [os, setOs] = React.useState<OSs>(OSs.Linux);

  React.useEffect(() => {
    onDone(os !== undefined);
  }, [os, onDone]);

  return (
    <>
      <DefaultProjectKey component={component} />
      <NumberedListItem>
        {translate('onboarding.build.other.os')}
        <RenderOptions
          label={translate('onboarding.build.other.os')}
          checked={os}
          optionLabelKey="onboarding.build.other.os"
          onCheck={(value) => setOs(value as OSs)}
          options={Object.values(OSs)}
        />
        {os && (
          <GithubCFamilyExampleRepositories
            className="sw-my-4 sw-w-abs-600"
            os={os}
            ci={TutorialModes.Jenkins}
          />
        )}
      </NumberedListItem>
      {os && (
        <CreateJenkinsfileBulletPoint
          alertTranslationKeyPart="onboarding.tutorial.with.jenkins.jenkinsfile.other.step3"
          snippet={YAML_MAP[os](baseUrl)}
        >
          <CompilationInfo />
        </CreateJenkinsfileBulletPoint>
      )}
    </>
  );
}
