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
import { ButtonPrimary, FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import MandatoryFieldsExplanation from '../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../helpers/l10n';

interface Props {
  onClose: () => void;
  onSubmit: (name: string, url: string) => Promise<void>;
}

interface State {
  name: string;
  url: string;
}

const FORM_ID = 'create-link-form';

export default class CreationModal extends React.PureComponent<Props, State> {
  state: State = { name: '', url: '' };

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    return this.props.onSubmit(this.state.name, this.state.url).then(this.props.onClose);
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleUrlChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ url: event.currentTarget.value });
  };

  render() {
    const header = translate('project_links.create_new_project_link');

    const formBody = (
      <form id={FORM_ID} onSubmit={this.handleSubmit} className="sw-mb-2">
        <MandatoryFieldsExplanation />

        <FormField
          label={translate('project_links.name')}
          htmlFor="create-link-name"
          className="sw-mt-4"
          required
        >
          <InputField
            autoFocus
            required
            size="auto"
            id="create-link-name"
            maxLength={128}
            name="name"
            onChange={this.handleNameChange}
            type="text"
            value={this.state.name}
          />
        </FormField>

        <FormField label={translate('project_links.url')} htmlFor="create-link-url" required>
          <InputField
            size="auto"
            required
            id="create-link-url"
            maxLength={128}
            name="url"
            onChange={this.handleUrlChange}
            type="text"
            value={this.state.url}
          />
        </FormField>
      </form>
    );

    return (
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        body={formBody}
        primaryButton={
          <ButtonPrimary form={FORM_ID} type="submit">
            {translate('create')}
          </ButtonPrimary>
        }
      />
    );
  }
}
