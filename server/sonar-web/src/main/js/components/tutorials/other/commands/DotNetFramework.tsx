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
import DocLink from '../../../common/DocLink';
import { DotNetProps } from './DotNet';
import DotNetExecute from './DotNetExecute';

export default function DotNetFramework(props: DotNetProps) {
  const { baseUrl, component, token } = props;

  const commands = [
    `SonarScanner.MSBuild.exe begin /k:"${component.key}" /d:sonar.host.url="${baseUrl}" /d:sonar.login="${token}"`,
    'MsBuild.exe /t:Rebuild',
    `SonarScanner.MSBuild.exe end /d:sonar.login="${token}"`,
  ];

  return (
    <div>
      <div>
        <h4 className="spacer-bottom huge-spacer-top">
          {translate('onboarding.analysis.msbuild.header')}
        </h4>
        <p className="markdown">
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.msbuild.text')}
            id="onboarding.analysis.msbuild.text"
            values={{
              code: <code>%PATH%</code>,
              link: (
                <DocLink to="/analyzing-source-code/scanners/sonarscanner-for-dotnet/">
                  {translate('onboarding.analysis.msbuild.docs_link')}
                </DocLink>
              ),
            }}
          />
        </p>
      </div>

      <DotNetExecute commands={commands} component={component} />
    </div>
  );
}
