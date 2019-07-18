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
import { EditButton } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import { quote } from '../../../utils';
import BuildWrapper from '../BuildWrapper';
import { ClangGCCCommon } from '../Custom/ClangGCCCustom';
import SQScanner from '../SQScanner';

export interface Props {
  host: string;
  onDone: VoidFunction;
  os: string;
  organization?: string;
  projectKey: string;
  small?: boolean;
  toggleModal: VoidFunction;
  token: string;
}

const executables: T.Dict<string> = {
  linux: 'build-wrapper-linux-x86-64',
  win: 'build-wrapper-win-x86-64.exe',
  mac: 'build-wrapper-macosx-x86'
};

export default function ClangGCCOtherCI(props: Props) {
  const command1 = `${executables[props.os]} --out-dir bw-output make clean all`;

  const q = quote(props.os);
  const command2 = [
    props.os === 'win' ? 'sonar-scanner.bat' : 'sonar-scanner',
    '-D' + q(`sonar.projectKey=${props.projectKey}`),
    props.organization && '-D' + q(`sonar.organization=${props.organization}`),
    '-D' + q('sonar.sources=.'),
    '-D' + q('sonar.cfamily.build-wrapper-output=bw-output'),
    '-D' + q(`sonar.host.url=${props.host}`),
    '-D' + q(`sonar.login=${props.token}`)
  ];

  const renderCommand2 = () => (
    <>
      {command2.join(' \\\n  ')}{' '}
      <EditButton className="edit-token spacer-left" onClick={props.toggleModal} />
    </>
  );

  const commandLinuxMac = `
local SONAR_SCANNER_VERSION=${translate('onboarding.analysis.sonar_scanner_version')}
export SONAR_SCANNER_HOME=$HOME/.sonar/sonar-scanner-$SONAR_SCANNER_VERSION
rm -rf $SONAR_SCANNER_HOME
mkdir -p $SONAR_SCANNER_HOME
curl -sSLo $HOME/.sonar/sonar-scanner.zip http://repo1.maven.org/maven2/org/sonarsource/scanner/cli/sonar-scanner-cli/$SONAR_SCANNER_VERSION/sonar-scanner-cli-$SONAR_SCANNER_VERSION.zip
unzip $HOME/.sonar/sonar-scanner.zip -d $HOME/.sonar/
rm $HOME/.sonar/sonar-scanner.zip
export PATH=$SONAR_SCANNER_HOME/bin:$PATH
export SONAR_SCANNER_OPTS="-server"

curl -LsS https://sonarcloud.io/static/cpp/build-wrapper-linux-x86.zip > build-wrapper-linux-x86.zip
unzip build-wrapper-linux-x86.zip`;

  return (
    <div className="huge-spacer-top">
      {props.os === 'win' ? (
        <>
          <SQScanner os="ci" />

          <BuildWrapper className="huge-spacer-top" os="ci" />
        </>
      ) : (
        <>
          <h4 className="huge-spacer-top spacer-bottom">
            {translate('onboarding.analysis.sq_scanner.header.ci')}
          </h4>

          <CodeSnippet snippet={commandLinuxMac} />
        </>
      )}

      <ClangGCCCommon
        command1={command1}
        command2={command2}
        onDone={props.onDone}
        os={props.os}
        renderCommand2={renderCommand2}
      />
    </div>
  );
}
