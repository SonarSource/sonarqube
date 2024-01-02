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
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { getHostUrl } from '../../../../helpers/urls';
import CodeSnippet from '../../../common/CodeSnippet';
import { CompilationInfo } from '../../components/CompilationInfo';
import GithubCFamilyExampleRepositories from '../../components/GithubCFamilyExampleRepositories';
import RenderOptions from '../../components/RenderOptions';
import SentenceWithHighlights from '../../components/SentenceWithHighlights';
import { BuildTools, OSs, TutorialModes } from '../../types';
import AlertClassicEditor from './AlertClassicEditor';
import PrepareAnalysisCommand, { PrepareType } from './PrepareAnalysisCommand';
import PublishSteps from './PublishSteps';

export interface ClangGCCProps {
  projectKey: string;
  onStepValidationChange: (isValid: boolean) => void;
}

type OsConstant = {
  [key in OSs]: {
    highlightScriptKey: string;
    script: string;
    scriptBuild: string;
  };
};

export default function ClangGCC(props: ClangGCCProps) {
  const { projectKey } = props;
  const [os, setOs] = React.useState<OSs | undefined>();
  const host = getHostUrl();

  const codeSnippetDownload: OsConstant = {
    [OSs.Linux]: {
      script: `curl '${host}/static/cpp/build-wrapper-linux-x86.zip' --output build-wrapper.zip
unzip build-wrapper.zip`,
      highlightScriptKey:
        'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build_wrapper.ccpp.nix',
      scriptBuild:
        './build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir bw-output <your build command here>',
    },
    [OSs.Windows]: {
      script: `Invoke-WebRequest -Uri '${host}/static/cpp/build-wrapper-win-x86.zip' -OutFile 'build-wrapper.zip'
Expand-Archive -Path 'build-wrapper.zip' -DestinationPath '.'`,
      highlightScriptKey:
        'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build_wrapper.ccpp.win',
      scriptBuild:
        'build-wrapper-win-x86/build-wrapper-win-x86-64.exe --out-dir bw-output <your build command here>',
    },
    [OSs.MacOS]: {
      script: `curl '${host}/static/cpp/build-wrapper-macosx-x86.zip' --output build-wrapper.zip
unzip build-wrapper.zip`,
      highlightScriptKey:
        'onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build_wrapper.ccpp.nix',
      scriptBuild:
        './build-wrapper-macos-x86/build-wrapper-macos-x86 --out-dir bw-output <your build command here>',
    },
  };

  React.useEffect(() => {
    if (os) {
      props.onStepValidationChange(true);
    } else {
      props.onStepValidationChange(false);
    }
  }, [os, props.onStepValidationChange]);

  const handlOsChange = (value: OSs) => {
    setOs(value);
  };

  return (
    <>
      <span className="big-spacer-top display-block">
        {translate('onboarding.tutorial.with.azure_pipelines.os')}
      </span>
      <RenderOptions
        label={translate('onboarding.tutorial.with.azure_pipelines.os')}
        checked={os}
        onCheck={handlOsChange}
        optionLabelKey="onboarding.build.other.os"
        options={Object.values(OSs)}
      />

      {os && (
        <>
          <GithubCFamilyExampleRepositories
            className="big-spacer-top abs-width-600"
            os={os}
            ci={TutorialModes.AzurePipelines}
          />
          <AlertClassicEditor />
          <ol className="list-styled big-spacer-top">
            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build_wrapper.ccpp"
                highlightPrefixKeys="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare"
                highlightKeys={['pipeline']}
              />
              <ul className="list-styled list-alpha spacer-top">
                <li>
                  <SentenceWithHighlights
                    translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build_wrapper.ccpp.script"
                    highlightPrefixKeys={codeSnippetDownload[os].highlightScriptKey}
                    highlightKeys={['task', 'inline']}
                  />
                  <CodeSnippet snippet={codeSnippetDownload[os].script} />
                </li>
              </ul>
            </li>

            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare.ccpp"
                highlightPrefixKeys="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.prepare"
                highlightKeys={['task', 'before']}
              />
              <PrepareAnalysisCommand
                buildTool={BuildTools.CFamily}
                kind={PrepareType.StandAlone}
                projectKey={projectKey}
              />
            </li>

            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build.ccpp"
                highlightKeys={['task']}
              />
              <ul className="list-styled list-alpha spacer-top">
                <li>
                  <SentenceWithHighlights
                    translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.build_script.ccpp"
                    highlightKeys={['build_wrapper']}
                  />
                  <CodeSnippet snippet={codeSnippetDownload[os].scriptBuild} />
                  <CompilationInfo />
                </li>
              </ul>
            </li>

            <li>
              <SentenceWithHighlights
                translationKey="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run.ccpp"
                highlightPrefixKeys="onboarding.tutorial.with.azure_pipelines.BranchAnalysis.run"
                highlightKeys={['task', 'after']}
              />
            </li>
            <PublishSteps />
          </ol>
        </>
      )}
    </>
  );
}
