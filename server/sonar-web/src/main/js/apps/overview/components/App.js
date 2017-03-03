/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import shallowCompare from 'react-addons-shallow-compare';
import { withRouter } from 'react-router';
import OverviewApp from './OverviewApp';
import EmptyOverview from './EmptyOverview';

type Props = {
  component: {
    id: string,
    key: string,
    qualifier: string
  },
  router: Object
};

class App extends React.Component {
  props: Props;
  state: Object;

  componentDidMount () {
    if (['VW', 'SVW'].includes(this.props.component.qualifier)) {
      this.props.router.replace({
        pathname: '/view',
        query: { id: this.props.component.key }
      });
    }
  }

  shouldComponentUpdate (nextProps: Props) {
    return shallowCompare(this, nextProps);
  }

  render () {
    const { component } = this.props;

    if (['FIL', 'UTS'].includes(component.qualifier)) {
      const SourceViewer = require('../../../components/source-viewer/SourceViewer').default;
      return (
          <div className="page">
            <SourceViewer component={component}/>
          </div>
      );
    }

    if (!component.snapshotDate) {
      return <EmptyOverview {...this.props}/>;
    }

    return (
        <OverviewApp
            {...this.props}
            leakPeriodIndex="1"/>
    );
  }
}

export default withRouter(App);

export const UnconnectedApp = App;
