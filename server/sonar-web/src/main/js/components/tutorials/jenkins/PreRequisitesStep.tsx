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

import { Text } from '@sonarsource/echoes-react';
import { FlagMessage, Link, TutorialStep } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { DocLink } from '../../../helpers/doc-links';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import SentenceWithHighlights from '../components/SentenceWithHighlights';

export interface PreRequisitesStepProps {
  alm: AlmKeys;
  branchesEnabled: boolean;
}

export default function PreRequisitesStep(props: PreRequisitesStepProps) {
  const { alm, branchesEnabled } = props;

  const docUrl = useDocUrl(DocLink.CIJenkins);

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.prereqs.title')}>
      <FlagMessage className="sw-mb-4" variant="warning">
        <span>
          <SentenceWithHighlights
            highlightKeys={['installed', 'configured']}
            translationKey="onboarding.tutorial.with.jenkins.prereqs.intro"
          />
        </span>
      </FlagMessage>
      <Text as="div">
        <ul className="sw-mb-4">
          {branchesEnabled && (
            <li>
              {translate('onboarding.tutorial.with.jenkins.prereqs.plugins.branch_source', alm)}
            </li>
          )}
          {!branchesEnabled && alm === AlmKeys.GitLab && (
            <li>{translate('onboarding.tutorial.with.jenkins.prereqs.plugins.gitlab_plugin')}</li>
          )}
          <li>{translate('onboarding.tutorial.with.jenkins.prereqs.plugins.sonar_scanner')}</li>
        </ul>
        <p className="sw-mb-4">
          <FormattedMessage
            defaultMessage={translate(
              'onboarding.tutorial.with.jenkins.prereqs.step_by_step_guide',
            )}
            id="onboarding.tutorial.with.jenkins.prereqs.step_by_step_guide"
            values={{
              link: (
                <Link to={docUrl}>
                  {translate('onboarding.tutorial.with.jenkins.prereqs.step_by_step_guide.link')}
                </Link>
              ),
            }}
          />
        </p>
        <p className="sw-mb-4">
          {translate('onboarding.tutorial.with.jenkins.prereqs.following_are_recommendations')}
        </p>
      </Text>
    </TutorialStep>
  );
}
