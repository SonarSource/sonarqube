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
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import DocLink from '../../common/DocLink';

export interface DoneNextStepsProps {
  component: Component;
}

export default function DoneNextSteps({ component }: DoneNextStepsProps) {
  const isProjectAdmin = component.configuration?.showSettings;

  return (
    <>
      <hr className="big-spacer-top big-spacer-bottom" />

      <p>
        <strong>{translate('onboarding.analysis.auto_refresh_after_analysis.done')}</strong>{' '}
        {translate('onboarding.analysis.auto_refresh_after_analysis.auto_refresh')}
      </p>
      <p className="big-spacer-top">
        {isProjectAdmin
          ? translate('onboarding.analysis.auto_refresh_after_analysis.set_up_pr_deco_and_ci.admin')
          : translate('onboarding.analysis.auto_refresh_after_analysis.set_up_pr_deco_and_ci')}
      </p>
      <p className="big-spacer-top">
        <FormattedMessage
          defaultMessage={translate(
            'onboarding.analysis.auto_refresh_after_analysis.check_these_links'
          )}
          id="onboarding.analysis.auto_refresh_after_analysis.check_these_links"
          values={{
            link_branches: (
              <DocLink to="/analyzing-source-code/branches/branch-analysis/">
                {translate(
                  'onboarding.analysis.auto_refresh_after_analysis.check_these_links.branches'
                )}
              </DocLink>
            ),
            link_pr_analysis: (
              <DocLink to="/analyzing-source-code/pull-request-analysis">
                {translate(
                  'onboarding.analysis.auto_refresh_after_analysis.check_these_links.pr_analysis'
                )}
              </DocLink>
            ),
          }}
        />
      </p>
    </>
  );
}
