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
import ComponentNavHeader from './ComponentNavHeader';
import ComponentNavMeta from './ComponentNavMeta';
import ComponentNavMenu from './ComponentNavMenu';
import ComponentNavBgTaskNotif from './ComponentNavBgTaskNotif';
import RecentHistory from '../../RecentHistory';
import * as theme from '../../../theme';
import { Branch, Component } from '../../../types';
import ContextNavBar from '../../../../components/nav/ContextNavBar';
import { Task } from '../../../../api/ce';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import './ComponentNav.css';

interface Props {
  branches: Branch[];
  currentBranch?: Branch;
  component: Component;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
  location: {};
}

export default class ComponentNav extends React.PureComponent<Props> {
  mounted = false;

  componentDidMount() {
    this.populateRecentHistory();
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.component.key !== prevProps.component.key) {
      this.populateRecentHistory();
    }
  }

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
    const { currentTask, isInProgress, isPending } = this.props;
    let notifComponent;
    if (isInProgress || isPending || (currentTask && currentTask.status === STATUSES.FAILED)) {
      notifComponent = (
        <ComponentNavBgTaskNotif
          component={this.props.component}
          currentTask={currentTask}
          isInProgress={isInProgress}
          isPending={isPending}
        />
      );
    }
    return (
      <ContextNavBar
        id="context-navigation"
        height={notifComponent ? theme.contextNavHeightRaw + 20 : theme.contextNavHeightRaw}
        notif={notifComponent}>
        <ComponentNavHeader
          branches={this.props.branches}
          component={this.props.component}
          currentBranch={this.props.currentBranch}
          // to close dropdown on any location change
          location={this.props.location}
        />
        <ComponentNavMeta branch={this.props.currentBranch} component={this.props.component} />
        <ComponentNavMenu
          branch={this.props.currentBranch}
          component={this.props.component}
          // to re-render selected menu item
          location={this.props.location}
        />
      </ContextNavBar>
    );
  }
}
