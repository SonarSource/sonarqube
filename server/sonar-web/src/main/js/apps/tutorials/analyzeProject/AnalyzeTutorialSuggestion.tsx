/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';

export default function AnalyzeTutorialSuggestion({ almId }: { almId?: string }) {
  if (almId && almId.startsWith('bitbucket')) {
    return (
      <div className="alert alert-info big-spacer-bottom">
        <p>{translate('onboarding.project_analysis.commands_for_analysis')}</p>
        <p>{translate('onboarding.project_analysis.suggestions.bitbucket')}</p>
        <FormattedMessage
          defaultMessage={translate('onboarding.project_analysis.simply_link')}
          id={'onboarding.project_analysis.simply_link'}
          values={{
            link: (
              <a
                href={
                  getBaseUrl() +
                  '/documentation/integrations/bitbucketcloud#analyzing-with-pipelines'
                }
                target="_blank">
                {translate('onboarding.project_analysis.guide_to_integrate_piplines')}
              </a>
            )
          }}
        />
      </div>
    );
  } else if (almId === 'github') {
    return (
      <div className="alert alert-info big-spacer-bottom">
        <p>{translate('onboarding.project_analysis.commands_for_analysis')} </p>
        <p>{translate('onboarding.project_analysis.suggestions.github')}</p>
        <FormattedMessage
          defaultMessage={translate('onboarding.project_analysis.simply_link')}
          id={'onboarding.project_analysis.simply_link'}
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
      </div>
    );
  } else if (almId === 'microsoft') {
    return (
      <p className="alert alert-info big-spacer-bottom">
        <FormattedMessage
          defaultMessage={translate('onboarding.project_analysis.simply_link')}
          id={'onboarding.project_analysis.simply_link'}
          values={{
            link: (
              <a href={getBaseUrl() + '/documentation/integrations/vsts'} target="_blank">
                {translate('onboarding.project_analysis.guide_to_integrate_vsts')}
              </a>
            )
          }}
        />
      </p>
    );
  }
  return null;
}
