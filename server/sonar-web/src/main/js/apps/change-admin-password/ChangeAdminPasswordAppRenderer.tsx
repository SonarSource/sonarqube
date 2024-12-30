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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import {
  ButtonPrimary,
  Card,
  CenteredLayout,
  DarkLabel,
  FlagMessage,
  PageContentFontWrapper,
  Spinner,
  SubTitle,
  Title,
} from '~design-system';
import { Location } from '~sonar-aligned/types/router';
import UserPasswordInput, {
  PasswordChangeHandlerParams,
} from '../../components/common/UserPasswordInput';
import { translate } from '../../helpers/l10n';
import { getReturnUrl } from '../../helpers/urls';
import Unauthorized from '../sessions/components/Unauthorized';
import { DEFAULT_ADMIN_PASSWORD } from './constants';

export interface ChangeAdminPasswordAppRendererProps {
  canAdmin?: boolean;
  location: Location;
  onSubmit: (password: string) => void;
  submitting: boolean;
  success: boolean;
}

export default function ChangeAdminPasswordAppRenderer(
  props: Readonly<ChangeAdminPasswordAppRendererProps>,
) {
  const { canAdmin, location, onSubmit, submitting, success } = props;
  const [newPassword, setNewPassword] = React.useState<PasswordChangeHandlerParams>({
    value: '',
    isValid: false,
  });
  const canSubmit = newPassword.isValid && newPassword.value !== DEFAULT_ADMIN_PASSWORD;

  if (!canAdmin) {
    return <Unauthorized />;
  }

  return (
    <CenteredLayout className="sw-h-screen">
      <Helmet defer={false} title={translate('users.change_admin_password.page')} />

      <PageContentFontWrapper className="sw-typo-default sw-flex sw-flex-col sw-items-center sw-justify-center">
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
                  onSubmit(newPassword.value);
                }}
              >
                <SubTitle className="sw-mb-4">
                  {translate('users.change_admin_password.form.header')}
                </SubTitle>

                <UserPasswordInput
                  value={newPassword.value}
                  onChange={setNewPassword}
                  size="medium"
                />

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
