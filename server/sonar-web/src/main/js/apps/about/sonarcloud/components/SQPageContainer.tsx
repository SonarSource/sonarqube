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
import { withRouter, WithRouterProps } from 'react-router';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import GlobalContainer from '../../../../app/components/GlobalContainer';
import { getCurrentUser, getMyOrganizations, Store } from '../../../../store/rootReducer';
import Footer from './Footer';

interface StateProps {
  currentUser: T.CurrentUser;
  userOrganizations?: T.Organization[];
}

interface OwnProps {
  children: (props: StateProps) => React.ReactNode;
}

type Props = StateProps & WithRouterProps & OwnProps;

class SQPageContainer extends React.Component<Props> {
  componentDidMount() {
    addWhitePageClass();
  }

  componentWillUnmount() {
    removeWhitePageClass();
  }

  render() {
    const { children, currentUser, userOrganizations } = this.props;
    return (
      <GlobalContainer footer={<Footer />} location={this.props.location}>
        {children({ currentUser, userOrganizations })}
      </GlobalContainer>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state),
  userOrganizations: getMyOrganizations(state)
});

export default withRouter<OwnProps>(connect(mapStateToProps)(SQPageContainer));
