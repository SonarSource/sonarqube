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
import { Button, EditButton } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import InstanceMessage from '../../../../../components/common/InstanceMessage';
import { quote } from '../../../utils';
import SQScanner from '../SQScanner';

export interface Props {
  component: T.Component;
  currentUser: T.LoggedInUser;
  host: string;
  onDone: VoidFunction;
  organization?: string;
  os: string;
  projectKey: string;
  toggleModal: VoidFunction;
  token: string;
}

export default function OtherOtherCI(props: Props) {
  const q = quote(props.os);
  const command = [
    props.os === 'win' ? 'sonar-scanner.bat' : 'sonar-scanner',
    '-D' + q(`sonar.projectKey=${props.projectKey}`),
    props.organization && '-D' + q(`sonar.organization=${props.organization}`),
    '-D' + q('sonar.sources=.'),
    '-D' + q(`sonar.host.url=${props.host}`),
    '-D' + q(`sonar.login=${props.token}`)
  ];

  const renderCommand = () => (
    <>
      {command.join(' \\\n  ')}{' '}
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
export SONAR_SCANNER_OPTS="-server"`;

  return (
    <div className="huge-spacer-top">
      {props.os === 'win' ? (
        <SQScanner os={props.os} />
      ) : (
        <>
          <h4 className="huge-spacer-top spacer-bottom">
            {translate('onboarding.analysis.sq_scanner.header.ci')}
          </h4>

          <CodeSnippet snippet={commandLinuxMac} />
        </>
      )}

      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.sq_scanner.execute.ci')}
      </h4>

      <InstanceMessage message={translate('onboarding.analysis.sq_scanner.execute.text.custom')}>
        {transformedMessage => (
          <p
            className="spacer-bottom markdown"
            dangerouslySetInnerHTML={{ __html: transformedMessage }}
          />
        )}
      </InstanceMessage>

      <CodeSnippet isOneLine={props.os === 'win'} render={renderCommand} snippet={command} />

      <p
        className="big-spacer-top markdown"
        dangerouslySetInnerHTML={{ __html: translate('onboarding.analysis.standard.docs') }}
      />

      <div className="big-spacer-top">
        <Button className="js-continue" onClick={props.onDone}>
          {translate('onboarding.finish')}
        </Button>
      </div>
    </div>
  );
}
