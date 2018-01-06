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

export default class UpdateForm extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object.isRequired,
    onKeyChange: PropTypes.func.isRequired
  };

  state = { newKey: null };

  handleSubmit(e) {
    e.preventDefault();

    const newKey = this.refs.newKey.value;

    new UpdateKeyConfirmation({
      newKey,
      component: this.props.component,
      onChange: this.props.onKeyChange
    }).render();
  }

  handleChange(e) {
    const newKey = e.target.value;
    this.setState({ newKey });
  }

  handleReset(e) {
    e.preventDefault();
    this.setState({ newKey: null });
  }

  render() {
    const value = this.state.newKey != null ? this.state.newKey : this.props.component.key;

    const hasChanged = value !== this.props.component.key;

    return (
      <form onSubmit={this.handleSubmit.bind(this)}>
        <input
          ref="newKey"
          id="update-key-new-key"
          className="input-super-large"
          value={value}
          type="text"
          placeholder={translate('update_key.new_key')}
          required={true}
          onChange={this.handleChange.bind(this)}
        />

        <div className="spacer-top">
          <button id="update-key-submit" disabled={!hasChanged}>
            {translate('update_verb')}
          </button>{' '}
          <button
            id="update-key-reset"
            disabled={!hasChanged}
            onClick={this.handleReset.bind(this)}>
            {translate('reset_verb')}
          </button>
        </div>
      </form>
    );
  }
}
