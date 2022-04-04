/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { ClipboardIconButton } from 'sonar-ui-common/components/controls/clipboard';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../common/CodeSnippet';
import RenderOptions from '../components/RenderOptions';
import Step from '../components/Step';
import { BuildTools } from '../types';
import { GitlabBuildTools, GITLAB_BUILDTOOLS_LIST } from './types';

export interface ProjectKeyStepProps {
  buildTool?: GitlabBuildTools;
  component: T.Component;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  setBuildTool: (tool: GitlabBuildTools) => void;
}

const mavenSnippet = (key: string) => `<properties>
  <sonar.projectKey>${key}</sonar.projectKey>
  <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
</properties>`;

const gradleSnippet = (key: string) => `plugins {
  id "org.sonarqube" version "3.0"
}

sonarqube {
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
  [BuildTools.Other]: otherSnippet
};

const filenameForBuildTool = {
  [BuildTools.Maven]: 'pom.xml',
  [BuildTools.Gradle]: 'build.gradle',
  [BuildTools.Other]: 'sonar-project.properties'
};

export default function ProjectKeyStep(props: ProjectKeyStepProps) {
  const { buildTool, component, finished, open } = props;

  const buildToolSelect = (value: GitlabBuildTools) => {
    props.setBuildTool(value);
    if (value === BuildTools.DotNet) {
      props.onDone();
    }
  };

  const renderForm = () => (
    <div className="boxed-group-inner">
      <ol className="list-styled">
        <li>
          {translate('onboarding.build')}
          <RenderOptions
            checked={buildTool}
            name="buildtool"
            onCheck={buildToolSelect}
            optionLabelKey="onboarding.build"
            options={GITLAB_BUILDTOOLS_LIST}
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
                )
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
