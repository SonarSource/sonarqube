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
import { connect } from 'react-redux';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { sendTestEmail } from '../../../api/settings';
import { parseError } from '../../../helpers/request';
import { getCurrentUser } from '../../../store/rootReducer';
import { SubmitButton } from '../../../components/ui/buttons';

class EmailForm extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      recipient: this.props.currentUser.email,
      subject: translate('email_configuration.test.subject'),
      message: translate('email_configuration.test.message_text'),
      loading: false,
      success: false,
      error: null
    };
  }

  handleFormSubmit = event => {
    event.preventDefault();
    this.setState({ success: false, error: null, loading: true });
    const { recipient, subject, message } = this.state;
    sendTestEmail(recipient, subject, message).then(
      () => this.setState({ success: true, loading: false }),
      error => parseError(error).then(message => this.setState({ error: message, loading: false }))
    );
  };

  render() {
    return (
      <div className="huge-spacer-top">
        <h3 className="spacer-bottom">{translate('email_configuration.test.title')}</h3>

        <form onSubmit={this.handleFormSubmit} style={{ marginLeft: 201 }}>
          {this.state.success && (
            <div className="modal-field">
              <div className="alert alert-success">
                {translateWithParameters(
                  'email_configuration.test.email_was_sent_to_x',
                  this.state.recipient
                )}
              </div>
            </div>
          )}

          {this.state.error != null && (
            <div className="modal-field">
              <div className="alert alert-danger">{this.state.error}</div>
            </div>
          )}

          <div className="modal-field">
            <label htmlFor="test-email-to">
              {translate('email_configuration.test.to_address')}
              <em className="mandatory">*</em>
            </label>
            <input
              className="settings-large-input"
              disabled={this.state.loading}
              id="test-email-to"
              onChange={e => this.setState({ recipient: e.target.value })}
              required={true}
              type="email"
              value={this.state.recipient}
            />
          </div>
          <div className="modal-field">
            <label htmlFor="test-email-subject">
              {translate('email_configuration.test.subject')}
            </label>
            <input
              className="settings-large-input"
              disabled={this.state.loading}
              id="test-email-subject"
              onChange={e => this.setState({ subject: e.target.value })}
              type="text"
              value={this.state.subject}
            />
          </div>
          <div className="modal-field">
            <label htmlFor="test-email-message">
              {translate('email_configuration.test.message')}
              <em className="mandatory">*</em>
            </label>
            <textarea
              className="settings-large-input"
              disabled={this.state.loading}
              id="test-email-title"
              onChange={e => this.setState({ message: e.target.value })}
              required={true}
              rows="5"
              value={this.state.message}
            />
          </div>

          <div className="modal-field">
            {this.state.loading && <i className="spacer-right spinner" />}
            <SubmitButton disabled={this.state.loading}>
              {translate('email_configuration.test.send')}
            </SubmitButton>
          </div>
        </form>
      </div>
    );
  }
}

const mapStateToProps = state => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(EmailForm);
