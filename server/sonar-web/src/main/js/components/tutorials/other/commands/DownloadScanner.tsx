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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../../helpers/l10n';
import CodeSnippet from '../../../common/CodeSnippet';
import DocLink from '../../../common/DocLink';
import { ClipboardButton } from '../../../controls/clipboard';
import { OSs } from '../../types';

export interface DownloadScannerProps {
  isLocal: boolean;
  os: OSs;
  token: string;
}

export default function DownloadScanner(props: DownloadScannerProps) {
  const { os, isLocal, token } = props;

  return (
    <div>
      <h4 className="spacer-bottom">{translate('onboarding.analysis.sq_scanner.header', os)}</h4>
      {isLocal ? (
        <p className="spacer-bottom markdown">
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.sq_scanner.text')}
            id="onboarding.analysis.sq_scanner.text"
            values={{
              dir: <code>bin</code>,
              env_var: <code>{os === OSs.Windows ? '%PATH%' : 'PATH'}</code>,
              link: (
                <DocLink to="/analyzing-source-code/scanners/sonarscanner/">
                  {translate('onboarding.analysis.sq_scanner.docs_link')}
                </DocLink>
              ),
            }}
          />
        </p>
      ) : (
        <>
          <CodeSnippet snippet={getRemoteDownloadSnippet(os)} />
          <h4 className="spacer-bottom big-spacer-top">
            {translate('onboarding.analysis.sq_scanner.sonar_token_env.header')}
          </h4>
          <ul className="list-styled">
            <li className="markdown">
              {translate('onboarding.analysis.sq_scanner.sonar_token_env.var_name')}:{' '}
              <code>SONAR_TOKEN</code>
              <ClipboardButton className="spacer-left" copyValue="SONAR_TOKEN" />
            </li>
            <li className="markdown">
              {translate('onboarding.analysis.sq_scanner.sonar_token_env.var_value')}:{' '}
              <code>{token}</code>
              <ClipboardButton className="spacer-left" copyValue={token} />
            </li>
          </ul>
        </>
      )}
    </div>
  );
}

function getRemoteDownloadSnippet(os: OSs) {
  if (os === OSs.Windows) {
    return `$env:SONAR_SCANNER_VERSION = "4.7.0.2747"
$env:SONAR_DIRECTORY = [System.IO.Path]::Combine($(get-location).Path,".sonar")
$env:SONAR_SCANNER_HOME = "$env:SONAR_DIRECTORY/sonar-scanner-$env:SONAR_SCANNER_VERSION-windows"
rm $env:SONAR_SCANNER_HOME -Force -Recurse -ErrorAction SilentlyContinue
New-Item -path $env:SONAR_SCANNER_HOME -type directory
(New-Object System.Net.WebClient).DownloadFile("https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-$env:SONAR_SCANNER_VERSION-windows.zip", "$env:SONAR_DIRECTORY/sonar-scanner.zip")
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$env:SONAR_DIRECTORY/sonar-scanner.zip", "$env:SONAR_DIRECTORY")
rm ./.sonar/sonar-scanner.zip -Force -ErrorAction SilentlyContinue
$env:Path += ";$env:SONAR_SCANNER_HOME/bin"
$env:SONAR_SCANNER_OPTS="-server"
`;
  }
  const suffix = os === OSs.MacOS ? 'macosx' : 'linux';
  return `export SONAR_SCANNER_VERSION=4.7.0.2747
export SONAR_SCANNER_HOME=$HOME/.sonar/sonar-scanner-$SONAR_SCANNER_VERSION-${suffix}
curl --create-dirs -sSLo $HOME/.sonar/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-$SONAR_SCANNER_VERSION-${suffix}.zip
unzip -o $HOME/.sonar/sonar-scanner.zip -d $HOME/.sonar/
export PATH=$SONAR_SCANNER_HOME/bin:$PATH
export SONAR_SCANNER_OPTS="-server"
`;
}
