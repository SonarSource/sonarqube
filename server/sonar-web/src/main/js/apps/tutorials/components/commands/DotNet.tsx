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
import CodeSnippet from '../../../../components/common/CodeSnippet';
import InstanceMessage from '../../../../components/common/InstanceMessage';
import MSBuildScanner from './MSBuildScanner';

export interface Props {
  host: string;
  organization?: string;
  projectKey: string;
  small?: boolean;
  token: string;
}

export default function DotNet(props: Props) {
  const command1 = [
    'SonarScanner.MSBuild.exe begin',
    `/k:"${props.projectKey}"`,
    props.organization && `/d:sonar.organization="${props.organization}"`,
    `/d:sonar.host.url="${props.host}"`,
    `/d:sonar.login="${props.token}"`
  ];

  const command2 = 'MsBuild.exe /t:Rebuild';

  const command3 = ['SonarScanner.MSBuild.exe end', `/d:sonar.login="${props.token}"`];

  return (
    <div>
      <MSBuildScanner />

      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.msbuild.execute')}
      </h4>
      <InstanceMessage message={translate('onboarding.analysis.msbuild.execute.text')}>
        {transformedMessage => <p className="spacer-bottom markdown">{transformedMessage}</p>}
      </InstanceMessage>
      <CodeSnippet isOneLine={true} snippet={command1} />
      <CodeSnippet isOneLine={false} snippet={command2} />
      <CodeSnippet isOneLine={props.small} snippet={command3} />
      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: (
              <a
                href="http://redirect.sonarsource.com/doc/install-configure-scanner-msbuild.html"
                rel="noopener noreferrer"
                target="_blank">
                {translate('onboarding.analysis.msbuild.docs_link')}
              </a>
            )
          }}
        />
      </p>
    </div>
  );
}
