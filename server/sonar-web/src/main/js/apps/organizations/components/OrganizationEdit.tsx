/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import { debounce } from 'lodash';
import OrganizationDelete from './OrganizationDelete';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { translate } from "../../../helpers/l10n";
import OrganizationUrlInput from '../../create/components/OrganizationUrlInput';
import { SubmitButton } from "../../../components/controls/buttons";
import { Organization } from "../../../types/types";
import OrganizationAvatar from "./OrganizationAvatar";
import { withOrganizationContext } from "../OrganizationContext";
import { updateOrganization } from "../../../api/organizations";

interface Props {
  organization: Organization;
}

interface State {
  loading: boolean;
  avatar: string;
  avatarImage: string;
  description: string;
  name: string;
  url: string;
}

export class OrganizationEdit extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
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

  handleAvatarInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = event.target;
    this.setState({ avatar: value });
    this.changeAvatarImage(value);
  };

  changeAvatarImage = (value: string) => {
    this.setState({ avatarImage: value });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const changes = {
      avatar: this.state.avatar,
      description: this.state.description,
      name: this.state.name,
      url: this.state.url
    };
    this.setState({ loading: true });
    updateOrganization(this.props.organization.kee, { ...changes, kee: this.props.organization.kee })
        .then(this.stopLoading, this.stopLoading);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleUrlUpdate = (url: string) => {
    this.setState({ url });
  };

  canSubmit = (state: State) => {
    return Boolean(
        state.name !== undefined &&
        state.description !== undefined &&
        state.avatar !== undefined &&
        state.url !== undefined
    );
  }

  render() {
    const { organization } = this.props;
    const title = translate('organization.settings');

    const showDelete = organization.actions && organization.actions.delete;

    return (
        <div className="page page-limited">
          <Helmet title={title}/>

          <header className="page-header">
            <h1 className="page-title">{title}</h1>
          </header>

          <div className="boxed-group boxed-group-inner">
            <h2 className="boxed-title">{translate('organization.details')}</h2>
            <form onSubmit={this.handleSubmit}>
              <div className="form-field">
                <label htmlFor="organization-name">
                  {translate('organization.name')}
                  <em className="mandatory">*</em>
                </label>
                <input
                    className="input-super-large"
                    disabled={this.state.loading}
                    id="organization-name"
                    maxLength={80}
                    name="name"
                    onChange={e => this.setState({ name: e.target.value })}
                    required={true}
                    type="text"
                    value={this.state.name}
                />
                <div className="form-field-description">
                  {translate('organization.name.description')}
                </div>
              </div>
              <div className="form-field">
                <label htmlFor="organization-avatar">{translate('organization.avatar')}</label>
                <input
                    className="input-super-large"
                    disabled={this.state.loading}
                    id="organization-avatar"
                    maxLength={256}
                    name="avatar"
                    onChange={this.handleAvatarInputChange}
                    placeholder={translate('onboarding.create_organization.avatar.placeholder')}
                    type="text"
                    value={this.state.avatar}
                />
                <div className="form-field-description">
                  {translate('organization.avatar.description')}
                </div>
                {(this.state.avatarImage || this.state.name) && (
                    <div className="spacer-top">
                      <div className="little-spacer-bottom">
                        {translate('organization.avatar.preview')}
                        {':'}
                      </div>
                      <OrganizationAvatar
                          organization={{
                            avatar: this.state.avatarImage || undefined,
                            name: this.state.name || ''
                          }}
                      />
                    </div>
                )}
              </div>
              <div className="form-field">
                <label htmlFor="organization-description">{translate('description')}</label>
                <textarea
                    className="input-super-large"
                    disabled={this.state.loading}
                    id="organization-description"
                    maxLength={256}
                    name="description"
                    onChange={e => this.setState({ description: e.target.value })}
                    rows={3}
                    value={this.state.description}
                />
                <div className="form-field-description">
                  {translate('organization.description.description')}
                </div>
              </div>
              <div className="form-field">
                <OrganizationUrlInput initialValue={this.state.url} onChange={this.handleUrlUpdate}/>
                <div className="form-field-description">
                  {translate('organization.url.description')}
                </div>
              </div>
              <SubmitButton
                  disabled={this.state.loading || !this.canSubmit(this.state)}>{translate('save')}</SubmitButton>
              {this.state.loading && <i className="spinner spacer-left"/>}
            </form>
          </div>

          {showDelete && <OrganizationDelete/>}
        </div>
    );
  }
}

export default whenLoggedIn(withOrganizationContext(OrganizationEdit));
