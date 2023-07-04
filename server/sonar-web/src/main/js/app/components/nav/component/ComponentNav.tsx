/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { TopBar } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import {
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingResponse,
} from '../../../../types/alm-settings';
import { ComponentQualifier } from '../../../../types/component';
import { Task } from '../../../../types/tasks';
import { Component } from '../../../../types/types';
import RecentHistory from '../../RecentHistory';
import ComponentNavProjectBindingErrorNotif from './ComponentNavProjectBindingErrorNotif';
import Header from './Header';
import HeaderMeta from './HeaderMeta';
import Menu from './Menu';

export interface ComponentNavProps {
  component: Component;
  currentTask?: Task;
  isInProgress?: boolean;
  isPending?: boolean;
  projectBinding?: ProjectAlmBindingResponse;
  projectBindingErrors?: ProjectAlmBindingConfigurationErrors;
}

export default function ComponentNav(props: ComponentNavProps) {
  const { component, currentTask, isInProgress, isPending, projectBinding, projectBindingErrors } =
    props;

  React.useEffect(() => {
    const { breadcrumbs, key, name } = component;
    const { qualifier } = breadcrumbs[breadcrumbs.length - 1];
    if (
      [
        ComponentQualifier.Project,
        ComponentQualifier.Portfolio,
        ComponentQualifier.Application,
      ].includes(qualifier as ComponentQualifier)
    ) {
      RecentHistory.add(key, name, qualifier.toLowerCase());
    }
  }, [component, component.key]);

  let prDecoNotifComponent;
  if (projectBindingErrors !== undefined) {
    prDecoNotifComponent = <ComponentNavProjectBindingErrorNotif component={component} />;
  }

  return (
    <>
      <TopBar id="context-navigation" aria-label={translate('qualifier', component.qualifier)}>
        <div className="sw-min-h-10 sw-flex sw-justify-between">
          <Header component={component} projectBinding={projectBinding} />
          <HeaderMeta
            component={component}
            currentTask={currentTask}
            isInProgress={isInProgress}
            isPending={isPending}
          />
        </div>
        <Menu component={component} isInProgress={isInProgress} isPending={isPending} />
      </TopBar>
      {prDecoNotifComponent}
    </>
  );
}
