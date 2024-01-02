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
import { getBaseUrl } from '../../../../helpers/system';
import CodeSnippet from '../../../common/CodeSnippet';
import { OSs } from '../../types';

export interface DownloadBuildWrapperProps {
  isLocal: boolean;
  os: OSs;
  baseUrl: string;
}

const FILENAMES: { [x in OSs]: string } = {
  win: 'build-wrapper-win-x86',
  linux: 'build-wrapper-linux-x86',
  mac: 'build-wrapper-macosx-x86',
};

export default function DownloadBuildWrapper(props: DownloadBuildWrapperProps) {
  const { os, isLocal, baseUrl } = props;
  return (
    <div className="spacer-bottom">
      <h4 className="spacer-bottom">{translate('onboarding.analysis.build_wrapper.header', os)}</h4>
      {isLocal ? (
        <>
          <p className="spacer-bottom markdown">
            <FormattedMessage
              defaultMessage={translate('onboarding.analysis.build_wrapper.text')}
              id="onboarding.analysis.build_wrapper.text"
              values={{
                env_var: <code>{os === 'win' ? '%PATH%' : 'PATH'}</code>,
              }}
            />
          </p>
          <p>
            <a
              className="button"
              download={`${FILENAMES[os]}.zip`}
              href={`${getBaseUrl()}/static/cpp/${FILENAMES[os]}.zip`}
              rel="noopener noreferrer"
              target="_blank"
            >
              {translate('download_verb')}
            </a>
          </p>
        </>
      ) : (
        <CodeSnippet snippet={getRemoteDownloadSnippet(os, baseUrl)} />
      )}
    </div>
  );
}

function getRemoteDownloadSnippet(os: OSs, baseUrl: string) {
  if (os === OSs.Windows) {
    return `$env:SONAR_DIRECTORY = [System.IO.Path]::Combine($(get-location).Path,".sonar")
rm "$env:SONAR_DIRECTORY/build-wrapper-win-x86" -Force -Recurse -ErrorAction SilentlyContinue
New-Item -path $env:SONAR_DIRECTORY/build-wrapper-win-x86 -type directory
(New-Object System.Net.WebClient).DownloadFile("${baseUrl}/static/cpp/build-wrapper-win-x86.zip", "$env:SONAR_DIRECTORY/build-wrapper-win-x86.zip")
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$env:SONAR_DIRECTORY/build-wrapper-win-x86.zip", "$env:SONAR_DIRECTORY")
$env:Path += ";$env:SONAR_DIRECTORY/build-wrapper-win-x86"
`;
  }
  return `curl --create-dirs -sSLo $HOME/.sonar/${FILENAMES[os]}.zip ${baseUrl}/static/cpp/${FILENAMES[os]}.zip
unzip -o $HOME/.sonar/${FILENAMES[os]}.zip -d $HOME/.sonar/
export PATH=$HOME/.sonar/${FILENAMES[os]}:$PATH
`;
}
