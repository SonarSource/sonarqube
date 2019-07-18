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
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../components/common/CodeSnippet';
import { RequirementOtherBuild } from '../AnalysisCommandTravis';

interface CommonTravisSonarCloudProps {
  command: string;
  renderCommand: () => JSX.Element;
}

export function CommonTravisSonarCloud({ command, renderCommand }: CommonTravisSonarCloudProps) {
  return (
    <div>
      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.setup.title.a')}
      </h2>

      <RequirementOtherBuild />

      <hr className="no-horizontal-margins" />

      <h2 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.travis.setup.title.b')}
      </h2>

      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.sq_scanner.text.sonarcloud')}
        id="onboarding.analysis.sq_scanner.text.sonarcloud"
        values={{
          file: <code>.travis.yml</code>
        }}
      />

      <CodeSnippet render={renderCommand} snippet={command} />

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
    </div>
  );
}
