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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import { areThereCustomOrganizations } from '../../store/rootReducer';

const forSingleOrganization = (ComposedComponent /*: Object */) => {
  class X extends React.PureComponent {
    static displayName = `forSingleOrganization(${ComposedComponent.displayName})}`;

    render() {
      const { customOrganizations, router, ...other } = this.props;

      if (customOrganizations) {
        router.replace('/not_found');
        return null;
      }

      return <ComposedComponent {...other} />;
    }
  }

  const mapStateToProps = state => ({
    customOrganizations: areThereCustomOrganizations(state)
  });

  return connect(mapStateToProps)(withRouter(X));
};

export default forSingleOrganization;
