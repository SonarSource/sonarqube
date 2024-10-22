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

import { FormattedMessage } from 'react-intl';
import { CodeSnippet, Link, NumberedListItem } from '~design-system';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import MonorepoDocLinkFallback from './MonorepoDocLinkFallback';

export interface DartProps {
  branchesEnabled?: boolean;
  component: Component;
  mainBranchName: string;
  monorepo?: boolean;
}

export default function Dart(props: Readonly<DartProps>) {
  const { component, branchesEnabled, mainBranchName, monorepo } = props;
  const docUrl = useDocUrl(DocLink.SonarScanner);

  if (monorepo) {
    return <MonorepoDocLinkFallback />;
  }

  return (
    <NumberedListItem>
      <FormattedMessage
        defaultMessage={translate('onboarding.tutorial.with.github_action.dart')}
        id="onboarding.tutorial.with.github_action.dart"
      />
      <CodeSnippet
        className="sw-p-6 sw-overflow-auto"
        snippet={`
name: Build

on:
  push:
    branches:
      - ${mainBranchName}
${branchesEnabled ? '  pull_request:\n    types: [opened, synchronize, reopened]' : ''}

jobs:
  build:
    name: Build and analyze
    runs-on: ubuntu-latest
    steps:
      - <commands to build your project>
      - name: Download sonar-scanner
        run: |
          curl --create-dirs -sSLo $HOME/.sonar/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-6.1.0.4477-linux-x64.zip
          unzip $HOME/.sonar/sonar-scanner.zip -o -d $HOME/.sonar/
      - name: Run sonar-scanner
        env:
          SONAR_TOKEN: \${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: \${{ secrets.SONAR_HOST_URL }}
        run: |
          sonar-scanner-6.1.0.4477-linux-x64/bin/sonar-scanner \\
            -Dsonar.projectKey=${component.key}`}
        language="yml"
      />

      <p className="sw-mt-4">
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.sq_scanner.docs')}
          id="onboarding.analysis.sq_scanner.docs"
          values={{
            link: <Link to={docUrl}>{translate('onboarding.analysis.sq_scanner.docs_link')}</Link>,
          }}
        />
      </p>
    </NumberedListItem>
  );
}
