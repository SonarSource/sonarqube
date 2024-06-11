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

import { CodeSnippet, Link, Note, SubHeading } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { GRADLE_SCANNER_VERSION } from '../../../../helpers/constants';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import InstanceMessage from '../../../common/InstanceMessage';
import GradleBuildSelection from '../../components/GradleBuildSelection';
import { InlineSnippet } from '../../components/InlineSnippet';
import { GradleBuildDSL } from '../../types';
import DoneNextSteps from '../DoneNextSteps';

export interface JavaGradleProps {
  baseUrl: string;
  component: Component;
  token: string;
}

const config = {
  [GradleBuildDSL.Groovy]: {
    lang: 'groovy',
    snippet: `plugins {
  id "org.sonarqube" version "${GRADLE_SCANNER_VERSION}"
}`,
  },
  [GradleBuildDSL.Kotlin]: {
    lang: 'kts',
    snippet: `plugins {
  id("org.sonarqube") version "${GRADLE_SCANNER_VERSION}"
}`,
  },
};

export default function JavaGradle(props: JavaGradleProps) {
  const { baseUrl, component, token } = props;

  const docUrl = useDocUrl(DocLink.SonarScannerGradle);

  const command = [
    './gradlew sonar',
    `-Dsonar.projectKey=${component.key}`,
    `-Dsonar.projectName='${component.name}'`,
    `-Dsonar.host.url=${baseUrl}`,
    `-Dsonar.token=${token}`,
  ];

  return (
    <div>
      <SubHeading className="sw-mb-2">
        {translate('onboarding.analysis.java.gradle.header')}
      </SubHeading>
      <InstanceMessage message={translate('onboarding.analysis.java.gradle.text.1')}>
        {(transformedMessage) => (
          <p className="sw-mb-2">
            <FormattedMessage
              defaultMessage={transformedMessage}
              id="onboarding.analysis.java.gradle.text.1"
              values={{
                plugin_code: <InlineSnippet snippet="org.sonarqube" />,
                groovy: <InlineSnippet snippet={GradleBuildDSL.Groovy} />,
                kotlin: <InlineSnippet snippet={GradleBuildDSL.Kotlin} />,
              }}
            />
          </p>
        )}
      </InstanceMessage>
      <GradleBuildSelection className="sw-mt-4 sw-mb-4">
        {(build) => (
          <CodeSnippet
            language={config[build].lang}
            className="sw-p-4"
            snippet={config[build].snippet}
          />
        )}
      </GradleBuildSelection>
      <p className="sw-mb-4">
        <Note as="em">
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.java.gradle.latest_version')}
            id="onboarding.analysis.java.gradle.latest_version"
            values={{
              link: <Link to={docUrl}>{translate('here')}</Link>,
            }}
          />
        </Note>
      </p>
      <p className="sw-mt-2 sw-mb-2">{translate('onboarding.analysis.java.gradle.text.2')}</p>
      <CodeSnippet className="sw-p-4" snippet={command} />
      <p className="sw-mt-4">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: <Link to={docUrl}>{translate('onboarding.analysis.java.gradle.docs_link')}</Link>,
          }}
        />
      </p>
      <DoneNextSteps />
    </div>
  );
}
