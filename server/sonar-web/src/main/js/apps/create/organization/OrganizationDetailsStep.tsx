/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import OrganizationDetailsInput from './OrganizationDetailsInput';
import Step from '../../tutorials/components/Step';
import ValidationForm, { ChildrenProps } from '../../../components/controls/ValidationForm';
import { translate } from '../../../helpers/l10n';
import { ResetButtonLink, SubmitButton } from '../../../components/ui/buttons';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';
import { isUrl } from '../../../helpers/urls';
import { OrganizationBase } from '../../../app/types';
import { getOrganization } from '../../../api/organizations';

type Values = Required<OrganizationBase>;

const initialValues: Values = {
  avatar: '',
  description: '',
  name: '',
  key: '',
  url: ''
};

interface Props {
  onContinue: (organization: Required<OrganizationBase>) => Promise<void>;
}

interface State {
  additional: boolean;
}

export default class OrganizationDetailsStep extends React.PureComponent<Props, State> {
  state: State = { additional: false };

  handleAdditionalClick = () => {
    this.setState(state => ({ additional: !state.additional }));
  };

  checkFreeKey = (key: string) => {
    return getOrganization(key).then(organization => organization === undefined, () => true);
  };

  handleValidate = ({ avatar, name, key, url }: Values) => {
    const errors: { [P in keyof Values]?: string } = {};

    if (avatar.length > 0 && !isUrl(avatar)) {
      errors.avatar = translate('onboarding.create_organization.avatar.error');
    }

    if (name.length > 0 && (name.length < 2 || name.length > 64)) {
      errors.name = translate('onboarding.create_organization.display_name.error');
    }

    if (key.length < 2 || key.length > 32 || !/^[a-z0-9][a-z0-9-]*[a-z0-9]$/.test(key)) {
      errors.key = translate('onboarding.create_organization.organization_name.error');
    }

    if (url.length > 0 && !isUrl(url)) {
      errors.url = translate('onboarding.create_organization.url.error');
    }

    // don't try to check if the organization key is already taken if the key is invalid
    if (errors.key) {
      return Promise.reject(errors);
    }

    return this.checkFreeKey(key).then(free => {
      if (!free) {
        errors.key = translate('onboarding.create_organization.organization_name.taken');
      }
      return Object.keys(errors).length ? Promise.reject(errors) : Promise.resolve(errors);
    });
  };

  renderInnerForm = (props: ChildrenProps<Values>) => {
    const {
      dirty,
      errors,
      handleBlur,
      handleChange,
      isSubmitting,
      isValid,
      touched,
      values
    } = props;
    const commonProps = { dirty, isSubmitting, onBlur: handleBlur, onChange: handleChange };
    return (
      <>
        <OrganizationDetailsInput
          {...commonProps}
          description={translate('onboarding.create_organization.organization_name.description')}
          error={errors.key}
          id="organization-key"
          label={translate('onboarding.create_organization.organization_name')}
          name="key"
          required={true}
          touched={touched.key}
          value={values.key}>
          {props => <input autoFocus={true} {...props} />}
        </OrganizationDetailsInput>
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
            <OrganizationDetailsInput
              {...commonProps}
              description={translate('onboarding.create_organization.display_name.description')}
              error={errors.name}
              id="organization-display-name"
              label={translate('onboarding.create_organization.display_name')}
              name="name"
              touched={touched.name && values.name !== ''}
              value={values.name}>
              {props => <input {...props} />}
            </OrganizationDetailsInput>
          </div>
          <div className="big-spacer-top">
            <OrganizationDetailsInput
              {...commonProps}
              description={translate('onboarding.create_organization.avatar.description')}
              error={errors.avatar}
              id="organization-avatar"
              label={translate('onboarding.create_organization.avatar')}
              name="avatar"
              touched={touched.avatar && values.avatar !== ''}
              value={values.avatar}>
              {props => <input {...props} />}
            </OrganizationDetailsInput>
          </div>
          <div className="big-spacer-top">
            <OrganizationDetailsInput
              {...commonProps}
              error={errors.description}
              id="organization-description"
              label={translate('description')}
              name="description"
              touched={touched.description && values.description !== ''}
              value={values.description}>
              {props => <textarea {...props} rows={3} />}
            </OrganizationDetailsInput>
          </div>
          <div className="big-spacer-top">
            <OrganizationDetailsInput
              {...commonProps}
              error={errors.url}
              id="organization-url"
              label={translate('onboarding.create_organization.url')}
              name="url"
              touched={touched.url && values.url !== ''}
              value={values.url}>
              {props => <input {...props} />}
            </OrganizationDetailsInput>
          </div>
        </div>
        <div className="big-spacer-top">
          <SubmitButton disabled={isSubmitting || !isValid || !dirty}>
            {/* // TODO change me */}
            {translate('onboarding.create_organization.page.header')}
          </SubmitButton>
        </div>
      </>
    );
  };

  renderForm = () => {
    return (
      <div className="boxed-group-inner">
        <ValidationForm<Values>
          initialValues={initialValues}
          onSubmit={this.props.onContinue}
          validate={this.handleValidate}>
          {this.renderInnerForm}
        </ValidationForm>
      </div>
    );
  };

  render() {
    return (
      <Step
        finished={false}
        onOpen={() => {}}
        open={true}
        renderForm={this.renderForm}
        renderResult={() => <div />}
        stepNumber={1}
        stepTitle={translate('onboarding.create_organization.enter_org_details')}
      />
    );
  }
}
