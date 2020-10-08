/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../common/CodeSnippet';
import InstanceMessage from '../../../common/InstanceMessage';

export interface DotNetProps {
  host: string;
  projectKey: string;
  token: string;
}

export default function DotNet(props: DotNetProps) {
  const { host, projectKey, token } = props;

  const command1 = [
    'SonarScanner.MSBuild.exe begin',
    `/k:"${projectKey}"`,
    `/d:sonar.host.url="${host}"`,
    `/d:sonar.login="${token}"`
  ];

  const command2 = 'MsBuild.exe /t:Rebuild';

  const command3 = ['SonarScanner.MSBuild.exe end', `/d:sonar.login="${token}"`];

  return (
    <div>
      <div>
        <h4 className="spacer-bottom">{translate('onboarding.analysis.msbuild.header')}</h4>
        <p className="spacer-bottom markdown">
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.msbuild.text')}
            id="onboarding.analysis.msbuild.text"
            values={{ code: <code>%PATH%</code> }}
          />
        </p>
        <p>
          <Link
            className="button"
            to="/documentation/analysis/scan/sonarscanner-for-msbuild/"
            target="_blank">
            {translate('download_verb')}
          </Link>
        </p>
      </div>

      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.msbuild.execute')}
      </h4>
      <InstanceMessage message={translate('onboarding.analysis.msbuild.execute.text')}>
        {transformedMessage => <p className="spacer-bottom markdown">{transformedMessage}</p>}
      </InstanceMessage>
      <CodeSnippet isOneLine={true} snippet={command1} />
      <CodeSnippet isOneLine={true} snippet={command2} />
      <CodeSnippet isOneLine={true} snippet={command3} />
      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: (
              <Link to="/documentation/analysis/scan/sonarscanner-for-msbuild/" target="_blank">
                {translate('onboarding.analysis.msbuild.docs_link')}
              </Link>
            )
          }}
        />
      </p>
    </div>
  );
}
