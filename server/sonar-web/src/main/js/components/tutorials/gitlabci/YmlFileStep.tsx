/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { GRADLE_SCANNER_VERSION } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import CodeSnippet from '../../common/CodeSnippet';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import FinishButton from '../components/FinishButton';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import RenderOptions from '../components/RenderOptions';
import Step from '../components/Step';
import { BuildTools, TutorialModes } from '../types';
import PipeCommand from './commands/PipeCommand';

export interface YmlFileStepProps extends WithAvailableFeaturesProps {
  finished: boolean;
  component: Component;
  hasCLanguageFeature: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  mainBranchName: string;
}

const mavenSnippet = () => `<properties>
  <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
</properties>`;

const gradleSnippet = (key: string, name: string) => `plugins {
  id "org.sonarqube" version "${GRADLE_SCANNER_VERSION}"
}

sonar {
  properties {
    property "sonar.projectKey", "${key}"
    property "sonar.projectName", "${name}"
    property "sonar.qualitygate.wait", true 
  }
}`;

const otherSnippet = (key: string) => `sonar.projectKey=${key}
sonar.qualitygate.wait=true
`;

const snippetForBuildTool = {
  [BuildTools.Maven]: mavenSnippet,
  [BuildTools.Gradle]: gradleSnippet,
  [BuildTools.CFamily]: otherSnippet,
  [BuildTools.Other]: otherSnippet,
};

const filenameForBuildTool = {
  [BuildTools.Maven]: 'pom.xml',
  [BuildTools.Gradle]: 'build.gradle',
  [BuildTools.CFamily]: 'sonar-project.properties',
  [BuildTools.Other]: 'sonar-project.properties',
};

export function YmlFileStep(props: YmlFileStepProps) {
  const { open, finished, mainBranchName, hasCLanguageFeature, component } = props;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);

  const [buildTool, setBuildTool] = React.useState<BuildTools>();

  const buildTools = [BuildTools.Maven, BuildTools.Gradle, BuildTools.DotNet];
  if (hasCLanguageFeature) {
    buildTools.push(BuildTools.CFamily);
  }
  buildTools.push(BuildTools.Other);

  const renderForm = () => (
    <div className="boxed-group-inner">
      <ol className="list-styled">
        <li>
          {translate('onboarding.build')}
          <RenderOptions
            label={translate('onboarding.build')}
            checked={buildTool}
            onCheck={setBuildTool as (key: string) => void}
            optionLabelKey="onboarding.build"
            options={buildTools}
          />
          {buildTool === BuildTools.CFamily && (
            <GithubCFamilyExampleRepositories
              className="big-spacer-bottom big-spacer-top abs-width-600"
              ci={TutorialModes.GitLabCI}
            />
          )}
        </li>
        {buildTool !== undefined && buildTool !== BuildTools.DotNet && (
          <li className="abs-width-600">
            <FormattedMessage
              defaultMessage={translate(
                `onboarding.tutorial.with.gitlab_ci.project_key.${buildTool}.step2`
              )}
              id={`onboarding.tutorial.with.gitlab_ci.project_key.${buildTool}.step2`}
              values={{
                file: (
                  <>
                    <code className="rule">{filenameForBuildTool[buildTool]}</code>
                    <ClipboardIconButton
                      className="little-spacer-left"
                      copyValue={filenameForBuildTool[buildTool]}
                    />
                  </>
                ),
              }}
            />
            <CodeSnippet snippet={snippetForBuildTool[buildTool](component.key, component.name)} />
          </li>
        )}
        {buildTool && (
          <li className="abs-width-600">
            <div className="big-spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('onboarding.tutorial.with.gitlab_ci.yaml.description')}
                id="onboarding.tutorial.with.gitlab_ci.yaml.description"
                values={{
                  filename: (
                    <>
                      <code className="rule">
                        {translate('onboarding.tutorial.with.gitlab_ci.yaml.filename')}
                      </code>
                      <ClipboardIconButton
                        className="little-spacer-left"
                        copyValue={translate('onboarding.tutorial.with.gitlab_ci.yaml.filename')}
                      />
                    </>
                  ),
                }}
              />
            </div>
            <div className="big-spacer-bottom abs-width-600">
              <PipeCommand
                buildTool={buildTool}
                branchesEnabled={branchSupportEnabled}
                mainBranchName={mainBranchName}
                projectKey={component.key}
                projectName={component.name}
              />
            </div>
            <p className="little-spacer-bottom">
              {branchSupportEnabled
                ? translate('onboarding.tutorial.with.gitlab_ci.yaml.baseconfig')
                : translate('onboarding.tutorial.with.gitlab_ci.yaml.baseconfig.no_branches')}
            </p>
            <p>{translate('onboarding.tutorial.with.gitlab_ci.yaml.existing')}</p>
            <FinishButton onClick={props.onDone} />
          </li>
        )}
      </ol>
    </div>
  );

  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={renderForm}
      stepNumber={2}
      stepTitle={translate('onboarding.tutorial.with.gitlab_ci.yaml.title')}
    />
  );
}

export default withCLanguageFeature(withAvailableFeatures(YmlFileStep));
