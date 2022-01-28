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
import { Link } from 'react-router';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import CodeSnippet from '../../../common/CodeSnippet';
import InstanceMessage from '../../../common/InstanceMessage';
import DoneNextSteps from '../DoneNextSteps';

export interface JavaGradleProps {
  component: Component;
  host: string;
  token: string;
}

export default function JavaGradle(props: JavaGradleProps) {
  const { host, component, token } = props;
  const config = 'plugins {\n  id "org.sonarqube" version "3.3"\n}';

  const command = [
    './gradlew sonarqube',
    `-Dsonar.projectKey=${component.key}`,
    `-Dsonar.host.url=${host}`,
    `-Dsonar.login=${token}`
  ];

  return (
    <div>
      <h4 className="spacer-bottom">{translate('onboarding.analysis.java.gradle.header')}</h4>
      <InstanceMessage message={translate('onboarding.analysis.java.gradle.text.1')}>
        {transformedMessage => (
          <p className="spacer-bottom markdown">
            <FormattedMessage
              defaultMessage={transformedMessage}
              id="onboarding.analysis.java.gradle.text.1"
              values={{
                plugin_code: <code>org.sonarqube</code>,
                filename: <code>build.gradle</code>
              }}
            />
          </p>
        )}
      </InstanceMessage>
      <CodeSnippet snippet={config} />
      <p className="big-spacer-bottom markdown">
        <em className="small text-muted">
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.java.gradle.latest_version')}
            id="onboarding.analysis.java.gradle.latest_version"
            values={{
              link: (
                <Link to="/documentation/analysis/scan/sonarscanner-for-gradle/" target="_blank">
                  {translate('here')}
                </Link>
              )
            }}
          />
        </em>
      </p>
      <p className="spacer-top spacer-bottom markdown">
        {translate('onboarding.analysis.java.gradle.text.2')}
      </p>
      <CodeSnippet snippet={command} />
      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: (
              <Link to="/documentation/analysis/scan/sonarscanner-for-gradle/" target="_blank">
                {translate('onboarding.analysis.java.gradle.docs_link')}
              </Link>
            )
          }}
        />
      </p>
      <DoneNextSteps component={component} />
    </div>
  );
}
