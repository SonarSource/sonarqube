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
import { ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import Modal from '../../../../components/controls/Modal';
import { Alert } from '../../../../components/ui/Alert';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { translate } from '../../../../helpers/l10n';
import {
  AlmBindingDefinition,
  AlmKeys,
  AzureBindingDefinition,
  BitbucketCloudBindingDefinition,
  BitbucketServerBindingDefinition,
  GithubBindingDefinition,
  GitlabBindingDefinition,
} from '../../../../types/alm-settings';
import AzureForm from './AzureForm';
import BitbucketForm from './BitbucketForm';
import GithubForm from './GithubForm';
import GitlabForm from './GitlabForm';

export interface AlmBindingDefinitionFormProps {
  alm: AlmKeys;
  isUpdate: boolean;
  canSubmit: boolean;
  onCancel: () => void;
  onSubmit: () => void;
  onFieldChange: (fieldId: keyof AlmBindingDefinition, value: string) => void;
  formData: AlmBindingDefinition;
  submitting: boolean;
  bitbucketVariant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
  onBitbucketVariantChange: (
    bitbucketVariant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud
  ) => void;
  validationError?: string;
}

export default class AlmBindingDefinitionFormRenderer extends React.PureComponent<AlmBindingDefinitionFormProps> {
  renderForm = () => {
    const { alm, formData, isUpdate, bitbucketVariant } = this.props;

    switch (alm) {
      case AlmKeys.GitLab:
        return (
          <GitlabForm
            onFieldChange={this.props.onFieldChange}
            formData={formData as GitlabBindingDefinition}
          />
        );
      case AlmKeys.Azure:
        return (
          <AzureForm
            onFieldChange={this.props.onFieldChange}
            formData={formData as AzureBindingDefinition}
          />
        );
      case AlmKeys.GitHub:
        return (
          <GithubForm
            onFieldChange={this.props.onFieldChange}
            formData={formData as GithubBindingDefinition}
          />
        );
      case AlmKeys.BitbucketServer:
        return (
          <BitbucketForm
            onFieldChange={this.props.onFieldChange}
            formData={
              formData as BitbucketServerBindingDefinition | BitbucketCloudBindingDefinition
            }
            isUpdate={isUpdate}
            variant={bitbucketVariant}
            onVariantChange={this.props.onBitbucketVariantChange}
          />
        );
      default:
        return null;
    }
  };

  render() {
    const { isUpdate, canSubmit, submitting, validationError } = this.props;
    const header = translate('settings.almintegration.form.header', isUpdate ? 'edit' : 'create');

    const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
      event.preventDefault();
      this.props.onSubmit();
    };

    return (
      <Modal
        contentLabel={header}
        onRequestClose={this.props.onCancel}
        shouldCloseOnOverlayClick={false}
        size="medium"
      >
        <form className="views-form" onSubmit={handleSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body modal-container">
            {this.renderForm()}
            {validationError && !canSubmit && (
              <Alert variant="error">
                <p className="spacer-bottom">
                  {translate('settings.almintegration.configuration_invalid')}
                </p>
                <ul className="list-styled">
                  <li>{validationError}</li>
                </ul>
              </Alert>
            )}
          </div>

          <div className="modal-foot">
            <SubmitButton disabled={!canSubmit || submitting}>
              {translate('settings.almintegration.form.save')}
              <DeferredSpinner className="spacer-left" loading={submitting} />
            </SubmitButton>
            <ResetButtonLink onClick={this.props.onCancel}>{translate('cancel')}</ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
