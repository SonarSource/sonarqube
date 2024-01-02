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
import { Helmet } from 'react-helmet-async';
import { createWebhook, deleteWebhook, searchWebhooks, updateWebhook } from '../../../api/webhooks';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { WebhookResponse } from '../../../types/webhook';
import PageActions from './PageActions';
import PageHeader from './PageHeader';
import WebhooksList from './WebhooksList';

interface Props {
  // eslint-disable-next-line react/no-unused-prop-types
  component?: Component;
}

interface State {
  loading: boolean;
  webhooks: WebhookResponse[];
}

export class App extends React.PureComponent<Props, State> {
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

  getScopeParams = ({ component } = this.props) => {
    return {
      project: component && component.key,
    };
  };

  handleCreate = (data: { name: string; secret?: string; url: string }) => {
    const createData = {
      name: data.name,
      url: data.url,
      ...(data.secret && { secret: data.secret }),
      ...this.getScopeParams(),
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
          webhooks: webhooks.filter((item) => item.key !== webhook),
        }));
      }
    });
  };

  handleUpdate = (data: { webhook: string; name: string; secret?: string; url: string }) => {
    const updateData = {
      webhook: data.webhook,
      name: data.name,
      url: data.url,
      secret: data.secret,
    };

    return updateWebhook(updateData).then(() => {
      if (this.mounted) {
        this.setState(({ webhooks }) => ({
          webhooks: webhooks.map((webhook) =>
            webhook.key === data.webhook
              ? {
                  ...webhook,
                  name: data.name,
                  hasSecret: data.secret === undefined ? webhook.hasSecret : Boolean(data.secret),
                  url: data.url,
                }
              : webhook
          ),
        }));
      }
    });
  };

  render() {
    const { loading, webhooks } = this.state;

    return (
      <>
        <Suggestions suggestions="webhooks" />
        <Helmet defer={false} title={translate('webhooks.page')} />

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

export default withComponentContext(App);
