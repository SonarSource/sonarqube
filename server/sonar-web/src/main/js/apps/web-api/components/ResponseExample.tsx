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
import { fetchResponseExample as fetchResponseExampleApi } from '../../../api/web-api';

interface Props {
  action: T.WebApi.Action;
  domain: T.WebApi.Domain;
}

interface State {
  responseExample?: T.WebApi.Example;
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
      responseExample => this.setState({ responseExample }),
      () => {}
    );
  }

  render() {
    const { responseExample } = this.state;

    return (
      <div className="web-api-response">
        {responseExample && <pre style={{ whiteSpace: 'pre-wrap' }}>{responseExample.example}</pre>}
      </div>
    );
  }
}
