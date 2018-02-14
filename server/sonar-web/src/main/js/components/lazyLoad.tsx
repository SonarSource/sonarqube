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
import * as React from 'react';

type ReactComponent<P> = React.ComponentClass<P> | React.StatelessComponent<P>;

interface Loader<P> {
  (): Promise<{ default: ReactComponent<P> }>;
}

export function lazyLoad<P>(loader: Loader<P>) {
  interface State {
    Component?: ReactComponent<P>;
  }

  // use `React.Component`, not `React.PureComponent` to always re-render
  // and let the child component decide if it needs to change
  return class LazyLoader extends React.Component<any, State> {
    mounted = false;
    state: State = {};

    componentDidMount() {
      this.mounted = true;
      loader().then(i => this.receiveComponent(i.default), () => {});
    }

    componentWillUnmount() {
      this.mounted = false;
    }

    receiveComponent = (Component: ReactComponent<P>) => {
      if (this.mounted) {
        this.setState({ Component });
      }
    };

    render() {
      const { Component } = this.state;

      if (!Component) {
        return null;
      }

      return <Component {...this.props} />;
    }
  };
}
