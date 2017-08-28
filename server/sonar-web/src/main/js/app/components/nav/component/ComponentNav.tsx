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
import * as React from 'react';
import ComponentNavFavorite from './ComponentNavFavorite';
import ComponentNavBreadcrumbs from './ComponentNavBreadcrumbs';
import ComponentNavMeta from './ComponentNavMeta';
import ComponentNavMenu from './ComponentNavMenu';
import ComponentNavBranch from './ComponentNavBranch';
import RecentHistory from '../../RecentHistory';
import { Branch, Component, ComponentConfiguration } from '../../../types';
import ContextNavBar from '../../../../components/nav/ContextNavBar';
import { getTasksForComponent } from '../../../../api/ce';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import './ComponentNav.css';

interface Props {
  branches: Branch[];
  currentBranch: Branch;
  component: Component;
  conf: ComponentConfiguration;
  location: {};
}

interface State {
  incremental?: boolean;
  isFailed?: boolean;
  isInProgress?: boolean;
  isPending?: boolean;
}

export default class ComponentNav extends React.PureComponent<Props, State> {
  mounted: boolean;

  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.loadStatus();
    this.populateRecentHistory();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStatus = () => {
    getTasksForComponent(this.props.component.key).then((r: any) => {
      if (this.mounted) {
        this.setState({
          isPending: r.queue.some((task: any) => task.status === STATUSES.PENDING),
          isInProgress: r.queue.some((task: any) => task.status === STATUSES.IN_PROGRESS),
          isFailed: r.current && r.current.status === STATUSES.FAILED,
          incremental: r.current && r.current.incremental
        });
      }
    });
  };

  populateRecentHistory = () => {
    const { breadcrumbs } = this.props.component;
    const { qualifier } = breadcrumbs[breadcrumbs.length - 1];
    if (['TRK', 'VW', 'APP', 'DEV'].indexOf(qualifier) !== -1) {
      RecentHistory.add(
        this.props.component.key,
        this.props.component.name,
        qualifier.toLowerCase(),
        this.props.component.organization
      );
    }
  };

  render() {
    return (
      <ContextNavBar id="context-navigation" height={65}>
        <ComponentNavFavorite
          component={this.props.component.key}
          favorite={this.props.component.isFavorite}
        />

        <ComponentNavBreadcrumbs
          component={this.props.component}
          breadcrumbs={this.props.component.breadcrumbs}
        />

        <ComponentNavBranch
          branches={this.props.branches}
          currentBranch={this.props.currentBranch}
          // to close dropdown on any location change
          location={this.props.location}
          project={this.props.component}
        />

        <ComponentNavMeta
          branch={this.props.currentBranch}
          component={this.props.component}
          conf={this.props.conf}
          incremental={this.state.incremental}
        />

        <ComponentNavMenu
          branch={this.props.currentBranch}
          component={this.props.component}
          conf={this.props.conf}
          // to re-render selected menu item
          location={this.props.location}
        />
      </ContextNavBar>
    );
  }
}
