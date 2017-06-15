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
import Modal from 'react-modal';
import { debounce } from 'lodash';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import { translate } from '../../../helpers/l10n';
import { createOrganization } from '../../organizations/actions';

type State = {
  loading: boolean,
  avatar: string,
  avatarImage: string,
  description: string,
  key: string,
  name: string,
  url: string
};

class CreateOrganizationForm extends React.PureComponent {
  mounted: boolean;
  state: State;
  props: {
    createOrganization: (fields: {}) => Promise<*>,
    router: { push: string => void }
  };

  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      avatar: '',
      avatarImage: '',
      description: '',
      key: '',
      name: '',
      url: ''
    };
    this.changeAvatarImage = debounce(this.changeAvatarImage, 500);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  closeForm = () => {
    this.props.router.push('/account/organizations');
  };

  stopProcessing = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  stopProcessingAndClose = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
    this.closeForm();
  };

  handleAvatarInputChange = (e: Object) => {
    const { value } = e.target;
    this.setState({ avatar: value });
    this.changeAvatarImage(value);
  };

  changeAvatarImage = (value: string) => {
    this.setState({ avatarImage: value });
  };

  handleSubmit = (e: Object) => {
    e.preventDefault();
    const organization: Object = { name: this.state.name };
    if (this.state.avatar) {
      Object.assign(organization, { avatar: this.state.avatar });
    }
    if (this.state.description) {
      Object.assign(organization, { description: this.state.description });
    }
    if (this.state.key) {
      Object.assign(organization, { key: this.state.key });
    }
    if (this.state.url) {
      Object.assign(organization, { url: this.state.url });
    }
    this.setState({ loading: true });
    this.props
      .createOrganization(organization)
      .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render() {
    return (
      <Modal
        isOpen={true}
        contentLabel="modal form"
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{translate('my_account.create_organization')}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="organization-name">
                {translate('organization.name')}
                <em className="mandatory">*</em>
              </label>
              <input
                id="organization-name"
                autoFocus={true}
                name="name"
                required={true}
                type="text"
                minLength="2"
                maxLength="64"
                value={this.state.name}
                disabled={this.state.loading}
                onChange={e => this.setState({ name: e.target.value })}
              />
              <div className="modal-field-description">
                {translate('organization.name.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-key">
                {translate('organization.key')}
              </label>
              <input
                id="organization-key"
                name="key"
                type="text"
                minLength="2"
                maxLength="64"
                value={this.state.key}
                disabled={this.state.loading}
                onChange={e => this.setState({ key: e.target.value })}
              />
              <div className="modal-field-description">
                {translate('organization.key.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-avatar">
                {translate('organization.avatar')}
              </label>
              <input
                id="organization-avatar"
                name="avatar"
                type="text"
                maxLength="256"
                value={this.state.avatar}
                disabled={this.state.loading}
                onChange={this.handleAvatarInputChange}
              />
              <div className="modal-field-description">
                {translate('organization.avatar.description')}
              </div>
              {!!this.state.avatarImage &&
                <div className="spacer-top spacer-bottom">
                  <div className="little-spacer-bottom">
                    {translate('organization.avatar.preview')}
                    {':'}
                  </div>
                  <img src={this.state.avatarImage} alt="" height={30} />
                </div>}
            </div>
            <div className="modal-field">
              <label htmlFor="organization-description">
                {translate('description')}
              </label>
              <textarea
                id="organization-description"
                name="description"
                rows="3"
                maxLength="256"
                value={this.state.description}
                disabled={this.state.loading}
                onChange={e => this.setState({ description: e.target.value })}
              />
              <div className="modal-field-description">
                {translate('organization.description.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-url">
                {translate('organization.url')}
              </label>
              <input
                id="organization-url"
                name="url"
                type="text"
                maxLength="256"
                value={this.state.url}
                disabled={this.state.loading}
                onChange={e => this.setState({ url: e.target.value })}
              />
              <div className="modal-field-description">
                {translate('organization.url.description')}
              </div>
            </div>
          </div>

          <footer className="modal-foot">
            <div>
              <button disabled={this.state.loading} type="submit">
                {this.state.loading && <i className="spinner little-spacer-right" />}
                {translate('create')}
              </button>
              <button type="reset" className="button-link" onClick={this.closeForm}>
                {translate('cancel')}
              </button>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }
}

const mapStateToProps = null;

const mapDispatchToProps = { createOrganization };

export default connect(mapStateToProps, mapDispatchToProps)(withRouter(CreateOrganizationForm));
