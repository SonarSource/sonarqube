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
import { translate } from '../../../helpers/l10n';

export default class EncryptionForm extends React.PureComponent {
  static propTypes = {
    encryptedValue: PropTypes.string,
    encryptValue: PropTypes.func.isRequired,
    generateSecretKey: PropTypes.func.isRequired
  };

  state = { value: '' };

  handleEncrypt(e) {
    e.preventDefault();
    this.props.encryptValue(this.state.value);
  }

  handleGenerateNewKey(e) {
    e.preventDefault();
    this.props.generateSecretKey();
  }

  render() {
    return (
      <div id="encryption-form-container">
        <div className="spacer-bottom">{translate('encryption.form_intro')}</div>

        <form
          id="encryption-form"
          className="big-spacer-bottom"
          onSubmit={e => this.handleEncrypt(e)}>
          <textarea
            autoFocus={true}
            className="input-super-large"
            id="encryption-form-value"
            onChange={e => this.setState({ value: e.target.value })}
            required={true}
            rows={3}
            value={this.state.value}
          />
          <div className="spacer-top">
            <button>{translate('encryption.encrypt')}</button>
          </div>
        </form>

        {this.props.encryptedValue != null && (
          <div>
            {translate('encryption.encrypted_value')}
            {': '}
            <input
              id="encrypted-value"
              className="input-clear input-code input-super-large"
              type="text"
              readOnly={true}
              value={this.props.encryptedValue}
            />
          </div>
        )}

        <div className="huge-spacer-top bordered-top">
          <div
            className="big-spacer-top spacer-bottom"
            dangerouslySetInnerHTML={{ __html: translate('encryption.form_note') }}
          />
          <form id="encryption-new-key-form" onSubmit={e => this.handleGenerateNewKey(e)}>
            <button>{translate('encryption.generate_new_secret_key')}</button>
          </form>
        </div>
      </div>
    );
  }
}
