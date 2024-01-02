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
import { To } from 'react-router-dom';
import Link from '../../../components/common/Link';
import BranchIcon from '../../../components/icons/BranchIcon';
import LinkIcon from '../../../components/icons/LinkIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { fillBranchLike } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { splitPath } from '../../../helpers/path';
import { getComponentDrilldownUrlWithSelection, getProjectUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import {
  ComponentQualifier,
  isApplication,
  isPortfolioLike,
  isProject,
} from '../../../types/component';
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

export default function ComponentCell(props: ComponentCellProps) {
  const { branchLike, component, metric, rootComponent, view } = props;

  let head = '';
  let tail = component.name;

  if (
    view === 'list' &&
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
    view
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
    <td className="measure-details-component-cell">
      <div className="text-ellipsis">
        <Link
          className="link-no-underline"
          to={path}
          id={'component-measures-component-link-' + component.key}
        >
          {component.refKey && (
            <span className="big-spacer-right">
              <LinkIcon />
            </span>
          )}
          <span title={component.key}>
            <QualifierIcon className="little-spacer-right" qualifier={component.qualifier} />
            {head.length > 0 && <span className="note">{head}/</span>}
            <span>{tail}</span>
            {(isApplication(rootComponent.qualifier) || isPortfolioLike(rootComponent.qualifier)) &&
              (component.branch ? (
                <>
                  <BranchIcon className="spacer-left little-spacer-right" />
                  <span className="note">{component.branch}</span>
                </>
              ) : (
                <span className="spacer-left badge">{translate('branches.main_branch')}</span>
              ))}
          </span>
        </Link>
      </div>
    </td>
  );
}
