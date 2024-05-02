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
import {
  ButtonSecondary,
  FormField,
  InputField,
  InputTextArea,
  Modal,
  RadioButton,
} from 'design-system';
import * as React from 'react';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import { createApplication } from '../../../api/application';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onCreate: (application: { key: string; qualifier: ComponentQualifier }) => Promise<void>;
}

interface State {
  description: string;
  key: string;
  name: string;
  visibility: Visibility;
  submitting: boolean;
}

export default class CreateApplicationForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      description: '',
      key: '',
      name: '',
      visibility: Visibility.Public,
      submitting: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ key: event.currentTarget.value });
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleVisibilityChange = (visibility: Visibility) => {
    this.setState({ visibility });
  };

  handleFormSubmit = (event: React.FormEvent) => {
    event.preventDefault();

    const { name, description, key, visibility } = this.state;
    this.setState({ submitting: true });
    return createApplication(name, description, key.length > 0 ? key : undefined, visibility)
      .then(({ application }) => {
        if (this.mounted) {
          this.setState({ submitting: false });
          this.props.onCreate({
            key: application.key,
            qualifier: ComponentQualifier.Application,
          });
        }
      })
      .catch(() => {
        this.setState({ submitting: false });
      });
  };

  renderForm = () => {
    const { name, description, key, visibility } = this.state;

    return (
      <form onSubmit={this.handleFormSubmit} id="create-application-form">
        <MandatoryFieldsExplanation className="modal-field" />

        <FormField
          htmlFor="view-edit-name"
          label={translate('name')}
          required
          requiredAriaLabel={translate('field_required')}
        >
          <InputField
            autoFocus
            id="view-edit-name"
            maxLength={100}
            name="name"
            onChange={this.handleNameChange}
            type="text"
            size="full"
            value={name}
          />
        </FormField>
        <FormField htmlFor="view-edit-description" label={translate('description')}>
          <InputTextArea
            id="view-edit-description"
            name="description"
            onChange={this.handleDescriptionChange}
            size="full"
            value={description}
          />
        </FormField>
        <FormField
          htmlFor="view-edit-key"
          label={translate('key')}
          description={translate('onboarding.create_application.key.description')}
        >
          <InputField
            autoComplete="off"
            id="view-edit-key"
            maxLength={256}
            name="key"
            onChange={this.handleKeyChange}
            type="text"
            size="full"
            value={key}
          />
        </FormField>

        <FormField label={translate('visibility')}>
          {[Visibility.Public, Visibility.Private].map((v) => (
            <RadioButton
              key={v}
              checked={visibility === v}
              value={v}
              onCheck={this.handleVisibilityChange}
            >
              {translate('visibility', v)}
            </RadioButton>
          ))}
        </FormField>
      </form>
    );
  };

  render() {
    const { submitting } = this.state;
    const header = translate('qualifiers.create.APP');
    const submitDisabled = !this.state.name.length;

    return (
      <Modal
        onClose={this.props.onClose}
        headerTitle={header}
        isScrollable
        loading={submitting}
        body={this.renderForm()}
        primaryButton={
          <ButtonSecondary
            disabled={submitting || submitDisabled}
            form="create-application-form"
            type="submit"
          >
            {translate('create')}
          </ButtonSecondary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
