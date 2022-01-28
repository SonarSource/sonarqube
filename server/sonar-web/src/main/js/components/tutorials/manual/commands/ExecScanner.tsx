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
import { OSs } from '../../types';
import { quote } from '../../utils';
import DoneNextSteps from '../DoneNextSteps';

export interface ExecScannerProps {
  component: Component;
  host: string;
  os: OSs;
  token: string;
  cfamily?: boolean;
}

export default function ExecScanner(props: ExecScannerProps) {
  const { host, os, component, token, cfamily } = props;

  const q = quote(os);
  const command = [
    os === OSs.Windows ? 'sonar-scanner.bat' : 'sonar-scanner',
    '-D' + q(`sonar.projectKey=${component.key}`),
    '-D' + q('sonar.sources=.'),
    cfamily ? '-D' + q('sonar.cfamily.build-wrapper-output=bw-output') : undefined,
    '-D' + q(`sonar.host.url=${host}`),
    '-D' + q(`sonar.login=${token}`)
  ];

  return (
    <div>
      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.sq_scanner.execute')}
      </h4>
      <InstanceMessage message={translate('onboarding.analysis.sq_scanner.execute.text')}>
        {transformedMessage => <p className="spacer-bottom markdown">{transformedMessage}</p>}
      </InstanceMessage>
      <CodeSnippet isOneLine={os === OSs.Windows} snippet={command} />
      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.sq_scanner.docs')}
          id="onboarding.analysis.sq_scanner.docs"
          values={{
            link: (
              <Link to="/documentation/analysis/scan/sonarscanner/" target="_blank">
                {translate('onboarding.analysis.sq_scanner.docs_link')}
              </Link>
            )
          }}
        />
      </p>
      <DoneNextSteps component={component} />
    </div>
  );
}
