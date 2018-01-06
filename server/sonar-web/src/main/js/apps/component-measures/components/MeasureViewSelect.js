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
// @flow
import React from 'react';
import ListIcon from '../../../components/icons-components/ListIcon';
import TreeIcon from '../../../components/icons-components/TreeIcon';
import TreemapIcon from '../../../components/icons-components/TreemapIcon';
import Select from '../../../components/controls/Select';
import { hasList, hasTree, hasTreemap } from '../utils';
import { translate } from '../../../helpers/l10n';
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {
  className?: string,
  metric: Metric,
  handleViewChange: (view: string) => void,
  view: string
}; */

export default class MeasureViewSelect extends React.PureComponent {
  /*:: props: Props; */

  getOptions = () => {
    const { metric } = this.props;
    const options = [];
    if (hasList(metric.key)) {
      options.push({
        value: 'list',
        label: (
          <div>
            <ListIcon className="little-spacer-right" />
            {translate('component_measures.tab.list')}
          </div>
        ),
        icon: <ListIcon />
      });
    }
    if (hasTree(metric.key)) {
      options.push({
        value: 'tree',
        label: (
          <div>
            <TreeIcon className="little-spacer-right" />
            {translate('component_measures.tab.tree')}
          </div>
        ),
        icon: <TreeIcon />
      });
    }
    if (hasTreemap(metric.key, metric.type)) {
      options.push({
        value: 'treemap',
        label: (
          <div>
            <TreemapIcon className="little-spacer-right" />
            {translate('component_measures.tab.treemap')}
          </div>
        ),
        icon: <TreemapIcon />
      });
    }
    return options;
  };

  handleChange = (option /*: { value: string } */) => this.props.handleViewChange(option.value);

  renderValue = (value /*: { icon: Element<*> } */) => value.icon;

  render() {
    return (
      <Select
        autoBlur={true}
        className={this.props.className}
        clearable={false}
        searchable={false}
        value={this.props.view}
        valueRenderer={this.renderValue}
        options={this.getOptions()}
        onChange={this.handleChange}
      />
    );
  }
}
