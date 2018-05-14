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
import { SubmitButton } from '../../../components/ui/buttons';

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
                disabled={this.state.loading}
                id="organization-name"
                maxLength="64"
                name="name"
                onChange={e => this.setState({ name: e.target.value })}
                required={true}
                type="text"
                value={this.state.name}
              />
              <div className="modal-field-description">
                {translate('organization.name.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-avatar">{translate('organization.avatar')}</label>
              <input
                disabled={this.state.loading}
                id="organization-avatar"
                maxLength="256"
                name="avatar"
                onChange={this.handleAvatarInputChange}
                type="text"
                value={this.state.avatar}
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
                  <img alt="" height={30} src={this.state.avatarImage} />
                </div>
              )}
            </div>
            <div className="modal-field">
              <label htmlFor="organization-description">{translate('description')}</label>
              <textarea
                disabled={this.state.loading}
                id="organization-description"
                maxLength="256"
                name="description"
                onChange={e => this.setState({ description: e.target.value })}
                rows="3"
                value={this.state.description}
              />
              <div className="modal-field-description">
                {translate('organization.description.description')}
              </div>
            </div>
            <div className="modal-field">
              <label htmlFor="organization-url">{translate('organization.url')}</label>
              <input
                disabled={this.state.loading}
                id="organization-url"
                maxLength="256"
                name="url"
                onChange={e => this.setState({ url: e.target.value })}
                type="text"
                value={this.state.url}
              />
              <div className="modal-field-description">
                {translate('organization.url.description')}
              </div>
            </div>
            <div className="modal-field">
              <SubmitButton disabled={this.state.loading}>{translate('save')}</SubmitButton>
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
