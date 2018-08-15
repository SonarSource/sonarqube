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
import ListIcon from '../../../components/icons-components/ListIcon';
import TreeIcon from '../../../components/icons-components/TreeIcon';
import TreemapIcon from '../../../components/icons-components/TreemapIcon';
import Select from '../../../components/controls/Select';
import { hasList, hasTree, hasTreemap } from '../utils';
import { translate } from '../../../helpers/l10n';
import { Metric } from '../../../app/types';

interface Props {
  className?: string;
  metric: Metric;
  handleViewChange: (view: string) => void;
  view: string;
}

export default class MeasureViewSelect extends React.PureComponent<Props> {
  getOptions = () => {
    const { metric } = this.props;
    const options = [];
    if (hasList(metric.key)) {
      options.push({
        icon: <ListIcon />,
        label: translate('component_measures.tab.list'),
        value: 'list'
      });
    }
    if (hasTree(metric.key)) {
      options.push({
        icon: <TreeIcon />,
        label: translate('component_measures.tab.tree'),
        value: 'tree'
      });
    }
    if (hasTreemap(metric.key, metric.type)) {
      options.push({
        icon: <TreemapIcon />,
        label: translate('component_measures.tab.treemap'),
        value: 'treemap'
      });
    }
    return options;
  };

  handleChange = (option: { value: string }) => {
    return this.props.handleViewChange(option.value);
  };

  renderOption = (option: { icon: JSX.Element; label: string }) => {
    return (
      <>
        {option.icon}
        <span className="little-spacer-left">{option.label}</span>
      </>
    );
  };

  renderValue = (value: { icon: JSX.Element }) => {
    return value.icon;
  };

  render() {
    return (
      <Select
        autoBlur={true}
        className={this.props.className}
        clearable={false}
        onChange={this.handleChange}
        optionRenderer={this.renderOption}
        options={this.getOptions()}
        searchable={false}
        value={this.props.view}
        valueRenderer={this.renderValue}
      />
    );
  }
}
