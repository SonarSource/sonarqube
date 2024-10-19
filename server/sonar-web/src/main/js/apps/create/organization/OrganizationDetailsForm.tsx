/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import OrganizationAvatarInput from '../components/OrganizationAvatarInput';
import OrganizationKeyInput from '../components/OrganizationKeyInput';
import OrganizationNameInput from '../components/OrganizationNameInput';
import OrganizationUrlInput from '../components/OrganizationUrlInput';
import { translate } from "../../../helpers/l10n";
import { Organization, OrganizationBase } from "../../../types/types";
import OrganizationDescriptionInput from '../components/OrganizationDescriptionInput';
import { Button, ButtonVariety, IconChevronDown, Spinner } from "@sonarsource/echoes-react";

type RequiredOrganization = Required<OrganizationBase>;

interface Props {
  infoBlock?: React.ReactNode;
  keyReadOnly?: boolean;
  createOrganization: (organization: Organization) => Promise<void>;
  organization?: Organization;
  submitText: string;
}

interface State {
  additional: boolean;
  avatar?: string;
  description?: string;
  kee?: string;
  name?: string;
  submitting: boolean;
  url?: string;
}

type ValidState = Pick<State, Exclude<keyof State, RequiredOrganization>> & RequiredOrganization;

export default class OrganizationDetailsForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const { organization } = props;
    this.state = {
      additional: false,
      avatar: (organization && organization.avatar) || '',
      description: (organization && organization.description) || '',
      kee: (organization && organization.kee) || undefined,
      name: (organization && organization.name) || '',
      submitting: false,
      url: (organization && organization.url) || ''
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  canSubmit(state: State): state is ValidState {
    return Boolean(
      state.kee !== undefined &&
        state.name !== undefined &&
        state.description !== undefined &&
        state.avatar !== undefined &&
        state.url !== undefined
    );
  }

  handleAdditionalClick = () => {
    this.setState(state => ({ additional: !state.additional }));
  };

  handleAvatarUpdate = (avatar: string | undefined) => {
    this.setState({ avatar });
  };

  handleDescriptionUpdate = (value:string|undefined) => {
    this.setState({ description: value });
  };

  handleKeyUpdate = (kee: string | undefined) => {
    this.setState({ kee });
  };

  handleNameUpdate = (name: string | undefined) => {
    this.setState({ name });
  };

  handleUrlUpdate = (url: string | undefined) => {
    this.setState({ url });
  };

  handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const { state } = this;
    if (this.canSubmit(state)) {
      this.setState({ submitting: true });
      this.props
        .createOrganization({
          avatar: state.avatar,
          description: state.description,
          kee: state.kee,
          name: state.name,
          url: state.url
        })
        .then(this.stopSubmitting, this.stopSubmitting);
    }
  };

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  render() {
    const { submitting } = this.state;
    const { infoBlock, keyReadOnly } = this.props;
    return (
      <form id="organization-form" onSubmit={this.handleSubmit}>
        {!keyReadOnly && (
          <OrganizationKeyInput initialValue={this.state.kee} onChange={this.handleKeyUpdate} />
        )}
        <div className="big-spacer-top">
          <Button onClick={this.handleAdditionalClick}>
            {translate(
              this.state.additional
                ? 'onboarding.create_organization.hide_additional_info'
                : 'onboarding.create_organization.add_additional_info'
            )}

            <IconChevronDown />
          </Button>
        </div>
        <div className="js-additional-info" hidden={!this.state.additional}>
          <div className="big-spacer-top">
            <label htmlFor="organization-display-name">
              <strong>{translate('onboarding.create_organization.display_name')}</strong>
            </label>
            <div className="little-spacer-top">
              <OrganizationNameInput isEditMode={false} showHelpIcon={true} initialValue={this.state.name} onChange={this.handleNameUpdate} />
            </div>
            <div className="note abs-width-400">
              {translate('onboarding.create_organization.display_name.description')}
            </div>
          </div>
          <div className="big-spacer-top">
            <OrganizationAvatarInput
              initialValue={this.state.avatar}
              name={this.state.name}
              onChange={this.handleAvatarUpdate}
            />
          </div>
          <div className="big-spacer-top">
            <label htmlFor="organization-description">
              <strong>{translate('onboarding.create_organization.description')}</strong>
            </label>
            <div className="little-spacer-top">
              <OrganizationDescriptionInput 
                onChange={this.handleDescriptionUpdate}
                value={this.state.description}>
                </OrganizationDescriptionInput>
                <div className="note abs-width-400">
                  {translate('organization.description.description')}
                </div>
            </div>
          </div>
          <div className="big-spacer-top">
            <OrganizationUrlInput initialValue={this.state.url} onChange={this.handleUrlUpdate} />
            <div className="note abs-width-400">
                {translate('organization.url.description')}
              </div>
          </div>
        </div>

        {infoBlock}

        <div className="display-flex-center big-spacer-top">
          <Button
            variety={ButtonVariety.Primary}
            disabled={submitting || !this.canSubmit(this.state)}
            type="submit"
          >
            {this.props.submitText}
          </Button>
          <Spinner isLoading={submitting} className="spacer-left" />
        </div>
      </form>
    );
  }
}
