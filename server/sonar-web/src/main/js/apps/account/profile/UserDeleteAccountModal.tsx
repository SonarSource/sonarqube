/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { connect } from 'react-redux';
import InputValidationField from 'sonar-ui-common/components/controls/InputValidationField';
import ValidationModal from 'sonar-ui-common/components/controls/ValidationModal';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { deactivateUser } from '../../../api/users';
import RecentHistory from '../../../app/components/RecentHistory';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { doLogout } from '../../../store/rootActions';
import UserDeleteAccountContent from './UserDeleteAccountContent';

interface Values {
  login: string;
}

interface DeleteModalProps {
  doLogout: () => Promise<void>;
  label: string;
  organizationsSafeToDelete: T.Organization[];
  organizationsToTransferOrDelete: T.Organization[];
  router: Pick<Router, 'push'>;
  toggleModal: VoidFunction;
  user: T.LoggedInUser;
}

export class UserDeleteAccountModal extends React.PureComponent<DeleteModalProps> {
  handleSubmit = () => {
    const { user } = this.props;

    return deactivateUser({ login: user.login })
      .then(this.props.doLogout)
      .then(() => {
        RecentHistory.clear();
        window.location.replace('/account-deleted');
      });
  };

  handleValidate = ({ login }: Values) => {
    const { user } = this.props;
    const errors: { login?: string } = {};
    const trimmedLogin = login.trim();

    if (!trimmedLogin) {
      errors.login = translate('my_profile.delete_account.login.required');
    } else if (user.externalIdentity && trimmedLogin !== user.externalIdentity.trim()) {
      errors.login = translate('my_profile.delete_account.login.wrong_value');
    }

    return errors;
  };

  render() {
    const {
      label,
      organizationsSafeToDelete,
      organizationsToTransferOrDelete,
      toggleModal,
      user
    } = this.props;

    return (
      <ValidationModal
        confirmButtonText={translate('delete')}
        header={translateWithParameters(
          'my_profile.delete_account.modal.header',
          label,
          user.externalIdentity || ''
        )}
        initialValues={{
          login: ''
        }}
        isDestructive={true}
        onClose={toggleModal}
        onSubmit={this.handleSubmit}
        validate={this.handleValidate}>
        {({ dirty, errors, handleBlur, handleChange, isSubmitting, touched, values }) => (
          <>
            <Alert className="big-spacer-bottom" variant="error">
              {translate('my_profile.warning_message')}
            </Alert>

            <UserDeleteAccountContent
              className="list-styled no-padding big-spacer-bottom"
              organizationsSafeToDelete={organizationsSafeToDelete}
              organizationsToTransferOrDelete={organizationsToTransferOrDelete}
            />

            <InputValidationField
              autoFocus={true}
              dirty={dirty}
              disabled={isSubmitting}
              error={errors.login}
              id="user-login"
              label={
                <label htmlFor="user-login">
                  {translate('my_profile.delete_account.verify')}
                  <em className="mandatory">*</em>
                </label>
              }
              name="login"
              onBlur={handleBlur}
              onChange={handleChange}
              touched={touched.login}
              type="text"
              value={values.login}
            />
          </>
        )}
      </ValidationModal>
    );
  }
}

const mapStateToProps = () => ({});

const mapDispatchToProps = { doLogout: doLogout as any };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(UserDeleteAccountModal));
