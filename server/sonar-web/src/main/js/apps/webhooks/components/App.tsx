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
import { Helmet } from 'react-helmet';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { createWebhook, deleteWebhook, searchWebhooks, updateWebhook } from '../../../api/webhooks';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import PageActions from './PageActions';
import PageHeader from './PageHeader';
import WebhooksList from './WebhooksList';

interface Props {
  component?: T.LightComponent;
  organization: T.Organization | undefined;
}

interface State {
  loading: boolean;
  webhooks: T.Webhook[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, webhooks: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchWebhooks();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchWebhooks = () => {
    return searchWebhooks(this.getScopeParams()).then(
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

  getScopeParams = ({ organization, component } = this.props) => {
    const organizationKey = organization && organization.key;
    return {
      organization: component ? component.organization : organizationKey,
      project: component && component.key
    };
  };

  handleCreate = (data: { name: string; secret?: string; url: string }) => {
    const createData = {
      name: data.name,
      url: data.url,
      ...(data.secret && { secret: data.secret }),
      ...this.getScopeParams()
    };

    return createWebhook(createData).then(({ webhook }) => {
      if (this.mounted) {
        this.setState(({ webhooks }) => ({ webhooks: [...webhooks, webhook] }));
      }
    });
  };

  handleDelete = (webhook: string) => {
    return deleteWebhook({ webhook }).then(() => {
      if (this.mounted) {
        this.setState(({ webhooks }) => ({
          webhooks: webhooks.filter(item => item.key !== webhook)
        }));
      }
    });
  };

  handleUpdate = (data: { webhook: string; name: string; secret?: string; url: string }) => {
    const udpateData = {
      webhook: data.webhook,
      name: data.name,
      url: data.url,
      ...(data.secret && { secret: data.secret })
    };

    return updateWebhook(udpateData).then(() => {
      if (this.mounted) {
        this.setState(({ webhooks }) => ({
          webhooks: webhooks.map(webhook =>
            webhook.key === data.webhook
              ? { ...webhook, name: data.name, secret: data.secret, url: data.url }
              : webhook
          )
        }));
      }
    });
  };

  render() {
    const { loading, webhooks } = this.state;

    return (
      <>
        <Suggestions suggestions="webhooks" />
        <Helmet title={translate('webhooks.page')} />

        <div className="page page-limited">
          <PageHeader loading={loading}>
            <PageActions
              loading={loading}
              onCreate={this.handleCreate}
              webhooksCount={webhooks.length}
            />
          </PageHeader>

          {!loading && (
            <div className="boxed-group boxed-group-inner">
              <WebhooksList
                onDelete={this.handleDelete}
                onUpdate={this.handleUpdate}
                webhooks={webhooks}
              />
            </div>
          )}
        </div>
      </>
    );
  }
}
