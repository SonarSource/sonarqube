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
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { lazyLoadComponent } from '../../../components/lazyLoadComponent';
import { isPullRequest } from '../../../helpers/branch-like';
import { ProjectAlmBindingResponse } from '../../../types/alm-settings';
import { BranchLike } from '../../../types/branch-like';
import { isPortfolioLike } from '../../../types/component';
import { AppState, Component } from '../../../types/types';
import BranchOverview from '../branches/BranchOverview';

const EmptyOverview = lazyLoadComponent(() => import('./EmptyOverview'));
const PullRequestOverview = lazyLoadComponent(() => import('../pullRequests/PullRequestOverview'));

interface Props {
  appState: AppState;
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  projectBinding?: ProjectAlmBindingResponse;
  router: Pick<Router, 'replace'>;
}

export class App extends React.PureComponent<Props> {
  isPortfolio = () => {
    return isPortfolioLike(this.props.component.qualifier);
  };

  render() {
    const {
      appState: { branchesEnabled },
      branchLike,
      branchLikes,
      component,
      projectBinding
    } = this.props;

    if (this.isPortfolio()) {
      return null;
    }

    return isPullRequest(branchLike) ? (
      <>
        <Suggestions suggestions="pull_requests" />
        <PullRequestOverview branchLike={branchLike} component={component} />
      </>
    ) : (
      <>
        <Suggestions suggestions="overview" />

        {!component.analysisDate && (
          <EmptyOverview
            branchLike={branchLike}
            branchLikes={branchLikes}
            component={component}
            hasAnalyses={this.props.isPending || this.props.isInProgress}
            projectBinding={projectBinding}
          />
        )}

        {component.analysisDate && (
          <BranchOverview
            branch={branchLike}
            branchesEnabled={branchesEnabled}
            component={component}
            projectBinding={projectBinding}
          />
        )}
      </>
    );
  }
}

export default withRouter(withAppStateContext(App));
