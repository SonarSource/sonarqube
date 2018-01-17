/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import Command from './Command';
import MSBuildScanner from './MSBuildScanner';
import { translate } from '../../../../helpers/l10n';

/*::
type Props = {|
  host: string,
  organization?: string,
  projectKey: string,
  token: string
|};
*/

export default function DotNet(props /*: Props */) {
  const command1 = [
    'SonarQube.Scanner.MSBuild.exe begin',
    `/k:"${props.projectKey}"`,
    props.organization && `/d:sonar.organization="${props.organization}"`,
    `/d:sonar.host.url="${props.host}"`,
    `/d:sonar.login="${props.token}"`
  ];

  const command2 = 'MsBuild.exe /t:Rebuild';

  const command3 = ['SonarQube.Scanner.MSBuild.exe end', `/d:sonar.login="${props.token}"`];

  return (
    <div>
      <MSBuildScanner />

      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.msbuild.execute')}
      </h4>
      <p
        className="spacer-bottom markdown"
        dangerouslySetInnerHTML={{
          __html: translate('onboarding.analysis.msbuild.execute.text')
        }}
      />
      <Command command={command1} isOneLine={true} />
      <Command command={command2} isOneLine={true} />
      <Command command={command3} isOneLine={true} />
      <p
        className="big-spacer-top markdown"
        dangerouslySetInnerHTML={{ __html: translate('onboarding.analysis.msbuild.docs') }}
      />
    </div>
  );
}
