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

import { FlagMessage } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { GRADLE_SCANNER_VERSION } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import CodeSnippet from '../../common/CodeSnippet';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import FinishButton from '../components/FinishButton';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import GradleBuildSelection from '../components/GradleBuildSelection';
import RenderOptions from '../components/RenderOptions';
import Step from '../components/Step';
import { BuildTools, GradleBuildDSL, TutorialModes } from '../types';
import PipeCommand from './commands/PipeCommand';

export interface YmlFileStepProps extends WithAvailableFeaturesProps {
  component: Component;
  finished: boolean;
  hasCLanguageFeature: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
}

const mavenSnippet = (key: string, name: string) => `<properties>
  <sonar.projectKey>${key}</sonar.projectKey>
  <sonar.projectName>${name}</sonar.projectName>
  <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
</properties>`;

const gradleSnippet = (key: string, name: string, build: GradleBuildDSL) => {
  const map = {
    [GradleBuildDSL.Groovy]: `plugins {
  id "org.sonarqube" version "${GRADLE_SCANNER_VERSION}"
}

sonar {
  properties {
    property "sonar.projectKey", "${key}"
    property "sonar.projectName", "${name}"
    property "sonar.qualitygate.wait", true 
  }
}`,
    [GradleBuildDSL.Kotlin]: `plugins {
  id ("org.sonarqube") version "${GRADLE_SCANNER_VERSION}"
}

sonar {
  properties {
    property("sonar.projectKey", "${key}")
    property("sonar.projectName", "${name}")
    property("sonar.qualitygate.wait", true)
  }
}`,
  };
  return map[build];
};

const otherSnippet = (key: string) => `sonar.projectKey=${key}
sonar.qualitygate.wait=true
`;

const snippetForBuildTool = {
  [BuildTools.CFamily]: otherSnippet,
  [BuildTools.Gradle]: gradleSnippet,
  [BuildTools.Maven]: mavenSnippet,
  [BuildTools.Other]: otherSnippet,
};

const filenameForBuildTool = {
  [BuildTools.CFamily]: 'sonar-project.properties',
  [BuildTools.Gradle]: GradleBuildDSL.Groovy,
  [BuildTools.Maven]: 'pom.xml',
  [BuildTools.Other]: 'sonar-project.properties',
};

export function YmlFileStep(props: YmlFileStepProps) {
  const { component, hasCLanguageFeature, finished, open } = props;

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
            checked={buildTool}
            label={translate('onboarding.build')}
            onCheck={setBuildTool as (key: string) => void}
            optionLabelKey="onboarding.build"
            options={buildTools}
          />

          {buildTool === BuildTools.CFamily && (
            <GithubCFamilyExampleRepositories
              ci={TutorialModes.GitLabCI}
              className="sw-mb-4 sw-mt-4 sw-w-[600px]"
            />
          )}
        </li>

        {buildTool !== undefined &&
          buildTool !== BuildTools.CFamily &&
          buildTool !== BuildTools.DotNet && (
            <li className="sw-w-[600px]">
              <FormattedMessage
                defaultMessage={translate(
                  `onboarding.tutorial.with.gitlab_ci.project_key.${buildTool}.step2`
                )}
                id={`onboarding.tutorial.with.gitlab_ci.project_key.${buildTool}.step2`}
                values={Object.assign(
                  {
                    file: (
                      <>
                        <code className="rule">{filenameForBuildTool[buildTool]}</code>

                        <ClipboardIconButton
                          className="little-spacer-left"
                          copyValue={filenameForBuildTool[buildTool]}
                        />
                      </>
                    ),
                  },
                  buildTool === BuildTools.Gradle
                    ? {
                        file2: (
                          <>
                            <code className="rule">{GradleBuildDSL.Kotlin}</code>

                            <ClipboardIconButton
                              className="sw-ml-1"
                              copyValue={GradleBuildDSL.Kotlin}
                            />
                          </>
                        ),
                      }
                    : {}
                )}
              />

              {buildTool === BuildTools.Gradle ? (
                <GradleBuildSelection className="sw-mb-4 sw-mt-2">
                  {(build) => (
                    <CodeSnippet
                      snippet={snippetForBuildTool[buildTool](component.key, component.name, build)}
                    />
                  )}
                </GradleBuildSelection>
              ) : (
                <CodeSnippet
                  snippet={snippetForBuildTool[buildTool](component.key, component.name)}
                />
              )}
            </li>
          )}

        {buildTool && (
          <li className="sw-w-[600px]">
            {buildTool !== BuildTools.CFamily && (
              <>
                <div className="sw-mb-4">
                  <FormattedMessage
                    defaultMessage={translate(
                      'onboarding.tutorial.with.gitlab_ci.yaml.description'
                    )}
                    id="onboarding.tutorial.with.gitlab_ci.yaml.description"
                    values={{
                      filename: (
                        <>
                          <code className="rule">
                            {translate('onboarding.tutorial.with.gitlab_ci.yaml.filename')}
                          </code>

                          <ClipboardIconButton
                            className="sw-ml-1"
                            copyValue={translate(
                              'onboarding.tutorial.with.gitlab_ci.yaml.filename'
                            )}
                          />
                        </>
                      ),
                    }}
                  />
                </div>

                <div className="sw-mb-4 sw-w-[600px]">
                  <PipeCommand buildTool={buildTool} projectKey={component.key} />
                </div>

                <FlagMessage className="sw-mb-4" variant="warning">
                  {translate('onboarding.tutorial.with.gitlab_ci.yaml.premium')}
                </FlagMessage>

                <p className="sw-mb-1">
                  {translate('onboarding.tutorial.with.gitlab_ci.yaml.baseconfig')}
                </p>

                <p>{translate('onboarding.tutorial.with.gitlab_ci.yaml.existing')}</p>
              </>
            )}
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
