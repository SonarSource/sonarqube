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
import BuildConfigSelection from '../components/BuildConfigSelection';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import GradleBuildSelection from '../components/GradleBuildSelection';
import { InlineSnippet } from '../components/InlineSnippet';
import { JreRequiredWarning } from '../components/JreRequiredWarning';
import RenderOptions from '../components/RenderOptions';
import { Arch, BuildTools, GradleBuildDSL, OSs, TutorialConfig, TutorialModes } from '../types';
import {
  shouldShowArchSelector,
  shouldShowGithubCFamilyExampleRepositories,
  showJreWarning,
} from '../utils';
import PipeCommand from './commands/PipeCommand';

export interface YmlFileStepProps extends WithAvailableFeaturesProps {
  component: Component;
  hasCLanguageFeature: boolean;
  setDone: (done: boolean) => void;
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
  [BuildTools.Cpp]: otherSnippet,
  [BuildTools.ObjectiveC]: otherSnippet,
  [BuildTools.Gradle]: gradleSnippet,
  [BuildTools.Maven]: mavenSnippet,
  [BuildTools.Other]: otherSnippet,
};

const filenameForBuildTool = {
  [BuildTools.Cpp]: 'sonar-project.properties',
  [BuildTools.ObjectiveC]: 'sonar-project.properties',
  [BuildTools.Gradle]: GradleBuildDSL.Groovy,
  [BuildTools.Maven]: 'pom.xml',
  [BuildTools.Other]: 'sonar-project.properties',
};

const snippetLanguageForBuildTool = {
  [BuildTools.Cpp]: undefined,
  [BuildTools.ObjectiveC]: undefined,
  [BuildTools.Gradle]: undefined,
  [BuildTools.Maven]: 'xml',
  [BuildTools.Other]: undefined,
};

export function YmlFileStep(props: Readonly<YmlFileStepProps>) {
  const { component, hasCLanguageFeature, setDone } = props;
  const [os, setOs] = React.useState<OSs>(OSs.Linux);
  const [arch, setArch] = React.useState<Arch>(Arch.X86_64);

  const [config, setConfig] = React.useState<TutorialConfig>({});
  const { buildTool } = config;

  function onSetConfig(config: TutorialConfig) {
    setConfig(config);
  }

  React.useEffect(() => {
    setDone(Boolean(config.buildTool));
  }, [config.buildTool, setDone]);

  const renderForm = () => (
    <NumberedList>
      <NumberedListItem>
        <BuildConfigSelection
          ci={TutorialModes.GitLabCI}
          config={config}
          supportCFamily={hasCLanguageFeature}
          onSetConfig={onSetConfig}
        />
        {(config.buildTool === BuildTools.Other ||
          config.buildTool === BuildTools.Cpp ||
          config.buildTool === BuildTools.ObjectiveC) && (
          <RenderOptions
            label={translate('onboarding.build.other.os')}
            checked={os}
            onCheck={(value: OSs) => setOs(value)}
            optionLabelKey="onboarding.build.other.os"
            options={[OSs.Linux, OSs.Windows, OSs.MacOS]}
            titleLabelKey="onboarding.build.other.os"
          />
        )}
        {shouldShowArchSelector(os, config) && (
          <RenderOptions
            label={translate('onboarding.build.other.architecture')}
            checked={arch}
            onCheck={(value: Arch) => setArch(value)}
            optionLabelKey="onboarding.build.other.architecture"
            options={[Arch.X86_64, Arch.Arm64]}
            titleLabelKey="onboarding.build.other.architecture"
          />
        )}

        {shouldShowGithubCFamilyExampleRepositories(config) && (
          <GithubCFamilyExampleRepositories
            ci={TutorialModes.GitLabCI}
            className="sw-my-4 sw-w-abs-600"
          />
        )}
      </NumberedListItem>

      {/* Step 2 */}
      {buildTool !== undefined && buildTool !== BuildTools.DotNet && (
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

      {/* Step 3 */}
      {buildTool && (
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
          {showJreWarning(config, arch) && <JreRequiredWarning />}

          <PipeCommand
            buildTool={buildTool}
            projectKey={component.key}
            os={os}
            arch={arch}
            config={config}
          />

          <FlagMessage className="sw-mb-4 sw-mt-2" variant="warning">
            {translate('onboarding.tutorial.with.gitlab_ci.yaml.premium')}
          </FlagMessage>

          <p className="sw-mb-1">
            {translate('onboarding.tutorial.with.gitlab_ci.yaml.baseconfig')}
          </p>

          <p>{translate('onboarding.tutorial.with.gitlab_ci.yaml.existing')}</p>
        </NumberedListItem>
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
