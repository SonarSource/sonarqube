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
import { Component } from '../../../../types/types';
import CodeSnippet from '../../../common/CodeSnippet';
import DocLink from '../../../common/DocLink';
import InstanceMessage from '../../../common/InstanceMessage';
import DoneNextSteps from '../DoneNextSteps';

export interface DotNetExecuteProps {
  commands: string[];
  component: Component;
}

export default function DotNetExecute({ commands, component }: DotNetExecuteProps) {
  return (
    <>
      <h4 className="huge-spacer-top spacer-bottom">
        {translate('onboarding.analysis.sq_scanner.execute')}
      </h4>

      <InstanceMessage message={translate('onboarding.analysis.msbuild.execute.text')}>
        {(transformedMessage) => <p className="spacer-bottom markdown">{transformedMessage}</p>}
      </InstanceMessage>
      {commands.map((command, index) => (
        <CodeSnippet key={index} snippet={command} />
      ))}
      <p className="big-spacer-top markdown">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.docs')}
          id="onboarding.analysis.docs"
          values={{
            link: (
              <DocLink to="/analyzing-source-code/scanners/sonarscanner-for-dotnet/">
                {translate('onboarding.analysis.msbuild.docs_link')}
              </DocLink>
            ),
          }}
        />
      </p>
      <DoneNextSteps component={component} />
    </>
  );
}
