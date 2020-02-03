/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { CreateProjectModes } from './types';

export interface CreateProjectModeSelectionProps {
  bbsBindingCount: number;
  loadingBindings: boolean;
  onSelectMode: (mode: CreateProjectModes) => void;
}

export default function CreateProjectModeSelection(props: CreateProjectModeSelectionProps) {
  const { bbsBindingCount, loadingBindings } = props;

  return (
    <>
      <header className="huge-spacer-top big-spacer-bottom padded">
        <h1 className="text-center huge big-spacer-bottom">
          {translate('my_account.create_new.TRK')}
        </h1>
        <p className="text-center big">{translate('onboarding.create_project.select_method')}</p>
      </header>

      <div className="create-project-modes huge-spacer-top display-flex-space-around">
        <button
          className="button button-huge display-flex-column create-project-mode-type-manual"
          onClick={() => props.onSelectMode(CreateProjectModes.Manual)}
          type="button">
          <img
            alt="" // Should be ignored by screen readers
            height={80}
            src={`${getBaseUrl()}/images/sonarcloud/analysis/manual.svg`}
            width={80}
          />
          <div className="medium big-spacer-top">
            {translate('onboarding.create_project.select_method.manual')}
          </div>
        </button>

        <button
          className="button button-huge big-spacer-left display-flex-column create-project-mode-type-bbs"
          disabled={bbsBindingCount !== 1}
          onClick={() => props.onSelectMode(CreateProjectModes.BitbucketServer)}
          type="button">
          <img
            alt="" // Should be ignored by screen readers
            height={80}
            src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
            width={80}
          />
          <div className="medium big-spacer-top">
            {translate('onboarding.create_project.select_method.from_bbs')}
          </div>

          {loadingBindings && (
            <span>
              {translate('onboarding.create_project.check_bbs_supported')}
              <i className="little-spacer-left spinner" />
            </span>
          )}

          {!loadingBindings && bbsBindingCount !== 1 && (
            <div className="text-muted small spacer-top" style={{ lineHeight: 1.5 }}>
              {translate('onboarding.create_project.bbs_not_configured')}
              <HelpTooltip
                className="little-spacer-left"
                overlay={
                  bbsBindingCount === 0
                    ? translate('onboarding.create_project.zero_bbs_instances')
                    : translateWithParameters(
                        'onboarding.create_project.too_many_bbs_instances_X',
                        bbsBindingCount
                      )
                }
              />
            </div>
          )}
        </button>
      </div>
    </>
  );
}
