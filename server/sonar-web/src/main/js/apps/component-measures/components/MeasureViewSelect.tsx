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
import { components, OptionProps, SingleValueProps } from 'react-select';
import Select from '../../../components/controls/Select';
import ListIcon from '../../../components/icons/ListIcon';
import TreeIcon from '../../../components/icons/TreeIcon';
import TreemapIcon from '../../../components/icons/TreemapIcon';
import { translate } from '../../../helpers/l10n';
import { MeasurePageView } from '../../../types/measures';
import { Metric } from '../../../types/types';
import { hasList, hasTree, hasTreemap } from '../utils';

interface Props {
  className?: string;
  metric: Metric;
  handleViewChange: (view: MeasurePageView) => void;
  view: MeasurePageView;
}

interface ViewOption {
  icon: JSX.Element;
  label: string;
  value: string;
}

export default class MeasureViewSelect extends React.PureComponent<Props> {
  getOptions = () => {
    const { metric } = this.props;
    const options: ViewOption[] = [];
    if (hasTree(metric.key)) {
      options.push({
        icon: <TreeIcon />,
        label: translate('component_measures.tab.tree'),
        value: 'tree',
      });
    }
    if (hasList(metric.key)) {
      options.push({
        icon: <ListIcon />,
        label: translate('component_measures.tab.list'),
        value: 'list',
      });
    }
    if (hasTreemap(metric.key, metric.type)) {
      options.push({
        icon: <TreemapIcon />,
        label: translate('component_measures.tab.treemap'),
        value: 'treemap',
      });
    }
    return options;
  };

  handleChange = (option: ViewOption) => {
    return this.props.handleViewChange(option.value as MeasurePageView);
  };

  renderOption = (props: OptionProps<ViewOption, false>) => (
    <components.Option {...props} className="display-flex-center">
      {props.data.icon}
      <span className="little-spacer-left">{props.data.label}</span>
    </components.Option>
  );

  renderValue = (props: SingleValueProps<ViewOption>) => (
    <components.SingleValue {...props} className="display-flex-center">
      {props.data.icon}
      <span className="little-spacer-left">{props.data.label}</span>
    </components.SingleValue>
  );

  render() {
    const { className, view } = this.props;
    const options = this.getOptions();

    return (
      <Select
        aria-labelledby="measures-view-selection-label"
        blurInputOnSelect={true}
        className={className}
        onChange={this.handleChange}
        components={{
          Option: this.renderOption,
          SingleValue: this.renderValue,
        }}
        options={options}
        isSearchable={false}
        value={options.find((o) => o.value === view)}
      />
    );
  }
}
