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

import { LinkStandalone } from '@sonarsource/echoes-react';
import {
  ButtonPrimary,
  Card,
  CenteredLayout,
  DarkLabel,
  FlagMessage,
  FormField,
  InputField,
  PageContentFontWrapper,
  Spinner,
  SubTitle,
  Title,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Location } from '../../components/hoc/withRouter';
import { translate } from '../../helpers/l10n';
import { getReturnUrl } from '../../helpers/urls';
import Unauthorized from '../sessions/components/Unauthorized';
import { DEFAULT_ADMIN_PASSWORD } from './constants';

export interface ChangeAdminPasswordAppRendererProps {
  canAdmin?: boolean;
  canSubmit?: boolean;
  confirmPasswordValue: string;
  location: Location;
  onConfirmPasswordChange: (password: string) => void;
  onPasswordChange: (password: string) => void;
  onSubmit: () => void;
  passwordValue: string;
  submitting: boolean;
  success: boolean;
}

const PASSWORD_FIELD_ID = 'user-password';
const CONFIRM_PASSWORD_FIELD_ID = 'confirm-user-password';

export default function ChangeAdminPasswordAppRenderer(
  props: Readonly<ChangeAdminPasswordAppRendererProps>,
) {
  const {
    canAdmin,
    canSubmit,
    confirmPasswordValue,
    location,
    passwordValue,
    submitting,
    success,
  } = props;

  if (!canAdmin) {
    return <Unauthorized />;
  }

  return (
    <CenteredLayout>
      <Helmet defer={false} title={translate('users.change_admin_password.page')} />

      <PageContentFontWrapper className="sw-body-sm sw-flex sw-flex-col sw-items-center sw-justify-center">
        <Card className="sw-mx-auto sw-mt-24 sw-w-abs-600 sw-flex sw-items-stretch sw-flex-col">
          {success ? (
            <FlagMessage className="sw-my-8" variant="success">
              <div>
                <p className="sw-mb-2">{translate('users.change_admin_password.form.success')}</p>

                {/* We must reload because we need a refresh of the /api/navigation/global call. */}
                <LinkStandalone to={getReturnUrl(location)} reloadDocument>
                  {translate('users.change_admin_password.form.continue_to_app')}
                </LinkStandalone>
              </div>
            </FlagMessage>
          ) : (
            <>
              <Title>{translate('users.change_admin_password.instance_is_at_risk')}</Title>

              <DarkLabel className="sw-mb-2">
                {translate('users.change_admin_password.header')}
              </DarkLabel>

              <p>{translate('users.change_admin_password.description')}</p>

              <form
                className="sw-mt-8"
                onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
                  e.preventDefault();
                  props.onSubmit();
                }}
              >
                <SubTitle className="sw-mb-4">
                  {translate('users.change_admin_password.form.header')}
                </SubTitle>

                <FormField
                  htmlFor={PASSWORD_FIELD_ID}
                  label={translate('users.change_admin_password.form.password')}
                  required
                >
                  <InputField
                    id={PASSWORD_FIELD_ID}
                    name="password"
                    onChange={(e: React.SyntheticEvent<HTMLInputElement>) => {
                      props.onPasswordChange(e.currentTarget.value);
                    }}
                    required
                    type="password"
                    value={passwordValue}
                  />
                </FormField>

                <FormField
                  description={
                    confirmPasswordValue === passwordValue &&
                    passwordValue === DEFAULT_ADMIN_PASSWORD && (
                      <FlagMessage className="sw-mt-2" variant="warning">
                        {translate('users.change_admin_password.form.cannot_use_default_password')}
                      </FlagMessage>
                    )
                  }
                  htmlFor={CONFIRM_PASSWORD_FIELD_ID}
                  label={translate('users.change_admin_password.form.confirm')}
                  required
                >
                  <InputField
                    id={CONFIRM_PASSWORD_FIELD_ID}
                    name="confirm-password"
                    onChange={(e: React.SyntheticEvent<HTMLInputElement>) => {
                      props.onConfirmPasswordChange(e.currentTarget.value);
                    }}
                    required
                    type="password"
                    value={confirmPasswordValue}
                  />
                </FormField>

                <ButtonPrimary
                  className="sw-mt-8"
                  disabled={!canSubmit || submitting}
                  type="submit"
                >
                  <Spinner className="sw-mr-2" loading={submitting} />

                  {translate('update_verb')}
                </ButtonPrimary>
              </form>
            </>
          )}
        </Card>
      </PageContentFontWrapper>
    </CenteredLayout>
  );
}
