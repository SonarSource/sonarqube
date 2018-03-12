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
import { Component, BranchLike } from '../../../app/types';
import { isShortLivingBranch } from '../../../helpers/branches';
import { getShortLivingBranchUrl, getCodeUrl } from '../../../helpers/urls';

interface Props {
  branchLike?: BranchLike;
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
    const { branchLike, component } = this.props;

    if (this.isPortfolio()) {
      this.context.router.replace({
        pathname: '/portfolio',
        query: { id: component.key }
      });
    } else if (this.isFile()) {
      this.context.router.replace(
        getCodeUrl(component.breadcrumbs[0].key, branchLike, component.key)
      );
    } else if (isShortLivingBranch(branchLike)) {
      this.context.router.replace(getShortLivingBranchUrl(component.key, branchLike.name));
    }
  }

  isPortfolio = () => ['VW', 'SVW'].includes(this.props.component.qualifier);

  isFile = () => ['FIL', 'UTS'].includes(this.props.component.qualifier);

  render() {
    const { branchLike, component } = this.props;

    if (this.isPortfolio() || this.isFile() || isShortLivingBranch(branchLike)) {
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
        branchLike={branchLike}
        component={component}
        onComponentChange={this.props.onComponentChange}
      />
    );
  }
}
