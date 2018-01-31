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
import { Helmet } from 'react-helmet';
import PageHeader from './PageHeader';
import WebhooksList from './WebhooksList';
import { searchWebhooks, Webhook } from '../../../api/webhooks';
import { LightComponent, Organization } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  organization: Organization | undefined;
  component?: LightComponent;
}

interface State {
  loading: boolean;
  webhooks: Webhook[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted: boolean = false;
  state: State = { loading: true, webhooks: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchWebhooks();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchWebhooks = ({ organization, component } = this.props) => {
    this.setState({ loading: true });
    searchWebhooks({
      organization: organization && organization.key,
      project: component && component.key
    }).then(
      ({ webhooks }) => {
        if (this.mounted) {
          this.setState({ loading: false, webhooks });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { loading, webhooks } = this.state;
    return (
      <div className="page page-limited">
        <Helmet title={translate('webhooks.page')} />
        <PageHeader loading={loading} />
        {!loading && (
          <div className="boxed-group boxed-group-inner">
            <WebhooksList webhooks={webhooks} />
          </div>
        )}
      </div>
    );
  }
}
