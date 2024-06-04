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
import { OSs } from '../../types';
import { quote } from '../../utils';
import DoneNextSteps from '../DoneNextSteps';

export interface ExecScannerProps {
  baseUrl: string;
  cfamily?: boolean;
  component: Component;
  isLocal: boolean;
  os: OSs;
  token: string;
}

export default function ExecScanner(props: ExecScannerProps) {
  const { baseUrl, os, isLocal, component, token, cfamily } = props;

  const docUrl = useDocUrl(DocLink.SonarScanner);

  const q = quote(os);
  const command = [
    os === OSs.Windows ? 'sonar-scanner.bat' : 'sonar-scanner',
    '-D' + q(`sonar.projectKey=${component.key}`),
    '-D' + q('sonar.sources=.'),
    cfamily
      ? '-D' + q('sonar.cfamily.compile-commands=bw-output/compile_commands.json')
      : undefined,
    '-D' + q(`sonar.host.url=${baseUrl}`),
    isLocal ? '-D' + q(`sonar.token=${token}`) : undefined,
  ];

  return (
    <div>
      <SubHeading className="sw-mt-4 sw-mb-2">
        {translate('onboarding.analysis.sq_scanner.execute')}
      </SubHeading>
      <InstanceMessage message={translate('onboarding.analysis.sq_scanner.execute.text')}>
        {(transformedMessage) => <p className="sw-mb-2">{transformedMessage}</p>}
      </InstanceMessage>
      <CodeSnippet className="sw-p-4" isOneLine={os === OSs.Windows} snippet={command} />
      <p className="sw-mt-4">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.sq_scanner.docs')}
          id="onboarding.analysis.sq_scanner.docs"
          values={{
            link: <Link to={docUrl}>{translate('onboarding.analysis.sq_scanner.docs_link')}</Link>,
          }}
        />
      </p>
      <DoneNextSteps component={component} />
    </div>
  );
}
