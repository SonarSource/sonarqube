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
import classNames from 'classnames';
import * as React from 'react';
import ContextNavBar from '../../../../components/ui/ContextNavBar';
import {
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingResponse,
} from '../../../../types/alm-settings';
import { BranchLike } from '../../../../types/branch-like';
import { ComponentQualifier } from '../../../../types/component';
import { Task, TaskStatuses, TaskWarning } from '../../../../types/tasks';
import { Component } from '../../../../types/types';
import { rawSizes } from '../../../theme';
import RecentHistory from '../../RecentHistory';
import ComponentNavBgTaskNotif from './ComponentNavBgTaskNotif';
import ComponentNavProjectBindingErrorNotif from './ComponentNavProjectBindingErrorNotif';
import Header from './Header';
import HeaderMeta from './HeaderMeta';
import Menu from './Menu';
import InfoDrawer from './projectInformation/InfoDrawer';
import ProjectInformation from './projectInformation/ProjectInformation';

export interface ComponentNavProps {
  branchLikes: BranchLike[];
  currentBranchLike: BranchLike | undefined;
  component: Component;
  currentTask?: Task;
  currentTaskOnSameBranch?: boolean;
  isInProgress?: boolean;
  isPending?: boolean;
  onComponentChange: (changes: Partial<Component>) => void;
  onWarningDismiss: () => void;
  projectBinding?: ProjectAlmBindingResponse;
  projectBindingErrors?: ProjectAlmBindingConfigurationErrors;
  warnings: TaskWarning[];
}

const ALERT_HEIGHT = 30;
const BRANCHLIKE_TOGGLE_ADDED_HEIGHT = 6;

export default function ComponentNav(props: ComponentNavProps) {
  const {
    branchLikes,
    component,
    currentBranchLike,
    currentTask,
    currentTaskOnSameBranch,
    isInProgress,
    isPending,
    projectBinding,
    projectBindingErrors,
    warnings,
  } = props;
  const { contextNavHeightRaw, globalNavHeightRaw } = rawSizes;

  const [displayProjectInfo, setDisplayProjectInfo] = React.useState(false);

  React.useEffect(() => {
    const { breadcrumbs, key, name } = component;
    const { qualifier } = breadcrumbs[breadcrumbs.length - 1];
    if (
      [
        ComponentQualifier.Project,
        ComponentQualifier.Portfolio,
        ComponentQualifier.Application,
        ComponentQualifier.Developper,
      ].includes(qualifier as ComponentQualifier)
    ) {
      RecentHistory.add(key, name, qualifier.toLowerCase());
    }
  }, [component, component.key]);

  let contextNavHeight = contextNavHeightRaw;

  let bgTaskNotifComponent;
  if (isInProgress || isPending || (currentTask && currentTask.status === TaskStatuses.Failed)) {
    bgTaskNotifComponent = (
      <ComponentNavBgTaskNotif
        component={component}
        currentTask={currentTask}
        currentTaskOnSameBranch={currentTaskOnSameBranch}
        isInProgress={isInProgress}
        isPending={isPending}
      />
    );
    contextNavHeight += ALERT_HEIGHT;
  }

  let prDecoNotifComponent;
  if (projectBindingErrors !== undefined) {
    prDecoNotifComponent = <ComponentNavProjectBindingErrorNotif component={component} />;
    contextNavHeight += ALERT_HEIGHT;
  }

  if (branchLikes.length) {
    contextNavHeight += BRANCHLIKE_TOGGLE_ADDED_HEIGHT;
  }

  return (
    <ContextNavBar
      height={contextNavHeight}
      id="context-navigation"
      notif={
        <>
          {bgTaskNotifComponent}
          {prDecoNotifComponent}
        </>
      }
    >
      <div
        className={classNames('display-flex-center display-flex-space-between', {
          'padded-bottom little-padded-top': warnings.length === 0,
          'little-padded-bottom': warnings.length > 0,
        })}
      >
        <Header
          branchLikes={branchLikes}
          component={component}
          currentBranchLike={currentBranchLike}
          projectBinding={projectBinding}
        />
        <HeaderMeta
          branchLike={currentBranchLike}
          component={component}
          onWarningDismiss={props.onWarningDismiss}
          warnings={warnings}
        />
      </div>
      <Menu
        branchLike={currentBranchLike}
        branchLikes={branchLikes}
        component={component}
        isInProgress={isInProgress}
        isPending={isPending}
        onToggleProjectInfo={() => setDisplayProjectInfo(!displayProjectInfo)}
        projectInfoDisplayed={displayProjectInfo}
      />
      <InfoDrawer
        displayed={displayProjectInfo}
        onClose={() => setDisplayProjectInfo(false)}
        top={globalNavHeightRaw + contextNavHeight}
      >
        <ProjectInformation
          branchLike={currentBranchLike}
          component={component}
          onComponentChange={props.onComponentChange}
        />
      </InfoDrawer>
    </ContextNavBar>
  );
}
