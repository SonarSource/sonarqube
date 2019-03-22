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
import { Alert } from './ui/Alert';
import { translate } from '../helpers/l10n';
import { get, save } from '../helpers/storage';

interface Loader<P> {
  (): Promise<{ default: React.ComponentType<P> }>;
}

export const LAST_FAILED_CHUNK_STORAGE_KEY = 'sonarqube.last_failed_chunk';

export function lazyLoad<P>(loader: Loader<P>, displayName?: string) {
  interface ImportError {
    request?: string;
  }

  interface State {
    Component?: React.ComponentType<P>;
    error?: ImportError;
  }

  // use `React.Component`, not `React.PureComponent` to always re-render
  // and let the child component decide if it needs to change
  // also, use any instead of P because typescript doesn't cope correctly with default props
  return class LazyLoader extends React.Component<any, State> {
    mounted = false;
    static displayName = displayName;
    state: State = {};

    componentDidMount() {
      this.mounted = true;
      loader().then(i => this.receiveComponent(i.default), this.failToReceiveComponent);
    }

    componentWillUnmount() {
      this.mounted = false;
    }

    receiveComponent = (Component: React.ComponentType<P>) => {
      if (this.mounted) {
        this.setState({ Component, error: undefined });
      }
    };

    failToReceiveComponent = (error?: ImportError) => {
      const lastFailedChunk = get(LAST_FAILED_CHUNK_STORAGE_KEY);
      if (error && error.request === lastFailedChunk) {
        // BOOM!
        // this is the second time we try to load the same file
        // usually that means the file does not exist on the server
        // so we should not try to reload the page to not fall into infinite reloading
        // just show the error message
        if (this.mounted) {
          this.setState({ Component: undefined, error });
        }
      } else {
        if (error && error.request) {
          save(LAST_FAILED_CHUNK_STORAGE_KEY, error.request);
        }
        window.location.reload();
      }
    };

    render() {
      const { Component, error } = this.state;

      if (error && error.request) {
        return <Alert variant="error">{translate('default_error_message')}</Alert>;
      }

      if (!Component) {
        return null;
      }

      return <Component {...this.props as any} />;
    }
  };
}
