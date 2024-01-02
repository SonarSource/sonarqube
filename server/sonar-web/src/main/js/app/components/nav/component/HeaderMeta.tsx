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
import { useIntl } from 'react-intl';
import BranchStatus from '../../../../components/common/BranchStatus';
import Link from '../../../../components/common/Link';
import HomePageSelect from '../../../../components/controls/HomePageSelect';
import { formatterOption } from '../../../../components/intl/DateTimeFormatter';
import { isBranch, isPullRequest } from '../../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { BranchLike } from '../../../../types/branch-like';
import { ComponentQualifier } from '../../../../types/component';
import { TaskWarning } from '../../../../types/tasks';
import { Component } from '../../../../types/types';
import { CurrentUser, HomePage, isLoggedIn } from '../../../../types/users';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
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
  const lastAnalysisDate = useIntl().formatDate(component.analysisDate, formatterOption);

  return (
    <>
      <div className="display-flex-center flex-0 small">
        {warnings.length > 0 && (
          <span className="header-meta-warnings">
            <ComponentNavWarnings
              isBranch={isABranch}
              componentKey={component.key}
              onWarningDismiss={props.onWarningDismiss}
              warnings={warnings}
            />
          </span>
        )}
        {component.analysisDate && (
          <span
            title={translateWithParameters(
              'overview.project.last_analysis.date_time',
              lastAnalysisDate
            )}
            className="spacer-left nowrap note"
          >
            {lastAnalysisDate}
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
            <Link
              className="link-no-underline big-spacer-right"
              to={branchLike.url}
              target="_blank"
              size={12}
            >
              {translate('branches.see_the_pr')}
            </Link>
          )}
          <BranchStatus branchLike={branchLike} component={component} />
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
        branch,
      };
      break;
    case ComponentQualifier.Project:
      // when home page is set to the default branch of a project, its name is returned as `undefined`
      currentPage = {
        type: 'PROJECT',
        component: component.key,
        branch,
      };
      break;
  }

  return currentPage;
}

export default withCurrentUserContext(HeaderMeta);
