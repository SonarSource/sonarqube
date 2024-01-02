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
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import CodeSnippet from '../../common/CodeSnippet';
import DefaultProjectKey from '../components/DefaultProjectKey';
import { BuildTools } from '../types';
import { buildGradleSnippet } from '../utils';

export interface PreambuleYamlProps {
  buildTool: BuildTools;
  component: Component;
}

export function PreambuleYaml(props: PreambuleYamlProps) {
  const { buildTool, component } = props;
  switch (buildTool) {
    case BuildTools.Gradle:
      return (
        <li className="abs-width-600">
          <FormattedMessage
            defaultMessage={translate('onboarding.tutorial.with.yaml.gradle')}
            id="onboarding.tutorial.with.yaml.gradle"
            values={{
              gradle: (
                <>
                  <code className="rule">build.gradle</code>
                  <ClipboardIconButton copyValue="build.gradle" />
                </>
              ),
              sq: <code className="rule">org.sonarqube</code>,
            }}
          />
          <CodeSnippet snippet={buildGradleSnippet(component.key)} />
        </li>
      );
    case BuildTools.CFamily:
    case BuildTools.Other:
      return <DefaultProjectKey component={component} />;
    default:
      return null;
  }
}
