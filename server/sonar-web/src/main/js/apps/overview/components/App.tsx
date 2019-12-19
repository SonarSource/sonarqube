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
import { lazyLoad } from 'sonar-ui-common/components/lazyLoad';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { isPullRequest } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import BranchOverview from '../branches/BranchOverview';

const EmptyOverview = lazyLoad(() => import('./EmptyOverview'));
const PullRequestOverview = lazyLoad(() => import('../pullRequests/PullRequestOverview'));

interface Props {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component: T.Component;
  isInProgress?: boolean;
  isPending?: boolean;
  router: Pick<Router, 'replace'>;
}

export class App extends React.PureComponent<Props> {
  componentDidMount() {
    const { component } = this.props;

    if (this.isPortfolio()) {
      this.props.router.replace({
        pathname: '/portfolio',
        query: { id: component.key }
      });
    }
  }

  isPortfolio = () => {
    return ([ComponentQualifier.Portfolio, ComponentQualifier.SubPortfolio] as string[]).includes(
      this.props.component.qualifier
    );
  };

  render() {
    const { branchLike, branchLikes, component } = this.props;

    if (this.isPortfolio()) {
      return null;
    }

    return (
      <>
        {isPullRequest(branchLike) ? (
          <>
            <Suggestions suggestions="pull_requests" />
            <PullRequestOverview branchLike={branchLike} component={component} />
          </>
        ) : (
          <>
            <Suggestions suggestions="overview" />

            {!component.analysisDate ? (
              <EmptyOverview
                branchLike={branchLike}
                branchLikes={branchLikes}
                component={component}
                hasAnalyses={this.props.isPending || this.props.isInProgress}
              />
            ) : (
              <BranchOverview branchLike={branchLike} component={component} />
            )}
          </>
        )}
      </>
    );
  }
}

export default withRouter(App);
