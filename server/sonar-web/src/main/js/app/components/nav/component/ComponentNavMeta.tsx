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
import { connect } from 'react-redux';
import {
  BranchLike,
  Component,
  CurrentUser,
  isLoggedIn,
  HomePageType,
  HomePage
} from '../../../types';
import BranchStatus from '../../../../components/common/BranchStatus';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Favorite from '../../../../components/controls/Favorite';
import HomePageSelect from '../../../../components/controls/HomePageSelect';
import Tooltip from '../../../../components/controls/Tooltip';
import DetachIcon from '../../../../components/icons-components/DetachIcon';
import {
  isShortLivingBranch,
  isLongLivingBranch,
  isMainBranch,
  isPullRequest
} from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import { getCurrentUser } from '../../../../store/rootReducer';

interface StateProps {
  currentUser: CurrentUser;
}

interface Props extends StateProps {
  branchLike?: BranchLike;
  component: Component;
}

export function ComponentNavMeta({ branchLike, component, currentUser }: Props) {
  const mainBranch = !branchLike || isMainBranch(branchLike);
  const longBranch = isLongLivingBranch(branchLike);
  const currentPage = getCurrentPage(component, branchLike);
  const displayVersion = component.version !== undefined && (mainBranch || longBranch);

  return (
    <div className="navbar-context-meta">
      {component.analysisDate && (
        <div className="spacer-left text-ellipsis">
          <DateTimeFormatter date={component.analysisDate} />
        </div>
      )}
      {displayVersion && (
        <Tooltip mouseEnterDelay={0.5} overlay={`${translate('version')} ${component.version}`}>
          <div className="spacer-left text-limited">
            {translate('version')} {component.version}
          </div>
        </Tooltip>
      )}
      {isLoggedIn(currentUser) && (
        <div className="navbar-context-meta-secondary">
          {mainBranch && (
            <Favorite
              component={component.key}
              favorite={Boolean(component.isFavorite)}
              qualifier={component.qualifier}
            />
          )}
          {(mainBranch || longBranch) &&
            currentPage !== undefined && (
              <HomePageSelect className="spacer-left" currentPage={currentPage} />
            )}
        </div>
      )}
      {(isShortLivingBranch(branchLike) || isPullRequest(branchLike)) && (
        <div className="navbar-context-meta-secondary">
          {isPullRequest(branchLike) &&
            branchLike.url !== undefined && (
              <a
                className="display-inline-flex-center big-spacer-right"
                href={branchLike.url}
                rel="nofollow"
                target="_blank">
                {translate('branches.see_the_pr')}
                <DetachIcon className="little-spacer-left" size={12} />
              </a>
            )}
          <BranchStatus branchLike={branchLike} />
        </div>
      )}
    </div>
  );
}

function getCurrentPage(component: Component, branchLike: BranchLike | undefined) {
  let currentPage: HomePage | undefined;
  if (component.qualifier === 'VW' || component.qualifier === 'SVW') {
    currentPage = { type: HomePageType.Portfolio, component: component.key };
  } else if (component.qualifier === 'APP') {
    currentPage = { type: HomePageType.Application, component: component.key };
  } else if (component.qualifier === 'TRK') {
    // when home page is set to the default branch of a project, its name is returned as `undefined`
    const branch = isLongLivingBranch(branchLike) ? branchLike.name : undefined;
    currentPage = { type: HomePageType.Project, component: component.key, branch };
  }
  return currentPage;
}

const mapStateToProps = (state: any): StateProps => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(ComponentNavMeta);
