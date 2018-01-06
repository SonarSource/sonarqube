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
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { debounce } from 'lodash';
import { translate } from '../../../helpers/l10n';
/*:: import type { Organization } from '../../../store/organizations/duck'; */
import { getOrganizationByKey } from '../../../store/rootReducer';
import { updateOrganization } from '../actions';

/*::
type Props = {
  organization: Organization,
  updateOrganization: (string, Object) => Promise<*>
};
*/

class OrganizationEdit extends React.PureComponent {
  /*:: mounted: boolean; */

  /*:: props: Props; */

  /*:: state: {
    loading: boolean,
    avatar: string,
    avatarImage: string,
    description: string,
    name: string,
    url: string
  };
*/

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      loading: false,

      avatar: props.organization.avatar || '',
      avatarImage: props.organization.avatar || '',
      description: props.organization.description || '',
      name: props.organization.name,
      url: props.organization.url || ''
    };
    this.changeAvatarImage = debounce(this.changeAvatarImage, 500);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleAvatarInputChange = (e /*: Object */) => {
    const { value } = e.target;
    this.setState({ avatar: value });
    this.changeAvatarImage(value);
  };

  changeAvatarImage = (value /*: string */) => {
    this.setState({ avatarImage: value });
  };

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    const changes = {
      avatar: this.state.avatar,
      description: this.state.description,
      name: this.state.name,
      url: this.state.url
    };
    this.setState({ loading: true });
    this.props.updateOrganization(this.props.organization.key, changes).then(() => {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    });
  };

  render() {
    const title = translate('organization.edit');
    return (
      <div className="page page-limited">
        <Helmet title={title} />

        <header className="page-header">
          <h1 className="page-title">{title}</h1>
        </header>

        <div className="boxed-group boxed-group-inner">
          <form onSubmit={this.handleSubmit}>
            <div className="modal-field">
              <label htmlFor="organization-name">
                {translate('organization.name')}
                <em className="mandatory">*</em>
              </label>
              <input
                id="organization-name"
                name="name"
                required={true}
                type="text"
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
              <label htmlFor="organization-avatar">{translate('organization.avatar')}</label>
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
              <label htmlFor="organization-url">{translate('organization.url')}</label>
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
            <div className="modal-field">
              <button type="submit" disabled={this.state.loading}>
                {translate('save')}
              </button>
              {this.state.loading && <i className="spinner spacer-left" />}
            </div>
          </form>
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = { updateOrganization };

export default connect(null, mapDispatchToProps)(OrganizationEdit);

export const UnconnectedOrganizationEdit = OrganizationEdit;
