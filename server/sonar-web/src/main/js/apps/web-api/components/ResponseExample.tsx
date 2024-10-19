/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { fetchResponseExample as fetchResponseExampleApi } from '../../../api/web-api';
import { WebApi } from '../../../types/types';

interface Props {
  action: WebApi.Action;
  domain: WebApi.Domain;
}

interface State {
  responseExample?: WebApi.Example;
}

export default class ResponseExample extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchResponseExample();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.action !== this.props.action) {
      this.fetchResponseExample();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchResponseExample() {
    const { domain, action } = this.props;
    fetchResponseExampleApi(domain.path, action.key).then(
      (responseExample) => this.setState({ responseExample }),
      () => {},
    );
  }

  render() {
    const { responseExample } = this.state;

    return (
      <div className="sw-mt-6">
        {responseExample && (
          <pre className="sw-code sw-whitespace-pre-wrap">{responseExample.example}</pre>
        )}
      </div>
    );
  }
}
