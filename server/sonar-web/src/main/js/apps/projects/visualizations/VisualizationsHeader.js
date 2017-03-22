/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Select from 'react-select';
import { translate } from '../../../helpers/l10n';
import { VISUALIZATIONS } from '../utils';

export default class VisualizationsHeader extends React.PureComponent {
  props: {
    onVisualizationChange: (string) => void,
    visualization: string
  };

  handleChange = (option: { value: string }) => {
    this.props.onVisualizationChange(option.value);
  };

  render() {
    const options = VISUALIZATIONS.map(option => ({
      value: option,
      label: option === 'quality'
        ? translate('projects.quality_model')
        : translate('metric', option, 'name')
    }));

    return (
      <header className="boxed-group-header">
        <Select
          className="input-medium"
          clearable={false}
          onChange={this.handleChange}
          options={options}
          searchable={false}
          value={this.props.visualization}
        />
      </header>
    );
  }
}
