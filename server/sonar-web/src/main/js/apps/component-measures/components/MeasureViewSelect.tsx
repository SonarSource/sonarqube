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
import { InputSelect } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { MeasurePageView } from '../../../types/measures';
import { Metric } from '../../../types/types';
import { hasList, hasTree, hasTreemap } from '../utils';

export interface MeasureViewSelectProps {
  className?: string;
  metric: Metric;
  handleViewChange: (view: MeasurePageView) => void;
  view: MeasurePageView;
}

interface ViewOption {
  label: string;
  value: MeasurePageView;
}

export default function MeasureViewSelect(props: MeasureViewSelectProps) {
  const { metric, view, className } = props;
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

  const handleChange = (option: ViewOption) => {
    return props.handleViewChange(option.value);
  };

  return (
    <InputSelect
      size="small"
      aria-labelledby="measures-view-selection-label"
      blurInputOnSelect
      className={className}
      onChange={handleChange}
      options={options}
      isSearchable={false}
      value={options.find((o) => o.value === view)}
    />
  );
}
