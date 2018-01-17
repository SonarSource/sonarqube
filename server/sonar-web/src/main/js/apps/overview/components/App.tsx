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
import * as PropTypes from 'prop-types';
import OverviewApp from './OverviewApp';
import EmptyOverview from './EmptyOverview';
import { getBranchName, isShortLivingBranch } from '../../../helpers/branches';
import { getProjectBranchUrl, getCodeUrl } from '../../../helpers/urls';
import { Branch, Component } from '../../../app/types';

interface Props {
  branch?: Branch;
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onComponentChange: (changes: Partial<Component>) => void;
}

export default class App extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object
  };

  componentDidMount() {
    const { branch, component } = this.props;

    if (this.isPortfolio()) {
      this.context.router.replace({
        pathname: '/portfolio',
        query: { id: component.key }
      });
    } else if (this.isFile()) {
      this.context.router.replace(
        getCodeUrl(component.breadcrumbs[0].key, getBranchName(branch), component.key)
      );
    } else if (isShortLivingBranch(branch)) {
      this.context.router.replace(getProjectBranchUrl(component.key, branch));
    }
  }

  isPortfolio = () => ['VW', 'SVW'].includes(this.props.component.qualifier);

  isFile = () => ['FIL', 'UTS'].includes(this.props.component.qualifier);

  render() {
    const { branch, component } = this.props;

    if (this.isPortfolio() || this.isFile() || isShortLivingBranch(branch)) {
      return null;
    }

    if (!component.analysisDate) {
      return (
        <EmptyOverview
          component={component.key}
          showWarning={!this.props.isPending && !this.props.isInProgress}
        />
      );
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
