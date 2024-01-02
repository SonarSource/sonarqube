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
import { translate } from '../../../../helpers/l10n';
import CodeSnippet from '../../../common/CodeSnippet';
import { Alert } from '../../../ui/Alert';
import { DotNetProps } from './DotNet';
import DotNetExecute from './DotNetExecute';

export default function DotNetCore(props: DotNetProps) {
  const { baseUrl, component, token } = props;

  const commands = [
    `dotnet sonarscanner begin /k:"${component.key}" /d:sonar.host.url="${baseUrl}"  /d:sonar.login="${token}"`,
    'dotnet build',
    `dotnet sonarscanner end /d:sonar.login="${token}"`,
  ];

  return (
    <div>
      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.dotnetcore.global')}
      </h4>
      <p className="big-spacer-top markdown">
        {translate('onboarding.analysis.dotnetcore.global.text')}
      </p>
      <CodeSnippet snippet="dotnet tool install --global dotnet-sonarscanner" />
      <Alert className="spacer-top" variant="info">
        {translate('onboarding.analysis.dotnetcore.global.text.path')}
      </Alert>
      <DotNetExecute commands={commands} component={component} />
    </div>
  );
}
