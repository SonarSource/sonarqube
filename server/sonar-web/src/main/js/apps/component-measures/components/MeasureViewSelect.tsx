/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Select from 'sonar-ui-common/components/controls/Select';
import ListIcon from 'sonar-ui-common/components/icons/ListIcon';
import TreeIcon from 'sonar-ui-common/components/icons/TreeIcon';
import TreemapIcon from 'sonar-ui-common/components/icons/TreemapIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { hasList, hasTree, hasTreemap, View } from '../utils';

interface Props {
  className?: string;
  metric: T.Metric;
  handleViewChange: (view: View) => void;
  view: View;
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
    return this.props.handleViewChange(option.value as View);
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
      <Select
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
