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
import { Helmet } from 'react-helmet-async';
import { SubmitButton } from '../../components/controls/buttons';
import { Location } from '../../components/hoc/withRouter';
import { Alert } from '../../components/ui/Alert';
import MandatoryFieldMarker from '../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../helpers/l10n';
import { getReturnUrl } from '../../helpers/urls';
import Unauthorized from '../sessions/components/Unauthorized';
import { DEFAULT_ADMIN_PASSWORD } from './constants';

export interface ChangeAdminPasswordAppRendererProps {
  passwordValue: string;
  confirmPasswordValue: string;
  onConfirmPasswordChange: (password: string) => void;
  onPasswordChange: (password: string) => void;
  onSubmit: () => void;
  canAdmin?: boolean;
  canSubmit?: boolean;
  submitting: boolean;
  success: boolean;
  location: Location;
}

export default function ChangeAdminPasswordAppRenderer(props: ChangeAdminPasswordAppRendererProps) {
  const {
    canAdmin,
    canSubmit,
    confirmPasswordValue,
    passwordValue,
    location,
    submitting,
    success,
  } = props;

  if (!canAdmin) {
    return <Unauthorized />;
  }

  return (
    <div className="page-wrapper-simple">
      <Helmet defer={false} title={translate('users.change_admin_password.page')} />
      <div className="page-simple">
        {success ? (
          <Alert variant="success">
            <p className="spacer-bottom">{translate('users.change_admin_password.form.success')}</p>
            {/* We must not use Link here, because we need a refresh of the /api/navigation/global call. */}
            <a href={getReturnUrl(location)}>
              {translate('users.change_admin_password.form.continue_to_app')}
            </a>
          </Alert>
        ) : (
          <>
            <h1 className="text-center bg-danger big padded">
              {translate('users.change_admin_password.instance_is_at_risk')}
            </h1>
            <p className="text-center huge huge-spacer-top">
              {translate('users.change_admin_password.header')}
            </p>
            <p className="text-center huge-spacer-top huge-spacer-bottom">
              {translate('users.change_admin_password.description')}
            </p>

            <form
              className="text-center"
              onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
                e.preventDefault();
                props.onSubmit();
              }}
            >
              <h2 className="big-spacer-bottom big">
                {translate('users.change_admin_password.form.header')}
              </h2>

              <MandatoryFieldsExplanation className="form-field" />

              <div className="form-field">
                <label htmlFor="user-password">
                  {translate('users.change_admin_password.form.password')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  id="user-password"
                  name="password"
                  onChange={(e: React.SyntheticEvent<HTMLInputElement>) => {
                    props.onPasswordChange(e.currentTarget.value);
                  }}
                  required
                  type="password"
                  value={passwordValue}
                />
              </div>

              <div className="form-field">
                <label htmlFor="confirm-user-password">
                  {translate('users.change_admin_password.form.confirm')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  id="confirm-user-password"
                  name="confirm-password"
                  onChange={(e: React.SyntheticEvent<HTMLInputElement>) => {
                    props.onConfirmPasswordChange(e.currentTarget.value);
                  }}
                  required
                  type="password"
                  value={confirmPasswordValue}
                />

                {confirmPasswordValue === passwordValue &&
                  passwordValue === DEFAULT_ADMIN_PASSWORD && (
                    <Alert className="spacer-top" variant="warning">
                      {translate('users.change_admin_password.form.cannot_use_default_password')}
                    </Alert>
                  )}
              </div>

              <div className="form-field">
                <SubmitButton disabled={!canSubmit || submitting}>
                  {translate('update_verb')}
                  {submitting && <i className="spinner spacer-left" />}
                </SubmitButton>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
