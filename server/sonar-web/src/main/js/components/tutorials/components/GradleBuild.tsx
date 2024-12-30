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
import { ClipboardIconButton, CodeSnippet, NumberedListItem } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { GradleBuildDSL } from '../types';
import { buildGradleSnippet } from '../utils';
import GradleBuildSelection from './GradleBuildSelection';
import { InlineSnippet } from './InlineSnippet';

interface Props {
  component: Component;
}

export default function GradleBuild({ component }: Props) {
  return (
    <NumberedListItem>
      <FormattedMessage
        defaultMessage={translate('onboarding.tutorial.with.yaml.gradle')}
        id="onboarding.tutorial.with.yaml.gradle"
        values={{
          groovy: (
            <>
              <InlineSnippet snippet={GradleBuildDSL.Groovy} />
              <ClipboardIconButton
                copyValue={GradleBuildDSL.Groovy}
                className="sw-ml-2 sw-align-sub"
              />
            </>
          ),
          kotlin: (
            <>
              <InlineSnippet snippet={GradleBuildDSL.Kotlin} />
              <ClipboardIconButton
                copyValue={GradleBuildDSL.Kotlin}
                className="sw-ml-2 sw-align-sub"
              />
            </>
          ),
          sq: <InlineSnippet snippet="org.sonarqube" />,
        }}
      />
      <GradleBuildSelection className="sw-my-4">
        {(build) => (
          <CodeSnippet
            language={build === GradleBuildDSL.Groovy ? 'groovy' : 'kotlin'}
            className="sw-p-6"
            snippet={buildGradleSnippet(component.key, component.name, build)}
          />
        )}
      </GradleBuildSelection>
    </NumberedListItem>
  );
}
