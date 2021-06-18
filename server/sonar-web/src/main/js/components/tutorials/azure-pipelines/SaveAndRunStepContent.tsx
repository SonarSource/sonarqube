/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { AlmKeys } from '../../../types/alm-settings';
import { withAppState } from '../../hoc/withAppState';

export interface SaveAndRunStepContentProps {
  alm?: AlmKeys;
  appState: T.AppState;
}

export function SaveAndRunStepContent(props: SaveAndRunStepContentProps) {
  const {
    alm,
    appState: { branchesEnabled }
  } = props;
  return (
    <>
      <div className="display-flex-row big-spacer-bottom">
        <div>
          <img
            alt="" // Should be ignored by screen readers
            className="big-spacer-right"
            width={30}
            src={`${getBaseUrl()}/images/tutorials/commit.svg`}
          />
        </div>
        <div>
          <p className="little-spacer-bottom">
            <strong>
              {translate('onboarding.tutorial.with.azure_pipelines.SaveAndRun.commit')}
            </strong>
          </p>
          <p>
            {translate('onboarding.tutorial.with.azure_pipelines.SaveAndRun.commit.why')}{' '}
            {branchesEnabled &&
              alm &&
              translateWithParameters(
                'onboarding.tutorial.with.azure_pipelines.SaveAndRun.commit.pr_deco',
                translate('alm', alm)
              )}
          </p>
        </div>
      </div>
      <div className="display-flex-row">
        <div>
          <img
            alt="" // Should be ignored by screen readers
            className="big-spacer-right"
            width={30}
            src={`${getBaseUrl()}/images/tutorials/refresh.svg`}
          />
        </div>
        <div>
          <p className="little-spacer-bottom">
            <strong>
              {translate('onboarding.tutorial.with.azure_pipelines.SaveAndRun.refresh')}
            </strong>
          </p>
          <p>{translate('onboarding.tutorial.with.azure_pipelines.SaveAndRun.refresh.why')}</p>
        </div>
      </div>
    </>
  );
}

export default withAppState(SaveAndRunStepContent);
