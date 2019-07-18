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
import { ListButton } from 'sonar-ui-common/components/controls/buttons';
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { getOrganizationUrl } from '../../../helpers/urls';

interface Props {
  onClick: VoidFunction;
  organization: T.Organization;
  router: Router;
}

export class OrganizationsShortListItem extends React.PureComponent<Props> {
  handleClick = () => {
    const { onClick, organization, router } = this.props;
    onClick();
    router.push(getOrganizationUrl(organization.key));
  };

  render() {
    const { organization } = this.props;
    return (
      <ListButton className="abs-width-300" onClick={this.handleClick}>
        <div className="display-flex-center">
          <OrganizationAvatar className="spacer-right" organization={organization} />
          <span>{organization.name}</span>
        </div>
      </ListButton>
    );
  }
}

export default withRouter(OrganizationsShortListItem);
