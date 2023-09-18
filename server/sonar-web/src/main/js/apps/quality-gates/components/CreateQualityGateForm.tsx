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
import { ButtonSecondary, FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import { createQualityGate } from '../../../api/quality-gates';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';

interface Props {
  onClose: () => void;
  onCreate: () => Promise<void>;
  router: Router;
}

interface State {
  name: string;
}

export class CreateQualityGateForm extends React.PureComponent<Props, State> {
  state: State = { name: '' };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.handleCreate();
  };

  handleCreate = async () => {
    const { name } = this.state;

    if (name !== undefined) {
      const qualityGate = await createQualityGate({ name });
      await this.props.onCreate();
      this.props.onClose();
      this.props.router.push(getQualityGateUrl(qualityGate.name));
    }
  };

  render() {
    const { name } = this.state;

    const body = (
      <form onSubmit={this.handleFormSubmit}>
        <MandatoryFieldsExplanation className="modal-field" />
        <FormField
          htmlFor="quality-gate-form-name"
          label={translate('name')}
          required
          requiredAriaLabel={translate('field_required')}
        >
          <InputField
            className="sw-mb-1"
            autoComplete="off"
            id="quality-gate-form-name"
            maxLength={256}
            name="key"
            onChange={this.handleNameChange}
            type="text"
            size="full"
            value={name}
          />
        </FormField>
      </form>
    );

    return (
      <Modal
        onClose={this.props.onClose}
        headerTitle={translate('quality_gates.create')}
        isScrollable
        body={body}
        primaryButton={
          <ButtonSecondary
            disabled={name === null || name === ''}
            form="create-application-form"
            type="submit"
            onClick={this.handleCreate}
          >
            {translate('quality_gate.create')}
          </ButtonSecondary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}

export default withRouter(CreateQualityGateForm);
