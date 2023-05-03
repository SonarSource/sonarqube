/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import CodeSnippet from '../../common/CodeSnippet';
import { ClipboardIconButton } from '../../controls/clipboard';
import { GradleBuildDSL } from '../types';
import { buildGradleSnippet } from '../utils';
import GradleBuildSelection from './GradleBuildSelection';

interface Props {
  component: Component;
}

export default function GradleBuild({ component }: Props) {
  return (
    <li className="abs-width-600">
      <FormattedMessage
        defaultMessage={translate('onboarding.tutorial.with.yaml.gradle')}
        id="onboarding.tutorial.with.yaml.gradle"
        values={{
          groovy: (
            <>
              <code className="rule">{GradleBuildDSL.Groovy}</code>
              <ClipboardIconButton copyValue={GradleBuildDSL.Groovy} />
            </>
          ),
          kotlin: (
            <>
              <code className="rule">{GradleBuildDSL.Kotlin}</code>
              <ClipboardIconButton copyValue={GradleBuildDSL.Kotlin} />
            </>
          ),
          sq: <code className="rule">org.sonarqube</code>,
        }}
      />
      <GradleBuildSelection className="big-spacer-top big-spacer-bottom">
        {(build) => (
          <CodeSnippet snippet={buildGradleSnippet(component.key, component.name, build)} />
        )}
      </GradleBuildSelection>
    </li>
  );
}
