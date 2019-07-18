/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import { ProjectAnalysisModes } from '../../ProjectAnalysisStepFromBuildTool';
import { JavaCustomProps, RenderCustomContent } from './JavaMavenCustom';

export default function JavaGradleCustom(props: JavaCustomProps) {
  const suffix = props.mode === ProjectAnalysisModes.CI ? '.ci' : '';
  const config = 'plugins {\n  id "org.sonarqube" version "2.7"\n}';

  const command = [
    './gradlew sonarqube',
    props.projectKey && `-Dsonar.projectKey=${props.projectKey}`,
    props.organization && `-Dsonar.organization=${props.organization}`,
    `-Dsonar.host.url=${props.host}`,
    `-Dsonar.login=${props.token}`
  ];

  return (
    <div>
      <h4 className="spacer-bottom">
        {translate(`onboarding.analysis.java.gradle.header${suffix}`)}
      </h4>

      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.java.gradle.text.1.sonarcloud')}
        id="onboarding.analysis.java.gradle.text.1.sonarcloud"
        values={{
          file: <code>build.gradle</code>,
          plugin: <code>org.sonarqube</code>
        }}
      />

      <CodeSnippet snippet={config} />

      <p className="spacer-top spacer-bottom markdown">
        {translate('onboarding.analysis.java.gradle.text.2')}
      </p>

      <RenderCustomContent
        command={command}
        linkText="onboarding.analysis.java.gradle.docs_link"
        linkUrl="http://redirect.sonarsource.com/doc/gradle.html"
        onDone={props.onDone}
        toggleModal={props.toggleModal}
      />
    </div>
  );
}
