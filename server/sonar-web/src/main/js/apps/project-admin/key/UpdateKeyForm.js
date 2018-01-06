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
import PropTypes from 'prop-types';
import UpdateKeyConfirmation from './views/UpdateKeyConfirmation';
import { translate } from '../../../helpers/l10n';

export default class UpdateKeyForm extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object.isRequired
  };

  state = {};

  componentWillMount() {
    this.handleInputChange = this.handleInputChange.bind(this);
    this.handleUpdateClick = this.handleUpdateClick.bind(this);
    this.handleResetClick = this.handleResetClick.bind(this);
  }

  handleInputChange(e) {
    const key = e.target.value;
    this.setState({ key });
  }

  handleUpdateClick(e) {
    e.preventDefault();
    e.target.blur();

    const newKey = this.refs.newKey.value;

    new UpdateKeyConfirmation({
      newKey,
      component: this.props.component,
      onChange: this.props.onKeyChange
    }).render();
  }

  handleResetClick(e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ key: null });
  }

  render() {
    const { component } = this.props;

    const value = this.state.key != null ? this.state.key : component.key;

    const hasChanged = this.state.key != null && this.state.key !== component.key;

    return (
      <div className="js-fine-grained-update" data-key={component.key}>
        <input
          ref="newKey"
          className="input-super-large big-spacer-right"
          type="text"
          value={value}
          onChange={this.handleInputChange}
        />

        <button disabled={!hasChanged} onClick={this.handleUpdateClick}>
          {translate('update_verb')}
        </button>

        <button className="spacer-left" disabled={!hasChanged} onClick={this.handleResetClick}>
          {translate('reset_verb')}
        </button>
      </div>
    );
  }
}
