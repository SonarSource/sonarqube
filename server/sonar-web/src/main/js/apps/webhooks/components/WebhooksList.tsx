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
import { sortBy } from 'lodash';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import WebhookItem from './WebhookItem';

interface Props {
  onDelete: (webhook: string) => Promise<void>;
  onUpdate: (data: { webhook: string; name: string; url: string }) => Promise<void>;
  webhooks: T.Webhook[];
}

export default class WebhooksList extends React.PureComponent<Props> {
  renderHeader = () => (
    <thead>
      <tr>
        <th>{translate('name')}</th>
        <th>{translate('webhooks.url')}</th>
        <th>{translate('webhooks.secret_header')}</th>
        <th>{translate('webhooks.last_execution')}</th>
        <th />
      </tr>
    </thead>
  );

  render() {
    const { webhooks } = this.props;
    if (webhooks.length < 1) {
      return <p>{translate('webhooks.no_result')}</p>;
    }
    return (
      <table className="data zebra">
        {this.renderHeader()}
        <tbody>
          {sortBy(webhooks, webhook => webhook.name.toLowerCase()).map(webhook => (
            <WebhookItem
              key={webhook.key}
              onDelete={this.props.onDelete}
              onUpdate={this.props.onUpdate}
              webhook={webhook}
            />
          ))}
        </tbody>
      </table>
    );
  }
}
