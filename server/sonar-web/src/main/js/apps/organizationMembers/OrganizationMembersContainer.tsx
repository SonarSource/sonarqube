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
import { connect } from 'react-redux';
import { withCurrentUser } from '../../components/hoc/withCurrentUser';
import { getOrganizationByKey, Store } from '../../store/rootReducer';
import OrganizationMembers from './OrganizationMembers';

interface OwnProps {
  params: { organizationKey: string };
}

interface StateProps {
  organization: T.Organization;
}

const mapStateToProps = (state: Store, ownProps: OwnProps): StateProps => {
  return { organization: getOrganizationByKey(state, ownProps.params.organizationKey) };
};

export default withCurrentUser(connect(mapStateToProps)(OrganizationMembers));
