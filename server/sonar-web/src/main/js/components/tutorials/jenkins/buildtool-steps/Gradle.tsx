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
import { CodeSnippet, NumberedListItem } from '~design-system';
import { translate } from '../../../../helpers/l10n';
import GradleBuildSelection from '../../components/GradleBuildSelection';
import { InlineSnippet } from '../../components/InlineSnippet';
import { GradleBuildDSL } from '../../types';
import { buildGradleSnippet } from '../../utils';
import { LanguageProps } from '../JenkinsStep';
import CreateJenkinsfileBulletPoint from './CreateJenkinsfileBulletPoint';

const JENKINSFILE_SNIPPET = `node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    withSonarQubeEnv() {
      sh "./gradlew sonar"
    }
  }
}`;

export default function Gradle(props: LanguageProps) {
  const { component } = props;

  return (
    <>
      <NumberedListItem>
        <span>
          <FormattedMessage
            defaultMessage={translate(
              'onboarding.tutorial.with.jenkins.jenkinsfile.gradle.step2',
              'sentence',
            )}
            id="onboarding.tutorial.with.jenkins.jenkinsfile.gradle.step2.sentence"
            values={{
              groovy: <InlineSnippet snippet={GradleBuildDSL.Groovy} />,
              kotlin: <InlineSnippet snippet={GradleBuildDSL.Kotlin} />,
            }}
          />
        </span>
        <GradleBuildSelection className="sw-my-4">
          {(build) => (
            <CodeSnippet
              className="sw-p-6"
              language={build === GradleBuildDSL.Groovy ? 'groovy' : 'kotlin'}
              snippet={buildGradleSnippet(component.key, component.name, build)}
            />
          )}
        </GradleBuildSelection>
      </NumberedListItem>
      <CreateJenkinsfileBulletPoint snippet={JENKINSFILE_SNIPPET} />
    </>
  );
}
