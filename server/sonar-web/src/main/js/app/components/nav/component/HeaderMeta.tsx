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
import * as React from 'react';
import { connect } from 'react-redux';
import BranchStatus from '../../../../components/common/BranchStatus';
import HomePageSelect from '../../../../components/controls/HomePageSelect';
import DetachIcon from '../../../../components/icons/DetachIcon';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import { isBranch, isPullRequest } from '../../../../helpers/branch-like';
import { translate } from '../../../../helpers/l10n';
import { isLoggedIn } from '../../../../helpers/users';
import { getCurrentUser, Store } from '../../../../store/rootReducer';
import { BranchLike } from '../../../../types/branch-like';
import { ComponentQualifier } from '../../../../types/component';
import { TaskWarning } from '../../../../types/tasks';
import { Component, CurrentUser, HomePage } from '../../../../types/types';
import ComponentNavWarnings from './ComponentNavWarnings';
import './HeaderMeta.css';

export interface HeaderMetaProps {
  branchLike?: BranchLike;
  currentUser: CurrentUser;
  component: Component;
  onWarningDismiss: () => void;
  warnings: TaskWarning[];
}

export function HeaderMeta(props: HeaderMetaProps) {
  const { branchLike, component, currentUser, warnings } = props;

  const isABranch = isBranch(branchLike);
  const currentPage = getCurrentPage(component, branchLike);
  const displayVersion = component.version !== undefined && isABranch;

  return (
    <>
      <div className="display-flex-center flex-0 small">
        {warnings.length > 0 && (
          <span className="header-meta-warnings">
            <ComponentNavWarnings
              componentKey={component.key}
              onWarningDismiss={props.onWarningDismiss}
              warnings={warnings}
            />
          </span>
        )}
        {component.analysisDate && (
          <span className="spacer-left nowrap note">
            <DateTimeFormatter date={component.analysisDate} />
          </span>
        )}
        {displayVersion && (
          <span className="spacer-left nowrap note">{`${translate('version')} ${
            component.version
          }`}</span>
        )}
        {isLoggedIn(currentUser) && currentPage !== undefined && !isPullRequest(branchLike) && (
          <HomePageSelect className="spacer-left" currentPage={currentPage} />
        )}
      </div>
      {isPullRequest(branchLike) && (
        <div className="navbar-context-meta-secondary display-inline-flex-center">
          {branchLike.url !== undefined && (
            <a
              className="display-inline-flex-center big-spacer-right"
              href={branchLike.url}
              rel="noopener noreferrer"
              target="_blank">
              {translate('branches.see_the_pr')}
              <DetachIcon className="little-spacer-left" size={12} />
            </a>
          )}
          <BranchStatus branchLike={branchLike} component={component.key} />
        </div>
      )}
    </>
  );
}

export function getCurrentPage(component: Component, branchLike: BranchLike | undefined) {
  let currentPage: HomePage | undefined;

  const branch = isBranch(branchLike) && !branchLike.isMain ? branchLike.name : undefined;

  switch (component.qualifier) {
    case ComponentQualifier.Portfolio:
    case ComponentQualifier.SubPortfolio:
      currentPage = { type: 'PORTFOLIO', component: component.key };
      break;
    case ComponentQualifier.Application:
      currentPage = {
        type: 'APPLICATION',
        component: component.key,
        branch
      };
      break;
    case ComponentQualifier.Project:
      // when home page is set to the default branch of a project, its name is returned as `undefined`
      currentPage = {
        type: 'PROJECT',
        component: component.key,
        branch
      };
      break;
  }

  return currentPage;
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(HeaderMeta);
