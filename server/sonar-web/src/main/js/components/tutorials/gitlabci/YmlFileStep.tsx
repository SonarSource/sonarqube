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
import {
  ClipboardIconButton,
  CodeSnippet,
  FlagMessage,
  NumberedList,
  NumberedListItem,
  TutorialStep,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { GRADLE_SCANNER_VERSION } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import GradleBuildSelection from '../components/GradleBuildSelection';
import { InlineSnippet } from '../components/InlineSnippet';
import RenderOptions from '../components/RenderOptions';
import { BuildTools, GradleBuildDSL, TutorialModes } from '../types';
import PipeCommand from './commands/PipeCommand';

export interface YmlFileStepProps extends WithAvailableFeaturesProps {
  component: Component;
  hasCLanguageFeature: boolean;
  setDone: (doneStatus: boolean) => void;
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

const snippetLanguageForBuildTool = {
  [BuildTools.CFamily]: undefined,
  [BuildTools.Gradle]: undefined,
  [BuildTools.Maven]: 'xml',
  [BuildTools.Other]: undefined,
};

export function YmlFileStep(props: YmlFileStepProps) {
  const { component, hasCLanguageFeature } = props;

  const [buildTool, setBuildTool] = React.useState<BuildTools>();

  const buildTools = [BuildTools.Maven, BuildTools.Gradle, BuildTools.DotNet];

  if (hasCLanguageFeature) {
    buildTools.push(BuildTools.CFamily);
  }

  buildTools.push(BuildTools.Other);

  const renderForm = () => (
    <NumberedList>
      <NumberedListItem>
        {translate('onboarding.build')}

        <RenderOptions
          checked={buildTool}
          label={translate('onboarding.build')}
          onCheck={setBuildTool as (key: string) => void}
          optionLabelKey="onboarding.build"
          options={buildTools}
          setDone={props.setDone}
        />

        {buildTool === BuildTools.CFamily && (
          <GithubCFamilyExampleRepositories
            ci={TutorialModes.GitLabCI}
            className="sw-my-4 sw-w-abs-600"
          />
        )}
      </NumberedListItem>

      {buildTool !== undefined &&
        buildTool !== BuildTools.CFamily &&
        buildTool !== BuildTools.DotNet && (
          <NumberedListItem>
            <FormattedMessage
              defaultMessage={translate(
                `onboarding.tutorial.with.gitlab_ci.project_key.${buildTool}.step2`,
              )}
              id={`onboarding.tutorial.with.gitlab_ci.project_key.${buildTool}.step2`}
              values={Object.assign(
                {
                  file: (
                    <>
                      <InlineSnippet snippet={filenameForBuildTool[buildTool]} />

                      <ClipboardIconButton
                        className="sw-ml-2 sw-align-sub"
                        copyValue={filenameForBuildTool[buildTool]}
                      />
                    </>
                  ),
                },
                buildTool === BuildTools.Gradle
                  ? {
                      file2: (
                        <>
                          <InlineSnippet snippet={GradleBuildDSL.Kotlin} />

                          <ClipboardIconButton
                            className="sw-ml-2 sw-align-sub"
                            copyValue={GradleBuildDSL.Kotlin}
                          />
                        </>
                      ),
                    }
                  : {},
              )}
            />
            {buildTool === BuildTools.Gradle ? (
              <GradleBuildSelection className="sw-mb-4 sw-mt-2">
                {(build) => (
                  <CodeSnippet
                    className="sw-p-6"
                    language="gradle"
                    snippet={snippetForBuildTool[buildTool](component.key, component.name, build)}
                  />
                )}
              </GradleBuildSelection>
            ) : (
              <CodeSnippet
                className="sw-p-6"
                language={snippetLanguageForBuildTool[buildTool]}
                snippet={snippetForBuildTool[buildTool](component.key, component.name)}
              />
            )}
          </NumberedListItem>
        )}

      {buildTool && (
        <>
          {buildTool !== BuildTools.CFamily && (
            <NumberedListItem>
              <FormattedMessage
                defaultMessage={translate('onboarding.tutorial.with.gitlab_ci.yaml.description')}
                id="onboarding.tutorial.with.gitlab_ci.yaml.description"
                values={{
                  filename: (
                    <>
                      <InlineSnippet
                        snippet={translate('onboarding.tutorial.with.gitlab_ci.yaml.filename')}
                      />

                      <ClipboardIconButton
                        className="sw-ml-2 sw-align-sub"
                        copyValue={translate('onboarding.tutorial.with.gitlab_ci.yaml.filename')}
                      />
                    </>
                  ),
                }}
              />

              <PipeCommand buildTool={buildTool} projectKey={component.key} />

              <FlagMessage className="sw-mb-4 sw-mt-2" variant="warning">
                {translate('onboarding.tutorial.with.gitlab_ci.yaml.premium')}
              </FlagMessage>

              <p className="sw-mb-1">
                {translate('onboarding.tutorial.with.gitlab_ci.yaml.baseconfig')}
              </p>

              <p>{translate('onboarding.tutorial.with.gitlab_ci.yaml.existing')}</p>
            </NumberedListItem>
          )}
        </>
      )}
    </NumberedList>
  );

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.gitlab_ci.yaml.title')}>
      {renderForm()}
    </TutorialStep>
  );
}

export default withCLanguageFeature(withAvailableFeatures(YmlFileStep));
