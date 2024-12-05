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
import { TopBar } from '~design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import NCDAutoUpdateMessage from '../../../../components/new-code-definition/NCDAutoUpdateMessage';
import { ComponentMissingMqrMetricsMessage } from '../../../../components/shared/ComponentMissingMqrMetricsMessage';
import { getBranchLikeDisplayName } from '../../../../helpers/branch-like';
import { translate } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import { useCurrentBranchQuery } from '../../../../queries/branch';
import { ProjectAlmBindingConfigurationErrors } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Component } from '../../../../types/types';
import RecentHistory from '../../RecentHistory';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../available-features/withAvailableFeatures';
import ComponentNavProjectBindingErrorNotif from './ComponentNavProjectBindingErrorNotif';
import Header from './Header';
import Menu from './Menu';

export interface ComponentNavProps extends WithAvailableFeaturesProps {
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  projectBindingErrors?: ProjectAlmBindingConfigurationErrors;
}

function ComponentNav(props: Readonly<ComponentNavProps>) {
  const { component, hasFeature, isInProgress, isPending, projectBindingErrors } = props;

  const { data: branchLike } = useCurrentBranchQuery(component);

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

  const branchName =
    hasFeature(Feature.BranchSupport) || !isDefined(branchLike)
      ? undefined
      : getBranchLikeDisplayName(branchLike);

  return (
    <>
      <TopBar id="context-navigation" aria-label={translate('qualifier', component.qualifier)}>
        <div className="sw-min-h-10 sw-flex sw-justify-between">
          <Header component={component} />
        </div>
        <Menu component={component} isInProgress={isInProgress} isPending={isPending} />
      </TopBar>
      <NCDAutoUpdateMessage branchName={branchName} component={component} />
      <ComponentMissingMqrMetricsMessage component={component} />
      {projectBindingErrors !== undefined && (
        <ComponentNavProjectBindingErrorNotif component={component} />
      )}
    </>
  );
}

export default withAvailableFeatures(ComponentNav);
