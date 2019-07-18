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
import {
  getSonarcloudAddonYml,
  getSonarcloudAddonYmlRender,
  RequirementJavaBuild
} from '../AnalysisCommandTravis';
import { Props } from '../JavaGradle';

export function JavaGradleTravisSonarCloud(props: Props) {
  const config = `plugins {
  id "org.sonarqube" version "2.7"
}

sonarqube {
  properties {
    sonar.projectKey: ${props.projectKey}
  }
}`;

  const command = `${getSonarcloudAddonYml(props.organization)}

script:
  - ./gradlew sonarqube`;

  const renderCommand = () => (
    <>
      {getSonarcloudAddonYmlRender(props.organization)}
      <br />
      {`  script:
  - ./gradlew sonarqube`}
    </>
  );

  return (
    <div>
      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.setup.title.a')}
      </h2>

      <RequirementJavaBuild />

      <hr className="no-horizontal-margins" />

      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.setup.title.b')}
      </h2>

      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.java.gradle.text.1.sonarcloud')}
        id="onboarding.analysis.java.gradle.text.1.sonarcloud"
        values={{
          file: <code>build.gradle</code>,
          plugin: <code>org.sonarqube</code>
        }}
      />

      <CodeSnippet snippet={config} />

      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.java.gradle.text.2.sonarcloud')}
        id="onboarding.analysis.java.gradle.text.2.sonarcloud"
        values={{
          file: <code>.travis.yml</code>
        }}
      />

      <CodeSnippet render={renderCommand} snippet={command} />

      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.sq_scanner.docs_use_case')}
        id="onboarding.analysis.sq_scanner.docs_use_case"
        values={{
          link: (
            <a
              href="http://redirect.sonarsource.com/doc/gradle.html"
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.analysis.sqscanner.docs.gradle.title')}
            </a>
          ),
          useCaseLink: (
            <a
              href="https://github.com/SonarSource/sq-com_example_java-gradle-travis"
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.analysis.sqscanner.docs.gradle.example_project.title')}
            </a>
          )
        }}
      />
    </div>
  );
}
