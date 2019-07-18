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
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import RadioCard from 'sonar-ui-common/components/controls/RadioCard';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { setOrganizationMemberSync, syncMembers } from '../../api/organizations';
import { sanitizeAlmId } from '../../helpers/almIntegrations';
import { fetchOrganization } from '../../store/rootActions';

interface Props {
  buttonText: string;
  fetchOrganization: (key: string) => void;
  hasOtherMembers?: boolean;
  organization: T.Organization;
  refreshMembers: () => Promise<void>;
}

interface State {
  membersSync: boolean;
}

export class SyncMemberForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      membersSync: Boolean(props.organization.alm && props.organization.alm.membersSync)
    };
  }

  handleConfirm = () => {
    const { organization } = this.props;
    const { membersSync } = this.state;

    return setOrganizationMemberSync({
      organization: organization.key,
      enabled: membersSync
    }).then(() => {
      this.props.fetchOrganization(organization.key);
      if (membersSync) {
        return this.handleMemberSync();
      }
      return Promise.resolve();
    });
  };

  handleManualClick = () => {
    this.setState({ membersSync: false });
  };

  handleAutoClick = () => {
    this.setState({ membersSync: true });
  };

  handleMemberSync = () => {
    return syncMembers(this.props.organization.key).then(this.props.refreshMembers);
  };

  renderModalDescription = () => {
    return (
      <p className="spacer-top">
        {translate('organization.members.management.description')}
        <Link
          className="spacer-left"
          target="_blank"
          to={{ pathname: '/documentation/organizations/manage-team/' }}>
          {translate('learn_more')}
        </Link>
      </p>
    );
  };

  renderModalBody = () => {
    const { membersSync } = this.state;
    const { hasOtherMembers, organization } = this.props;
    const almKey = organization.alm && sanitizeAlmId(organization.alm.key);
    const showWarning = hasOtherMembers && organization.alm && !organization.alm.membersSync;
    return (
      <div className="display-flex-stretch big-spacer-top">
        <RadioCard
          onClick={this.handleManualClick}
          selected={!membersSync}
          title={translate('organization.members.management.manual')}>
          <div className="spacer-left">
            <ul className="big-spacer-left note">
              <li className="spacer-bottom">
                {translate('organization.members.management.manual.add_members_manually')}
              </li>
              <li>{translate('organization.members.management.choose_members_permissions')}</li>
            </ul>
          </div>
        </RadioCard>
        <RadioCard
          onClick={this.handleAutoClick}
          selected={membersSync}
          title={translateWithParameters(
            'organization.members.management.automatic',
            translate(almKey || '')
          )}>
          <div className="spacer-left">
            <ul className="big-spacer-left note">
              {almKey && (
                <>
                  <li className="spacer-bottom">
                    {translateWithParameters(
                      'organization.members.management.automatic.synchronized_from_x',
                      translate('organization', almKey)
                    )}
                  </li>
                  <li className="spacer-bottom">
                    {translate(
                      'organization.members.management.automatic.members_changes_reflected',
                      almKey
                    )}
                  </li>
                </>
              )}
              <li>{translate('organization.members.management.choose_members_permissions')}</li>
            </ul>
          </div>
          {almKey && showWarning && (
            <Alert className="big-spacer-top" variant="warning">
              {translateWithParameters(
                'organization.members.management.automatic.warning_x',
                translate('organization', almKey)
              )}
            </Alert>
          )}
        </RadioCard>
      </div>
    );
  };

  render() {
    const { organization } = this.props;
    const orgMemberSync = Boolean(organization.alm && organization.alm.membersSync);
    return (
      <ConfirmButton
        cancelButtonText={translate('close')}
        confirmButtonText={translate('save')}
        confirmDisable={this.state.membersSync === orgMemberSync}
        modalBody={this.renderModalBody()}
        modalHeader={translate('organization.members.management.title')}
        modalHeaderDescription={this.renderModalDescription()}
        onConfirm={this.handleConfirm}
        size="medium">
        {({ onClick }) => <Button onClick={onClick}>{this.props.buttonText}</Button>}
      </ConfirmButton>
    );
  }
}

const mapDispatchToProps = { fetchOrganization };

export default connect(
  null,
  mapDispatchToProps
)(SyncMemberForm);
