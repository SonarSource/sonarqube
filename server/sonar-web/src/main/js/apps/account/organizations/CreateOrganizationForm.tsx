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
import * as React from 'react';
import { debounce } from 'lodash';
import { connect } from 'react-redux';
import * as PropTypes from 'prop-types';
import { createOrganization } from '../../organizations/actions';
import { Organization } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';

interface DispatchProps {
  createOrganization: (fields: Partial<Organization>) => Promise<{ key: string }>;
}

interface Props extends DispatchProps {
  onClose: () => void;
  onCreate: (organization: { key: string }) => void;
}

interface State {
  avatar: string;
  avatarImage: string;
  description: string;
  key: string;
  loading: boolean;
  name: string;
  url: string;
}

class CreateOrganizationForm extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      avatar: '',
      avatarImage: '',
      description: '',
      key: '',
      loading: false,
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

  stopProcessing = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleAvatarInputChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const { value } = event.currentTarget;
    this.setState({ avatar: value });
    this.changeAvatarImage(value);
  };

  changeAvatarImage = (value: string) => {
    this.setState({ avatarImage: value });
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ name: event.currentTarget.value });

  handleKeyChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ key: event.currentTarget.value });

  handleDescriptionChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) =>
    this.setState({ description: event.currentTarget.value });

  handleUrlChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ url: event.currentTarget.value });

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const organization = { name: this.state.name };
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
    this.props.createOrganization(organization).then(this.props.onCreate, this.stopProcessing);
  };

  render() {
    return (
      <Modal contentLabel="modal form" onRequestClose={this.props.onClose}>
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
                minLength={2}
                maxLength={64}
                value={this.state.name}
                disabled={this.state.loading}
                onChange={this.handleNameChange}
              />
              <div className="modal-field-description">
                {translate('organization.name.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-key">{translate('organization.key')}</label>
              <input
                id="organization-key"
                name="key"
                type="text"
                minLength={2}
                maxLength={64}
                value={this.state.key}
                disabled={this.state.loading}
                onChange={this.handleKeyChange}
              />
              <div className="modal-field-description">
                {translate('organization.key.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-avatar">{translate('organization.avatar')}</label>
              <input
                id="organization-avatar"
                name="avatar"
                type="text"
                maxLength={256}
                value={this.state.avatar}
                disabled={this.state.loading}
                onChange={this.handleAvatarInputChange}
              />
              <div className="modal-field-description">
                {translate('organization.avatar.description')}
              </div>
              {!!this.state.avatarImage && (
                <div className="spacer-top spacer-bottom">
                  <div className="little-spacer-bottom">
                    {translate('organization.avatar.preview')}
                    {':'}
                  </div>
                  <img src={this.state.avatarImage} alt="" height={30} />
                </div>
              )}
            </div>
            <div className="modal-field">
              <label htmlFor="organization-description">{translate('description')}</label>
              <textarea
                id="organization-description"
                name="description"
                rows={3}
                maxLength={256}
                value={this.state.description}
                disabled={this.state.loading}
                onChange={this.handleDescriptionChange}
              />
              <div className="modal-field-description">
                {translate('organization.description.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-url">{translate('organization.url')}</label>
              <input
                id="organization-url"
                name="url"
                type="text"
                maxLength={256}
                value={this.state.url}
                disabled={this.state.loading}
                onChange={this.handleUrlChange}
              />
              <div className="modal-field-description">
                {translate('organization.url.description')}
              </div>
            </div>
          </div>

          <footer className="modal-foot">
            <div>
              {this.state.loading && <i className="spinner spacer-right" />}
              <button disabled={this.state.loading} type="submit">
                {translate('create')}
              </button>
              <button className="button-link" onClick={this.props.onClose} type="reset">
                {translate('cancel')}
              </button>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }
}

const mapDispatchToProps: DispatchProps = { createOrganization: createOrganization as any };

export default connect(null, mapDispatchToProps)(CreateOrganizationForm);
