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
import { Button, EditButton } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import { quote } from '../../../utils';
import { ProjectAnalysisModes } from '../../ProjectAnalysisStepFromBuildTool';
import BuildWrapper from '../BuildWrapper';
import SQScanner from '../SQScanner';

export interface Props {
  host: string;
  mode: ProjectAnalysisModes;
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

interface ClangGCCProps extends Pick<Props, 'small' | 'onDone' | 'os'> {
  command1: string;
  command2: (string | undefined)[];
  renderCommand2: () => JSX.Element;
}

export function ClangGCCCommon(props: ClangGCCProps) {
  return (
    <>
      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.sq_scanner.execute')}
      </h4>

      <p className="spacer-bottom markdown">
        {translate('onboarding.analysis.sq_scanner.execute.text.custom')}
      </p>

      <CodeSnippet isOneLine={props.small} snippet={props.command1} />

      <CodeSnippet
        isOneLine={props.os === 'win'}
        render={props.renderCommand2}
        snippet={props.command2}
        wrap={true}
      />

      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.sq_scanner.docs')}
        id="onboarding.analysis.sq_scanner.docs"
        values={{
          link: (
            <a
              href="http://redirect.sonarsource.com/doc/install-configure-scanner.html"
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.analysis.sq_scanner.docs_link')}
            </a>
          )
        }}
      />

      <div className="big-spacer-top">
        <Button className="js-continue" onClick={props.onDone}>
          {translate('onboarding.finish')}
        </Button>
      </div>
    </>
  );
}

export default function ClangGCCCustom(props: Props) {
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

  return (
    <div className="huge-spacer-top">
      <SQScanner os={props.os} />
      <BuildWrapper className="huge-spacer-top" os={props.os} />
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
