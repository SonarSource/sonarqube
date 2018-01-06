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

export default class GenerateSecretKeyForm extends React.PureComponent {
  static propTypes = {
    secretKey: PropTypes.string,
    generateSecretKey: PropTypes.func.isRequired
  };

  handleSubmit(e) {
    e.preventDefault();
    this.props.generateSecretKey();
  }

  render() {
    return (
      <div id="generate-secret-key-form-container">
        {this.props.secretKey != null ? (
          <div>
            <div className="big-spacer-bottom">
              <h3 className="spacer-bottom">{translate('encryption.secret_key')}</h3>
              <input
                id="secret-key"
                className="input-large"
                type="text"
                readOnly={true}
                value={this.props.secretKey}
              />
            </div>

            <h3 className="spacer-bottom">{translate('encryption.how_to_use')}</h3>

            <div
              className="markdown"
              dangerouslySetInnerHTML={{ __html: translate('encryption.how_to_use.content') }}
            />
          </div>
        ) : (
          <div>
            <p
              className="spacer-bottom"
              dangerouslySetInnerHTML={{ __html: translate('ecryption.secret_key_description') }}
            />

            <form id="generate-secret-key-form" onSubmit={e => this.handleSubmit(e)}>
              <button>{translate('encryption.generate_secret_key')}s</button>
            </form>
          </div>
        )}
      </div>
    );
  }
}
