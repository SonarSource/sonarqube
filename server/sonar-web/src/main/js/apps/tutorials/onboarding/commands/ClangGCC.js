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
import SQScanner from './SQScanner';
import BuildWrapper from './BuildWrapper';
import { translate } from '../../../../helpers/l10n';

/*::
type Props = {
  host: string,
  os: string,
  organization?: string,
  projectKey: string,
  token: string
};
*/

const executables = {
  linux: 'build-wrapper-linux-x86-64',
  win: 'build-wrapper-win-x86-64.exe',
  mac: 'build-wrapper-macosx-x86'
};

export default function ClangGCC(props /*: Props */) {
  const command1 = `${executables[props.os]} --out-dir bw-output make clean all`;

  const command2 = [
    props.os === 'win' ? 'sonar-scanner.bat' : 'sonar-scanner',
    `-Dsonar.projectKey=${props.projectKey}`,
    props.organization && `-Dsonar.organization=${props.organization}`,
    '-Dsonar.sources=.',
    '-Dsonar.cfamily.build-wrapper-output=bw-output',
    `-Dsonar.host.url=${props.host}`,
    `-Dsonar.login=${props.token}`
  ];

  return (
    <div>
      <SQScanner os={props.os} />
      <BuildWrapper className="huge-spacer-top" os={props.os} />

      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.sq_scanner.execute')}
      </h4>
      <p
        className="spacer-bottom markdown"
        dangerouslySetInnerHTML={{
          __html: translate('onboarding.analysis.sq_scanner.execute.text')
        }}
      />
      <Command command={command1} isOneLine={true} />
      <Command command={command2} isOneLine={props.os === 'win'} />
      <p
        className="big-spacer-top markdown"
        dangerouslySetInnerHTML={{ __html: translate('onboarding.analysis.sq_scanner.docs') }}
      />
    </div>
  );
}
