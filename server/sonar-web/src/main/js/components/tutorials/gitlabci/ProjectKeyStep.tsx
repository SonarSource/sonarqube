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
import { FormattedMessage } from 'react-intl';
import { Button } from '../../../components/controls/buttons';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { GRADLE_SCANNER_VERSION } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import CodeSnippet from '../../common/CodeSnippet';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import RenderOptions from '../components/RenderOptions';
import Step from '../components/Step';
import { BuildTools } from '../types';

export interface ProjectKeyStepProps {
  buildTool?: BuildTools;
  component: Component;
  finished: boolean;
  hasCLanguageFeature: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  setBuildTool: (tool: BuildTools) => void;
}

const mavenSnippet = () => `<properties>
  <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
</properties>`;

const gradleSnippet = (key: string) => `plugins {
  id "org.sonarqube" version "${GRADLE_SCANNER_VERSION}"
}

sonar {
  properties {
    property "sonar.projectKey", "${key}"
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

export function ProjectKeyStep(props: ProjectKeyStepProps) {
  const { buildTool, component, finished, hasCLanguageFeature, open } = props;

  const buildToolSelect = (value: BuildTools) => {
    props.setBuildTool(value);
    if (value === BuildTools.DotNet) {
      props.onDone();
    }
  };

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
            onCheck={buildToolSelect}
            optionLabelKey="onboarding.build"
            options={buildTools}
          />
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
            <CodeSnippet snippet={snippetForBuildTool[buildTool](component.key)} />
          </li>
        )}
      </ol>
      {buildTool !== undefined && <Button onClick={props.onDone}>{translate('continue')}</Button>}
    </div>
  );

  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={renderForm}
      stepNumber={1}
      stepTitle={translate('onboarding.tutorial.with.gitlab_ci.project_key.title')}
    />
  );
}

export default withCLanguageFeature(ProjectKeyStep);
