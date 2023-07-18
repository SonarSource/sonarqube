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
import OrganizationAvatarUrlInput from '../../create/components/OrganizationAvatarUrlInput';
import OrganizationNameInput from '../../create/components/OrganizationNameInput';
import OrganizationDescriptionInput from '../../create/components/OrganizationDescriptionInput';
import { addGlobalSuccessMessage } from '../../../../js/helpers/globalMessages';

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

  handleAvatarInputChange = (value:string) => {
    
    this.setState({ avatar: value });
    this.changeAvatarImage(value);
  };

  handleNameChange = (value:string) =>{
    this.setState({name: value});
  }

  handleDescriptionChange = (value:string) =>{
    this.setState({description: value});
  }

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
        .then(()=>{
          this.stopLoading,
          addGlobalSuccessMessage(translate('organization.updated'));
          setTimeout(()=>{
            window.location.reload();
          },2000)
          }, this.stopLoading);
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
                <OrganizationNameInput  isEditMode={true} showHelpIcon = {false} initialValue = {this.state.name} onChange={this.handleNameChange}></OrganizationNameInput>
                <div className="form-field-description">
                  {translate('organization.name.description')}
                </div>
                </div>
              <div className="form-field">
                <OrganizationAvatarUrlInput initialValue={this.state.avatar} onChange={this.handleAvatarInputChange}/>
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
                <OrganizationDescriptionInput  showHelpIcon = {false} initialValue = {this.state.description} onChange={this.handleDescriptionChange}></OrganizationDescriptionInput>
                
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
