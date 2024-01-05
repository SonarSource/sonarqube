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
import { ButtonPrimary, FormField, InputField, InputTextArea, Modal, Spinner } from 'design-system';
import * as React from 'react';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';

interface Props {
  confirmButtonText: string;
  header: string;
  onClose: () => void;
  onSubmit: (data: {
    description: string;
    name: string;
    projectKeyPattern: string;
  }) => Promise<void>;
  permissionTemplate?: { description?: string; name: string; projectKeyPattern?: string };
}

interface State {
  description: string;
  name: string;
  projectKeyPattern: string;
  submitting: boolean;
}

export default class Form extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      description: (props.permissionTemplate && props.permissionTemplate.description) || '',
      name: (props.permissionTemplate && props.permissionTemplate.name) || '',
      projectKeyPattern:
        (props.permissionTemplate && props.permissionTemplate.projectKeyPattern) || '',
      submitting: false,
    };
  }

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    this.setState({ submitting: true });
    return this.props
      .onSubmit({
        description: this.state.description,
        name: this.state.name,
        projectKeyPattern: this.state.projectKeyPattern,
      })
      .then(this.props.onClose);
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleProjectKeyPatternChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ projectKeyPattern: event.currentTarget.value });
  };

  render() {
    const formBody = (
      <form id="permission-template-form" onSubmit={this.handleSubmit}>
        <div>
          <div className="sw-mb-4">
            <MandatoryFieldsExplanation />
          </div>

          <FormField
            description={translate('should_be_unique')}
            label={translate('name')}
            htmlFor="permission-template-name"
            required
          >
            <InputField
              autoFocus
              id="permission-template-name"
              maxLength={256}
              name="name"
              onChange={this.handleNameChange}
              required
              type="text"
              value={this.state.name}
              size="full"
            />
          </FormField>

          <FormField label={translate('description')} htmlFor="permission-template-description">
            <InputTextArea
              id="permission-template-description"
              name="description"
              onChange={this.handleDescriptionChange}
              value={this.state.description}
              size="full"
            />
          </FormField>

          <FormField
            htmlFor="permission-template-project-key-pattern"
            label={translate('permission_template.key_pattern')}
            description={translate('permission_template.key_pattern.description')}
          >
            <InputField
              id="permission-template-project-key-pattern"
              maxLength={500}
              name="projectKeyPattern"
              onChange={this.handleProjectKeyPatternChange}
              type="text"
              value={this.state.projectKeyPattern}
              size="full"
            />
          </FormField>
        </div>
      </form>
    );

    return (
      <Modal
        secondaryButtonLabel={translate('cancel')}
        headerTitle={this.props.header}
        onClose={this.props.onClose}
        body={formBody}
        primaryButton={
          <>
            <Spinner loading={this.state.submitting} />
            <ButtonPrimary
              disabled={this.state.submitting}
              type="submit"
              form="permission-template-form"
              id="permission-template-submit"
            >
              {this.props.confirmButtonText}
            </ButtonPrimary>
          </>
        }
      />
    );
  }
}
