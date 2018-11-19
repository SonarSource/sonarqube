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
import React from 'react';
import { debounce } from 'lodash';
import RadioToggle from '../controls/RadioToggle';
import { translate } from '../../helpers/l10n';

export default class Controls extends React.PureComponent {
  componentWillMount() {
    this.search = debounce(this.search, 100);
  }

  search = () => {
    const query = this.refs.search.value;
    this.props.search(query);
  };

  onCheck = value => {
    switch (value) {
      case 'selected':
        this.props.loadSelected();
        break;
      case 'deselected':
        this.props.loadDeselected();
        break;
      default:
        this.props.loadAll();
    }
  };

  render() {
    const selectionOptions = [
      { value: 'selected', label: 'Selected' },
      { value: 'deselected', label: 'Not Selected' },
      { value: 'all', label: 'All' }
    ];

    return (
      <div className="select-list-control">
        <div className="pull-left">
          <RadioToggle
            name="select-list-selection"
            options={selectionOptions}
            onCheck={this.onCheck}
            value={this.props.selection}
          />
        </div>
        <div className="pull-right">
          <input
            onChange={this.search}
            ref="search"
            type="search"
            placeholder={translate('search_verb')}
            initialValue={this.props.query}
          />
        </div>
      </div>
    );
  }
}
