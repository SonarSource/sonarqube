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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { isBitbucket, isGithub, isVSTS } from '../../../helpers/almIntegrations';

export function TutorialSuggestionBitbucket() {
  return (
    <Alert className="big-spacer-bottom" variant="info">
      <FormattedMessage
        defaultMessage={translate('onboarding.project_analysis.suggestions.bitbucket')}
        id="onboarding.project_analysis.suggestions.bitbucket"
        values={{
          link: (
            <a
              href={getBaseUrl() + '/documentation/integrations/bitbucketcloud/'}
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.project_analysis.guide_to_integrate_bitbucket_cloud')}
            </a>
          )
        }}
      />
      <p>{translate('onboarding.project_analysis.suggestions.bitbucket_extra')}</p>
    </Alert>
  );
}

export function TutorialSuggestionGithub() {
  return (
    <Alert className="big-spacer-bottom" variant="info">
      <p>{translate('onboarding.project_analysis.commands_for_analysis')} </p>
      <p>{translate('onboarding.project_analysis.suggestions.github')}</p>
      <FormattedMessage
        defaultMessage={translate('onboarding.project_analysis.simply_link')}
        id="onboarding.project_analysis.simply_link"
        values={{
          link: (
            <a
              href="https://docs.travis-ci.com/user/sonarcloud/"
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.project_analysis.guide_to_integrate_travis')}
            </a>
          )
        }}
      />
    </Alert>
  );
}

export function TutorialSuggestionVSTS() {
  return (
    <Alert className="big-spacer-bottom" variant="info">
      <FormattedMessage
        defaultMessage={translate('onboarding.project_analysis.simply_link')}
        id="onboarding.project_analysis.simply_link"
        values={{
          link: (
            <a
              href={getBaseUrl() + '/documentation/integrations/vsts/'}
              rel="noopener noreferrer"
              target="_blank">
              {translate('onboarding.project_analysis.guide_to_integrate_vsts')}
            </a>
          )
        }}
      />
    </Alert>
  );
}

export default function AnalyzeTutorialSuggestion({ almKey }: { almKey?: string }) {
  if (isBitbucket(almKey)) {
    return <TutorialSuggestionBitbucket />;
  } else if (isGithub(almKey)) {
    return <TutorialSuggestionGithub />;
  } else if (isVSTS(almKey)) {
    return <TutorialSuggestionVSTS />;
  }
  return null;
}
