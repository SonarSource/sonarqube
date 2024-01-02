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
import { ContentCell, HoverLink, Note, QualifierIcon } from 'design-system';
import * as React from 'react';
import { To } from 'react-router-dom';
import { fillBranchLike } from '../../../helpers/branch-like';
import { limitComponentName, splitPath } from '../../../helpers/path';
import { getComponentDrilldownUrlWithSelection, getProjectUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier, isApplication, isProject } from '../../../types/component';
import { MeasurePageView } from '../../../types/measures';
import { MetricKey } from '../../../types/metrics';
import { ComponentMeasure, ComponentMeasureEnhanced, Metric } from '../../../types/types';

export interface ComponentCellProps {
  branchLike?: BranchLike;
  component: ComponentMeasureEnhanced;
  metric: Metric;
  rootComponent: ComponentMeasure;
  view: MeasurePageView;
}

const COMPONENT_PATH_MAX_CHARS = 50;

export default function ComponentCell(props: ComponentCellProps) {
  const { branchLike, component, metric, rootComponent, view } = props;

  let head = '';
  let tail = component.name;

  if (
    view === MeasurePageView.list &&
    (
      [
        ComponentQualifier.File,
        ComponentQualifier.TestFile,
        ComponentQualifier.Directory,
      ] as string[]
    ).includes(component.qualifier) &&
    component.path
  ) {
    ({ head, tail } = splitPath(component.path));
  }

  let path: To;
  const targetKey = component.refKey || rootComponent.key;
  const selectionKey = component.refKey ? '' : component.key;

  // drilldown by default
  path = getComponentDrilldownUrlWithSelection(
    targetKey,
    selectionKey,
    metric.key,
    component.branch ? fillBranchLike(component.branch) : branchLike,
    view,
  );

  // This metric doesn't exist for project
  if (metric.key === MetricKey.projects && isProject(component.qualifier)) {
    path = getProjectUrl(targetKey, component.branch);
  }

  // Those metric doesn't exist for application and project
  if (
    ([MetricKey.releasability_rating, MetricKey.alert_status] as string[]).includes(metric.key) &&
    (isApplication(component.qualifier) || isProject(component.qualifier))
  ) {
    path = getProjectUrl(targetKey, component.branch);
  }

  return (
    <ContentCell className="sw-py-3">
      <HoverLink
        aria-hidden
        tabIndex={-1}
        icon={<QualifierIcon qualifier={component.qualifier} />}
        to={path}
        title={component.path}
      />
      <HoverLink to={path} title={component.path} className="sw-flex sw-flex-wrap">
        {head.length > 0 && <Note>{limitComponentName(head, COMPONENT_PATH_MAX_CHARS)}/</Note>}
        <strong>{tail}</strong>
      </HoverLink>
    </ContentCell>
  );
}
