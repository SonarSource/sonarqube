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
import OverviewApp from './OverviewApp';
import EmptyOverview from './EmptyOverview';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';

type Props = {
  component: {
    analysisDate?: string,
    id: string,
    key: string,
    qualifier: string,
    tags: Array<string>
  },
  router: Object
};

export default class App extends React.PureComponent {
  props: Props;
  state: Object;

  static contextTypes = {
    router: React.PropTypes.object
  };

  componentDidMount() {
    if (this.isPortfolio()) {
      this.context.router.replace({
        pathname: '/portfolio',
        query: { id: this.props.component.key }
      });
    }
  }

  isPortfolio() {
    return this.props.component.qualifier === 'VW' || this.props.component.qualifier === 'SVW';
  }

  render() {
    if (this.isPortfolio()) {
      return null;
    }

    const { component } = this.props;

    if (['FIL', 'UTS'].includes(component.qualifier)) {
      return (
        <div className="page">
          <SourceViewer component={component.key} />
        </div>
      );
    }

    if (!component.analysisDate) {
      return <EmptyOverview component={component} />;
    }

    return <OverviewApp component={component} />;
  }
}
