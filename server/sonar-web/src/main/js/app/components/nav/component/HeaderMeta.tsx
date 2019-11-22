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
import { connect } from 'react-redux';
import DetachIcon from 'sonar-ui-common/components/icons/DetachIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BranchStatus from '../../../../components/common/BranchStatus';
import HomePageSelect from '../../../../components/controls/HomePageSelect';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import { isBranch, isPullRequest } from '../../../../helpers/branch-like';
import { isLoggedIn } from '../../../../helpers/users';
import { getCurrentUser, Store } from '../../../../store/rootReducer';
import { BranchLike } from '../../../../types/branch-like';
import { ComponentQualifier } from '../../../../types/component';
import ComponentNavWarnings from './ComponentNavWarnings';
import './HeaderMeta.css';

export interface HeaderMetaProps {
  branchLike?: BranchLike;
  currentUser: T.CurrentUser;
  component: T.Component;
  warnings: string[];
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
            <ComponentNavWarnings warnings={warnings} />
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
        {isLoggedIn(currentUser) && isABranch && currentPage !== undefined && (
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

export function getCurrentPage(component: T.Component, branchLike: BranchLike | undefined) {
  let currentPage: T.HomePage | undefined;

  switch (component.qualifier) {
    case ComponentQualifier.Portfolio:
    case ComponentQualifier.SubPortfolio:
      currentPage = { type: 'PORTFOLIO', component: component.key };
      break;
    case ComponentQualifier.Application:
      currentPage = {
        type: 'APPLICATION',
        component: component.key,
        branch: isBranch(branchLike) ? branchLike.name : undefined
      };
      break;
    case ComponentQualifier.Project:
      // when home page is set to the default branch of a project, its name is returned as `undefined`
      currentPage = {
        type: 'PROJECT',
        component: component.key,
        branch: isBranch(branchLike) ? branchLike.name : undefined
      };
      break;
  }

  return currentPage;
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(HeaderMeta);
