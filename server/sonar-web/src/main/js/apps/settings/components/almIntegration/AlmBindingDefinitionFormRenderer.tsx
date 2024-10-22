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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FlagMessage, Modal, PageContentFontWrapper, Spinner } from '~design-system';
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

export interface Props {
  alm: AlmKeys;
  bitbucketVariant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
  canSubmit: boolean;
  errorListElementRef: React.RefObject<HTMLDivElement>;
  formData: AlmBindingDefinition;
  isUpdate: boolean;
  onBitbucketVariantChange: (
    bitbucketVariant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud,
  ) => void;
  onCancel: () => void;
  onFieldChange: (fieldId: keyof AlmBindingDefinition, value: string) => void;
  onSubmit: () => void;
  submitting: boolean;
  validationError?: string;
}

export default class AlmBindingDefinitionFormRenderer extends React.PureComponent<Readonly<Props>> {
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
    const { isUpdate, canSubmit, submitting, validationError, errorListElementRef } = this.props;
    const header = translate('settings.almintegration.form.header', isUpdate ? 'edit' : 'create');
    const FORM_ID = `settings.almintegration.form.${isUpdate ? 'edit' : 'create'}`;

    const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
      event.preventDefault();
      this.props.onSubmit();
    };

    const formBody = (
      <form id={FORM_ID} onSubmit={handleSubmit}>
        <PageContentFontWrapper className="sw-typo-default" ref={errorListElementRef}>
          {validationError && !canSubmit && (
            <FlagMessage variant="error" className="sw-w-full sw-mb-2">
              <div>
                <p>{translate('settings.almintegration.configuration_invalid')}</p>
                <ul>
                  <li>{validationError}</li>
                </ul>
              </div>
            </FlagMessage>
          )}
          {this.renderForm()}
        </PageContentFontWrapper>
      </form>
    );

    return (
      <Modal
        headerTitle={header}
        isScrollable
        onClose={this.props.onCancel}
        body={formBody}
        primaryButton={
          <>
            <Spinner loading={submitting} />
            <Button
              form={FORM_ID}
              type="submit"
              hasAutoFocus
              isDisabled={!canSubmit || submitting}
              variety={ButtonVariety.Primary}
            >
              {translate('settings.almintegration.form.save')}
            </Button>
          </>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
