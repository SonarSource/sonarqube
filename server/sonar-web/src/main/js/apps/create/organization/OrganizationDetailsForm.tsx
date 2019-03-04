/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';
import OrganizationAvatarInput from '../components/OrganizationAvatarInput';
import OrganizationKeyInput from '../components/OrganizationKeyInput';
import OrganizationUrlInput from '../components/OrganizationUrlInput';
import { ResetButtonLink, SubmitButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

type RequiredOrganization = Required<T.OrganizationBase>;

interface Props {
  infoBlock?: React.ReactNode;
  keyReadOnly?: boolean;
  onContinue: (organization: T.Organization) => Promise<void>;
  organization?: T.Organization;
  submitText: string;
}

interface State {
  additional: boolean;
  avatar?: string;
  description?: string;
  key?: string;
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
      key: (organization && organization.key) || undefined,
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
      state.key !== undefined &&
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

  handleDescriptionUpdate = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleKeyUpdate = (key: string | undefined) => {
    this.setState({ key });
  };

  handleNameUpdate = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
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
        .onContinue({
          avatar: state.avatar,
          description: state.description,
          key: state.key,
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
          <OrganizationKeyInput initialValue={this.state.key} onChange={this.handleKeyUpdate} />
        )}
        <div className="big-spacer-top">
          <ResetButtonLink onClick={this.handleAdditionalClick}>
            {translate(
              this.state.additional
                ? 'onboarding.create_organization.hide_additional_info'
                : 'onboarding.create_organization.add_additional_info'
            )}
            <DropdownIcon className="little-spacer-left" turned={this.state.additional} />
          </ResetButtonLink>
        </div>
        <div className="js-additional-info" hidden={!this.state.additional}>
          <div className="big-spacer-top">
            <label htmlFor="organization-display-name">
              <strong>{translate('onboarding.create_organization.display_name')}</strong>
            </label>
            <div className="little-spacer-top">
              <input
                className="input-super-large text-middle"
                id="organization-display-name"
                maxLength={255}
                onChange={this.handleNameUpdate}
                type="text"
                value={this.state.name}
              />
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
              <textarea
                className="input-super-large text-middle"
                id="organization-description"
                maxLength={256}
                onChange={this.handleDescriptionUpdate}
                rows={3}
                value={this.state.description}
              />
            </div>
          </div>
          <div className="big-spacer-top">
            <OrganizationUrlInput initialValue={this.state.url} onChange={this.handleUrlUpdate} />
          </div>
        </div>

        {infoBlock}

        <div className="display-flex-center big-spacer-top">
          <SubmitButton disabled={submitting || !this.canSubmit(this.state)}>
            {this.props.submitText}
          </SubmitButton>
          {submitting && <DeferredSpinner className="spacer-left" />}
        </div>
      </form>
    );
  }
}
