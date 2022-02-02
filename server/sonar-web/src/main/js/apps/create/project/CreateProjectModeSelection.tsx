/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import ChevronsIcon from '../../../components/icons/ChevronsIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { AlmKeys } from '../../../types/alm-settings';
import { AppState } from '../../../types/types';
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

function renderAlmOption(
  props: CreateProjectModeSelectionProps,
  alm: AlmKeys.Azure | AlmKeys.BitbucketServer | AlmKeys.GitHub | AlmKeys.GitLab,
  mode: CreateProjectModes,
  last = false
) {
  const {
    almCounts,
    appState: { canAdmin },
    loadingBindings
  } = props;

  const hasBitbucketCloudConf = almCounts[AlmKeys.BitbucketCloud] > 0;
  const isBitbucketOption = alm === AlmKeys.BitbucketServer;

  const count = isBitbucketOption
    ? almCounts[AlmKeys.BitbucketServer] + almCounts[AlmKeys.BitbucketCloud]
    : almCounts[alm];
  const hasConfig = count > 0;
  const hasTooManyConfig = count > 1;
  const disabled = loadingBindings || hasTooManyConfig || (!hasConfig && !canAdmin);

  const onClick = () => {
    if (hasTooManyConfig || (!hasConfig && !canAdmin)) {
      return null;
    }

    if (!hasConfig && canAdmin) {
      return props.onConfigMode(alm);
    }

    return props.onSelectMode(
      isBitbucketOption && hasBitbucketCloudConf ? CreateProjectModes.BitbucketCloud : mode
    );
  };

  let errorMessage = '';

  if (hasTooManyConfig) {
    errorMessage = translateWithParameters(
      'onboarding.create_project.too_many_alm_instances_X',
      translate('alm', alm)
    );
  } else if (!hasConfig) {
    errorMessage = canAdmin
      ? translate('onboarding.create_project.alm_not_configured.admin')
      : translate('onboarding.create_project.alm_not_configured');
  }

  return (
    <div className="display-flex-column">
      <button
        className={classNames(
          'button button-huge display-flex-column create-project-mode-type-alm',
          { disabled, 'big-spacer-right': !last }
        )}
        disabled={disabled}
        onClick={onClick}
        type="button">
        <img
          alt="" // Should be ignored by screen readers
          height={DEFAULT_ICON_SIZE}
          src={`${getBaseUrl()}/images/alm/${alm}.svg`}
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
    almCounts
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
        {renderAlmOption(props, AlmKeys.GitHub, CreateProjectModes.GitHub)}
        {renderAlmOption(props, AlmKeys.GitLab, CreateProjectModes.GitLab, true)}
      </div>

      <p className="big-spacer-bottom">
        {translate('onboarding.create_project.select_method.manually')}
      </p>
      <button
        className="button button-huge display-flex-column create-project-mode-type-manual"
        onClick={() => props.onSelectMode(CreateProjectModes.Manual)}
        type="button">
        <ChevronsIcon size={DEFAULT_ICON_SIZE} />
        <div className="medium big-spacer-top">
          {translate('onboarding.create_project.select_method.manual')}
        </div>
      </button>
    </>
  );
}

export default withAppStateContext(CreateProjectModeSelection);
