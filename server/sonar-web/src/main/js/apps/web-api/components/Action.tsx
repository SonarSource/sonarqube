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
import { Badge, Card, LinkBox, LinkIcon, SubHeading, Tabs } from 'design-system';
import * as React from 'react';
import { queryToSearch } from '~sonar-aligned/helpers/urls';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { WebApi } from '../../../types/types';
import { getActionKey, serializeQuery } from '../utils';
import ActionChangelog from './ActionChangelog';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import Params from './Params';
import ResponseExample from './ResponseExample';

interface Props {
  action: WebApi.Action;
  domain: WebApi.Domain;
  showDeprecated: boolean;
  showInternal: boolean;
}

enum TabOption {
  PARAMS = 'parameters',
  RESPONSE = 'response_example',
  CHANGELOG = 'changelog',
}

export default function Action(props: Props) {
  const { action, domain, showDeprecated, showInternal } = props;
  const verb = action.post ? 'POST' : 'GET';
  const actionKey = getActionKey(domain.path, action.key);

  const [tab, setTab] = React.useState<TabOption | undefined>(undefined);

  const tabOptions = React.useMemo(() => {
    const opts = [];

    if (action.params) {
      opts.push(TabOption.PARAMS);
    }
    if (action.hasResponseExample) {
      opts.push(TabOption.RESPONSE);
    }
    if (action.changelog.length > 0) {
      opts.push(TabOption.CHANGELOG);
    }

    return opts.map((option) => ({ label: translate('api_documentation', option), value: option }));
  }, [action]);

  return (
    <Card id={actionKey}>
      <header className="sw-flex sw-items-baseline sw-gap-2">
        <LinkBox
          to={{
            pathname: '/web_api/' + actionKey,
            search: queryToSearch(
              serializeQuery({
                deprecated: Boolean(action.deprecatedSince),
                internal: Boolean(action.internal),
              }),
            ),
          }}
        >
          <LinkIcon />
        </LinkBox>

        <SubHeading className="sw-m-0">
          {verb} {actionKey}
        </SubHeading>

        {action.internal && <InternalBadge />}

        {action.since && (
          <Badge variant="new">{translateWithParameters('since_x', action.since)}</Badge>
        )}

        {action.deprecatedSince && <DeprecatedBadge since={action.deprecatedSince} />}
      </header>

      <div
        className="sw-mt-4 markdown"
        // Safe: comes from the backend
        dangerouslySetInnerHTML={{ __html: action.description }}
      />

      <div className="sw-mt-4">
        <Tabs options={tabOptions} onChange={(opt) => setTab(opt)} value={tab} />

        {tab === TabOption.PARAMS && action.params && (
          <Params
            params={action.params}
            showDeprecated={showDeprecated}
            showInternal={showInternal}
          />
        )}

        {tab === TabOption.RESPONSE && action.hasResponseExample && (
          <ResponseExample action={action} domain={domain} />
        )}

        {tab === TabOption.CHANGELOG && <ActionChangelog changelog={action.changelog} />}
      </div>
    </Card>
  );
}
