/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import PropTypes from 'prop-types';
import OverviewApp from './OverviewApp';
import EmptyOverview from './EmptyOverview';
import { getBranchName, isShortLivingBranch } from '../../../helpers/branches';
import { getProjectBranchUrl } from '../../../helpers/urls';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';

/*::
type Props = {
  branch: { name: string },
  component: {
    analysisDate?: string,
    id: string,
    key: string,
    qualifier: string,
    tags: Array<string>
  },
  onComponentChange: {} => void,
  router: Object
};
*/

export default class App extends React.PureComponent {
  /*:: props: Props; */
  /*:: state: Object; */

  static contextTypes = {
    router: PropTypes.object
  };

  componentDidMount() {
    if (this.isPortfolio()) {
      this.context.router.replace({
        pathname: '/portfolio',
        query: { id: this.props.component.key }
      });
    }
    if (isShortLivingBranch(this.props.branch)) {
      this.context.router.replace(getProjectBranchUrl(this.props.component.key, this.props.branch));
    }
  }

  isPortfolio() {
    return this.props.component.qualifier === 'VW' || this.props.component.qualifier === 'SVW';
  }

  render() {
    const { branch, component } = this.props;

    if (this.isPortfolio() || isShortLivingBranch(branch)) {
      return null;
    }

    if (['FIL', 'UTS'].includes(component.qualifier)) {
      return (
        <div className="page page-limited">
          <SourceViewer branch={getBranchName(branch)} component={component.key} />
        </div>
      );
    }

    if (!component.analysisDate) {
      return <EmptyOverview component={component} />;
    }

    return (
      <OverviewApp
        branch={branch}
        component={component}
        onComponentChange={this.props.onComponentChange}
      />
    );
  }
}
