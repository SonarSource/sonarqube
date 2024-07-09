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
import { Select } from '@sonarsource/echoes-react';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { MeasurePageView } from '../../../types/measures';
import { Metric } from '../../../types/types';
import { hasList, hasTree, hasTreemap } from '../utils';

export interface MeasureViewSelectProps {
  className?: string;
  handleViewChange: (view: MeasurePageView) => void;
  metric: Metric;
  view: MeasurePageView;
}

export default function MeasureViewSelect(props: Readonly<MeasureViewSelectProps>) {
  const { metric, view, className, handleViewChange } = props;

  const measureViewOptions = React.useMemo(() => {
    const options = [];
    if (hasTree(metric.key)) {
      options.push({
        label: translate('component_measures.tab.tree'),
        value: MeasurePageView.tree,
      });
    }
    if (hasList(metric.key)) {
      options.push({
        label: translate('component_measures.tab.list'),
        value: MeasurePageView.list,
      });
    }
    if (hasTreemap(metric.key, metric.type)) {
      options.push({
        label: translate('component_measures.tab.treemap'),
        value: MeasurePageView.treemap,
      });
    }
    return options;
  }, [metric]);

  return (
    <Select
      ariaLabelledBy="measures-view-selection-label"
      className={className}
      data={measureViewOptions}
      isNotClearable
      onChange={handleViewChange}
      value={view}
    />
  );
}
