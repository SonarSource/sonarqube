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
import { Branch, Component, CurrentUser, isLoggedIn, HomePageType } from '../../../types';
import BranchStatus from '../../../../components/common/BranchStatus';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Favorite from '../../../../components/controls/Favorite';
import HomePageSelect from '../../../../components/controls/HomePageSelect';
import Tooltip from '../../../../components/controls/Tooltip';
import { isShortLivingBranch } from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import { getCurrentUser } from '../../../../store/rootReducer';

interface StateProps {
  currentUser: CurrentUser;
}

interface Props extends StateProps {
  branch?: Branch;
  component: Component;
}

export function ComponentNavMeta({ branch, component, currentUser }: Props) {
  const shortBranch = isShortLivingBranch(branch);
  const mainBranch = !branch || branch.isMain;

  return (
    <div className="navbar-context-meta">
      {component.analysisDate && (
        <div className="spacer-left">
          <DateTimeFormatter date={component.analysisDate} />
        </div>
      )}
      {component.version &&
        !shortBranch && (
          <Tooltip overlay={`${translate('version')} ${component.version}`} mouseEnterDelay={0.5}>
            <div className="spacer-left text-limited">
              {translate('version')} {component.version}
            </div>
          </Tooltip>
        )}
      {isLoggedIn(currentUser) &&
        mainBranch && (
          <div className="navbar-context-meta-secondary">
            <Favorite
              component={component.key}
              favorite={Boolean(component.isFavorite)}
              qualifier={component.qualifier}
            />
            <HomePageSelect
              className="spacer-left"
              currentPage={{ type: HomePageType.Project, parameter: component.key }}
            />
          </div>
        )}
      {shortBranch && (
        <div className="navbar-context-meta-secondary">
          <BranchStatus branch={branch!} />
        </div>
      )}
    </div>
  );
}

const mapStateToProps = (state: any): StateProps => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(ComponentNavMeta);
