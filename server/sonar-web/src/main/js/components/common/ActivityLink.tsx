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
import { StandoutLink } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { getActivityUrl, getMeasureHistoryUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import { GraphType } from '../../types/project-activity';
import { isCustomGraph } from '../activity-graph/utils';
import './ActivityLink.css';

export interface ActivityLinkProps {
  branchLike?: BranchLike;
  component: string;
  graph?: GraphType;
  label?: string;
  metric?: string;
}

export default function ActivityLink(props: ActivityLinkProps) {
  const { branchLike, component, graph, label, metric } = props;
  return (
    <StandoutLink
      className="sw-body-sm-highlight"
      to={
        metric !== undefined && graph !== undefined && isCustomGraph(graph)
          ? getMeasureHistoryUrl(component, metric, branchLike)
          : getActivityUrl(component, branchLike, graph)
      }
    >
      {label || translate('portfolio.activity_link')}
    </StandoutLink>
  );
}
