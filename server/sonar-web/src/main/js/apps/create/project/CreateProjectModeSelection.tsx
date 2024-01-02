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
/* eslint-disable react/no-unused-prop-types */

import classNames from 'classnames';
import * as React from 'react';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import ChevronsIcon from '../../../components/icons/ChevronsIcon';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { AlmKeys } from '../../../types/alm-settings';
import { AppState } from '../../../types/appstate';
import { CreateProjectModes } from './types';

export interface CreateProjectModeSelectionProps {
  almCounts: {
    [k in AlmKeys]: number;
  };
  appState: AppState;
  loadingBindings: boolean;
  onSelectMode: (mode: CreateProjectModes) => void;
  onConfigMode: (mode: AlmKeys) => void;
}

const DEFAULT_ICON_SIZE = 50;

function getErrorMessage(hasConfig: boolean, canAdmin: boolean | undefined) {
  if (!hasConfig) {
    return canAdmin
      ? translate('onboarding.create_project.alm_not_configured.admin')
      : translate('onboarding.create_project.alm_not_configured');
  }
  return undefined;
}

function renderAlmOption(
  props: CreateProjectModeSelectionProps,
  alm: AlmKeys,
  mode: CreateProjectModes,
  last = false
) {
  const {
    almCounts,
    appState: { canAdmin },
    loadingBindings,
  } = props;
  const count = almCounts[alm];
  const hasConfig = count > 0;
  const disabled = loadingBindings || (!hasConfig && !canAdmin);

  const onClick = () => {
    if (!hasConfig && !canAdmin) {
      return null;
    }

    if (!hasConfig && canAdmin) {
      const configMode = alm === AlmKeys.BitbucketCloud ? AlmKeys.BitbucketServer : alm;
      return props.onConfigMode(configMode);
    }

    return props.onSelectMode(mode);
  };

  const errorMessage = getErrorMessage(hasConfig, canAdmin);

  const svgFileName = alm === AlmKeys.BitbucketCloud ? AlmKeys.BitbucketServer : alm;

  return (
    <div className="display-flex-column">
      <button
        className={classNames(
          'button button-huge display-flex-column create-project-mode-type-alm',
          { disabled, 'big-spacer-right': !last }
        )}
        disabled={disabled}
        onClick={onClick}
        type="button"
      >
        <img
          alt="" // Should be ignored by screen readers
          height={DEFAULT_ICON_SIZE}
          src={`${getBaseUrl()}/images/alm/${svgFileName}.svg`}
        />
        <div className="medium big-spacer-top abs-height-50 display-flex-center">
          {translate('onboarding.create_project.select_method', alm)}
        </div>

        {loadingBindings && (
          <span>
            {translate('onboarding.create_project.check_alm_supported')}
            <i className="little-spacer-left spinner" />
          </span>
        )}

        {!loadingBindings && errorMessage && (
          <p className="text-muted small spacer-top" style={{ lineHeight: 1.5 }}>
            {errorMessage}
          </p>
        )}
      </button>
    </div>
  );
}

export function CreateProjectModeSelection(props: CreateProjectModeSelectionProps) {
  const {
    appState: { canAdmin },
    almCounts,
  } = props;
  const almTotalCount = Object.values(almCounts).reduce((prev, cur) => prev + cur);

  return (
    <>
      <h1 className="huge-spacer-top huge-spacer-bottom">
        {translate('onboarding.create_project.select_method')}
      </h1>

      <p>{translate('onboarding.create_project.select_method.devops_platform')}</p>
      {almTotalCount === 0 && canAdmin && (
        <p className="spacer-top">
          {translate('onboarding.create_project.select_method.no_alm_yet.admin')}
        </p>
      )}
      <div className="big-spacer-top huge-spacer-bottom display-flex-center">
        {renderAlmOption(props, AlmKeys.Azure, CreateProjectModes.AzureDevOps)}
        {renderAlmOption(props, AlmKeys.BitbucketServer, CreateProjectModes.BitbucketServer)}
        {renderAlmOption(props, AlmKeys.BitbucketCloud, CreateProjectModes.BitbucketCloud)}
        {renderAlmOption(props, AlmKeys.GitHub, CreateProjectModes.GitHub)}
        {renderAlmOption(props, AlmKeys.GitLab, CreateProjectModes.GitLab, true)}
      </div>

      <p className="big-spacer-bottom">
        {translate('onboarding.create_project.select_method.manually')}
      </p>
      <button
        className="button button-huge display-flex-column create-project-mode-type-manual"
        onClick={() => props.onSelectMode(CreateProjectModes.Manual)}
        type="button"
      >
        <ChevronsIcon size={DEFAULT_ICON_SIZE} />
        <div className="medium big-spacer-top">
          {translate('onboarding.create_project.select_method.manual')}
        </div>
      </button>
    </>
  );
}

export default withAppStateContext(CreateProjectModeSelection);
