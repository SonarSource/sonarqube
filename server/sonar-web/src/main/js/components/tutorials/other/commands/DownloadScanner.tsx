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
import {
  ClipboardIconButton,
  CodeSnippet,
  Link,
  NumberedList,
  NumberedListItem,
  SubHeading,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { InlineSnippet } from '../../components/InlineSnippet';
import { OSs } from '../../types';

export interface DownloadScannerProps {
  isLocal: boolean;
  os: OSs;
  token: string;
}

export default function DownloadScanner(props: DownloadScannerProps) {
  const { os, isLocal, token } = props;

  const docUrl = useDocUrl();

  return (
    <div className="sw-mb-4">
      <SubHeading className="sw-mb-2">
        {translate('onboarding.analysis.sq_scanner.header', os)}
      </SubHeading>
      {isLocal ? (
        <p className="sw-mb-2">
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.sq_scanner.text')}
            id="onboarding.analysis.sq_scanner.text"
            values={{
              dir: <InlineSnippet snippet="bin" />,
              env_var: <InlineSnippet snippet={os === OSs.Windows ? '%PATH%' : 'PATH'} />,
              link: (
                <Link to={docUrl('/analyzing-source-code/scanners/sonarscanner/')}>
                  {translate('onboarding.analysis.sq_scanner.docs_link')}
                </Link>
              ),
            }}
          />
        </p>
      ) : (
        <>
          <CodeSnippet
            className="sw-p-4"
            wrap
            language={os === OSs.Windows ? 'powershell' : 'bash'}
            snippet={getRemoteDownloadSnippet(os)}
            render={`<code>${getRemoteDownloadSnippet(os)}</code>`}
          />
          <SubHeading className="sw-mb-2 sw-mt-4">
            {translate('onboarding.analysis.sq_scanner.sonar_token_env.header')}
          </SubHeading>
          <NumberedList>
            <NumberedListItem className="sw-flex sw-items-center">
              <span className="sw-mr-1">
                {translate('onboarding.analysis.sq_scanner.sonar_token_env.var_name')}:
              </span>
              <InlineSnippet snippet="SONAR_TOKEN" />
              <ClipboardIconButton className="sw-ml-2" copyValue="SONAR_TOKEN" />
            </NumberedListItem>
            <NumberedListItem className="sw-flex sw-items-center">
              <span className="sw-mr-1">
                {translate('onboarding.analysis.sq_scanner.sonar_token_env.var_value')}:
              </span>
              <InlineSnippet snippet={token} />
              <ClipboardIconButton className="sw-ml-2" copyValue={token} />
            </NumberedListItem>
          </NumberedList>
        </>
      )}
    </div>
  );
}

function getRemoteDownloadSnippet(os: OSs) {
  if (os === OSs.Windows) {
    return `$env:SONAR_SCANNER_VERSION = "5.0.1.3006"
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
  return `export SONAR_SCANNER_VERSION=5.0.1.3006
export SONAR_SCANNER_HOME=$HOME/.sonar/sonar-scanner-$SONAR_SCANNER_VERSION-${suffix}
curl --create-dirs -sSLo $HOME/.sonar/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-$SONAR_SCANNER_VERSION-${suffix}.zip
unzip -o $HOME/.sonar/sonar-scanner.zip -d $HOME/.sonar/
export PATH=$SONAR_SCANNER_HOME/bin:$PATH
export SONAR_SCANNER_OPTS="-server"
`;
}
