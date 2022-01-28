/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import SelectLegacy from '../../../components/controls/SelectLegacy';
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

export default class MeasureViewSelect extends React.PureComponent<Props> {
  getOptions = () => {
    const { metric } = this.props;
    const options = [];
    if (hasTree(metric.key)) {
      options.push({
        icon: <TreeIcon />,
        label: translate('component_measures.tab.tree'),
        value: 'tree'
      });
    }
    if (hasList(metric.key)) {
      options.push({
        icon: <ListIcon />,
        label: translate('component_measures.tab.list'),
        value: 'list'
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
    return this.props.handleViewChange(option.value as MeasurePageView);
  };

  renderOption = (option: { icon: JSX.Element; label: string }) => {
    return (
      <>
        {option.icon}
        <span className="little-spacer-left">{option.label}</span>
      </>
    );
  };

  render() {
    return (
      <SelectLegacy
        autoBlur={true}
        className={this.props.className}
        clearable={false}
        onChange={this.handleChange}
        optionRenderer={this.renderOption}
        options={this.getOptions()}
        searchable={false}
        value={this.props.view}
        valueRenderer={this.renderOption}
      />
    );
  }
}
