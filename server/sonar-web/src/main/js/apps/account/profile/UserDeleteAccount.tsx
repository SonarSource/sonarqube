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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getOrganizationsThatPreventDeletion } from '../../../api/organizations';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import UserDeleteAccountContent from './UserDeleteAccountContent';
import UserDeleteAccountModal from './UserDeleteAccountModal';

interface Props {
  user: T.LoggedInUser;
  userOrganizations: T.Organization[];
}

interface State {
  loading: boolean;
  organizationsToTransferOrDelete: T.Organization[];
  showModal: boolean;
}

export class UserDeleteAccount extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: true,
    organizationsToTransferOrDelete: [],
    showModal: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchOrganizationsThatPreventDeletion();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchOrganizationsThatPreventDeletion = () => {
    getOrganizationsThatPreventDeletion().then(
      ({ organizations }) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            organizationsToTransferOrDelete: organizations
          });
        }
      },
      () => {}
    );
  };

  toggleModal = () => {
    if (this.mounted) {
      this.setState(state => ({
        showModal: !state.showModal
      }));
    }
  };

  render() {
    const { user, userOrganizations } = this.props;
    const { organizationsToTransferOrDelete, loading, showModal } = this.state;

    const label = translate('my_profile.delete_account');

    return (
      <div>
        <h2 className="spacer-bottom">{label}</h2>

        <DeferredSpinner loading={loading} />

        {!loading && (
          <>
            <UserDeleteAccountContent
              className="list-styled no-padding big-spacer-top big-spacer-bottom"
              organizationsSafeToDelete={userOrganizations}
              organizationsToTransferOrDelete={organizationsToTransferOrDelete}
            />

            <Button
              className="button-red"
              disabled={organizationsToTransferOrDelete.length > 0}
              onClick={this.toggleModal}
              type="button">
              {translate('delete')}
            </Button>

            {showModal && (
              <UserDeleteAccountModal
                label={label}
                organizationsSafeToDelete={userOrganizations}
                organizationsToTransferOrDelete={organizationsToTransferOrDelete}
                toggleModal={this.toggleModal}
                user={user}
              />
            )}
          </>
        )}
      </div>
    );
  }
}

export default whenLoggedIn(withUserOrganizations(UserDeleteAccount));
