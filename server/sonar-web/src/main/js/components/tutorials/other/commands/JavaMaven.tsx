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

import { CodeSnippet, Link, SubHeading } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import InstanceMessage from '../../../common/InstanceMessage';
import DoneNextSteps from '../DoneNextSteps';

export interface JavaMavenProps {
  component: Component;
  baseUrl: string;
  token: string;
}

export default function JavaMaven(props: JavaMavenProps) {
  const { baseUrl, component, token } = props;
  const command = [
    'mvn clean verify sonar:sonar',
    `-Dsonar.projectKey=${component.key}`,
    `-Dsonar.projectName='${component.name}'`,
    `-Dsonar.host.url=${baseUrl}`,
    `-Dsonar.token=${token}`,
  ];

  const docUrl = useDocUrl(DocLink.SonarScannerMaven);

  return (
    <div>
      <SubHeading className="sw-mb-2">
        {translate('onboarding.analysis.java.maven.header')}
      </SubHeading>
      <p className="sw-mb-2">
        <InstanceMessage message={translate('onboarding.analysis.java.maven.text')} />
      </p>
      <CodeSnippet className="sw-p-4" snippet={command} />
      <p className="sw-mt-4">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: <Link to={docUrl}>{translate('onboarding.analysis.java.maven.docs_link')}</Link>,
          }}
        />
      </p>
      <DoneNextSteps component={component} />
    </div>
  );
}
