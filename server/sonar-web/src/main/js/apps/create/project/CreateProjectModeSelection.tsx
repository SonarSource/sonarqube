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
import * as classNames from 'classnames';
import * as React from 'react';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { AlmKeys } from '../../../types/alm-settings';
import { CreateProjectModes } from './types';

export interface CreateProjectModeSelectionProps {
  almCounts: { [key in AlmKeys]: number };
  loadingBindings: boolean;
  onSelectMode: (mode: CreateProjectModes) => void;
}

function renderAlmOption(
  props: CreateProjectModeSelectionProps,
  alm: AlmKeys,
  mode: CreateProjectModes
) {
  const { almCounts, loadingBindings } = props;

  const count = almCounts[alm];
  const disabled = count !== 1 || loadingBindings;

  return (
    <button
      className={classNames(
        'button button-huge big-spacer-left display-flex-column create-project-mode-type-alm',
        { disabled }
      )}
      disabled={disabled}
      onClick={() => props.onSelectMode(mode)}
      type="button">
      <img
        alt="" // Should be ignored by screen readers
        height={80}
        src={`${getBaseUrl()}/images/alm/${alm}.svg`}
      />
      <div className="medium big-spacer-top">
        {translate('onboarding.create_project.select_method', alm)}
      </div>

      {loadingBindings && (
        <span>
          {translate('onboarding.create_project.check_alm_supported')}
          <i className="little-spacer-left spinner" />
        </span>
      )}

      {!loadingBindings && disabled && (
        <div className="text-muted small spacer-top" style={{ lineHeight: 1.5 }}>
          {translate('onboarding.create_project.alm_not_configured')}
          <HelpTooltip
            className="little-spacer-left"
            overlay={
              count === 0
                ? translate('onboarding.create_project.zero_alm_instances', alm)
                : `${translate('onboarding.create_project.too_many_alm_instances', alm)} 
                  ${translateWithParameters(
                    'onboarding.create_project.alm_instances_count_X',
                    count
                  )}`
            }
          />
        </div>
      )}
    </button>
  );
}

export default function CreateProjectModeSelection(props: CreateProjectModeSelectionProps) {
  return (
    <>
      <header className="huge-spacer-top big-spacer-bottom padded">
        <h1 className="text-center huge big-spacer-bottom">
          {translate('my_account.create_new.TRK')}
        </h1>
        <p className="text-center big">{translate('onboarding.create_project.select_method')}</p>
      </header>

      <div className="create-project-modes huge-spacer-top display-flex-justify-center">
        <button
          className="button button-huge display-flex-column create-project-mode-type-manual"
          onClick={() => props.onSelectMode(CreateProjectModes.Manual)}
          type="button">
          <img
            alt="" // Should be ignored by screen readers
            height={80}
            src={`${getBaseUrl()}/images/sonarcloud/analysis/manual.svg`}
          />
          <div className="medium big-spacer-top">
            {translate('onboarding.create_project.select_method.manual')}
          </div>
        </button>

        {renderAlmOption(props, AlmKeys.Bitbucket, CreateProjectModes.BitbucketServer)}
        {renderAlmOption(props, AlmKeys.GitHub, CreateProjectModes.GitHub)}
        {renderAlmOption(props, AlmKeys.GitLab, CreateProjectModes.GitLab)}
      </div>
    </>
  );
}
