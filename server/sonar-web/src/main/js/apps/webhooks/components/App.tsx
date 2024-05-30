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

import { LargeCenteredLayout, PageContentFontWrapper, Spinner } from 'design-system';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { createWebhook, deleteWebhook, searchWebhooks, updateWebhook } from '../../../api/webhooks';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { WebhookResponse } from '../../../types/webhook';
import PageActions from './PageActions';
import PageHeader from './PageHeader';
import WebhooksList from './WebhooksList';

export interface AppProps {
  component?: Component;
}

export function App({ component }: AppProps) {
  const [loading, setLoading] = useState(true);
  const [webhooks, setWebhooks] = useState<WebhookResponse[]>([]);

  const getScopeParams = useCallback(
    () => ({
      project: component?.key,
    }),
    [component?.key],
  );

  const fetchWebhooks = useCallback(async () => {
    try {
      const { webhooks } = await searchWebhooks(getScopeParams());
      setWebhooks(webhooks);
    } finally {
      setLoading(false);
    }
  }, [getScopeParams]);

  useEffect(() => {
    fetchWebhooks();
  }, [fetchWebhooks]);

  async function handleCreate(data: { name: string; secret?: string; url: string }) {
    const createData = {
      name: data.name,
      url: data.url,
      ...(data.secret && { secret: data.secret }),
      ...getScopeParams(),
    };

    const { webhook } = await createWebhook(createData);
    setWebhooks([...webhooks, webhook]);
  }

  async function handleDelete(webhook: string) {
    await deleteWebhook({ webhook });
    setWebhooks((webhooks) => webhooks.filter((item) => item.key !== webhook));
  }

  async function handleUpdate(data: {
    webhook: string;
    name: string;
    secret?: string;
    url: string;
  }) {
    await updateWebhook({ ...data });
    setWebhooks((webhooks) =>
      webhooks.map((webhook) =>
        webhook.key === data.webhook
          ? {
              ...webhook,
              name: data.name,
              hasSecret: data.secret === undefined ? webhook.hasSecret : Boolean(data.secret),
              url: data.url,
            }
          : webhook,
      ),
    );
  }

  return (
    <LargeCenteredLayout id="project-webhooks">
      <PageContentFontWrapper className="sw-my-8 sw-body-sm">
        <Suggestions suggestion={DocLink.Webhooks} />
        <Helmet defer={false} title={translate('webhooks.page')} />
        <PageHeader>
          <PageActions loading={loading} onCreate={handleCreate} webhooksCount={webhooks.length} />
        </PageHeader>

        <Spinner loading={loading}>
          <WebhooksList onDelete={handleDelete} onUpdate={handleUpdate} webhooks={webhooks} />
        </Spinner>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withComponentContext(App);
