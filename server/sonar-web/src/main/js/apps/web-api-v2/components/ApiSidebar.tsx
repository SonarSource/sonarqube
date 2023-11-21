/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import {
  Badge,
  Checkbox,
  HelperHintIcon,
  InputSearch,
  Link,
  SubnavigationAccordion,
  SubnavigationItem,
} from 'design-system';
import { OpenAPIV3 } from 'openapi-types';
import React, { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { URL_DIVIDER, getApiEndpointKey, getMethodClassName } from '../utils';

interface Api {
  name: string;
  method: string;
  info: OpenAPIV3.OperationObject<{ 'x-internal'?: 'true' }>;
}
interface Props {
  docInfo: OpenAPIV3.InfoObject;
  apisList: Api[];
}

export default function ApiSidebar({ apisList, docInfo }: Readonly<Props>) {
  const [search, setSearch] = useState('');
  const [showInternal, setShowInternal] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const activeApi = location.hash.replace('#', '').split(URL_DIVIDER);

  const handleApiClick = (value: string) => {
    navigate(`.#${value}`, { replace: true });
  };

  const lowerCaseSearch = search.toLowerCase();

  const groupedList = useMemo(
    () =>
      apisList
        .filter(
          (api) =>
            api.name.toLowerCase().includes(lowerCaseSearch) ||
            api.method.toLowerCase().includes(lowerCaseSearch) ||
            api.info.summary?.toLowerCase().includes(lowerCaseSearch),
        )
        .filter((api) => showInternal || !api.info['x-internal'])
        .reduce<Record<string, Api[]>>((acc, api) => {
          const subgroup = api.name.split('/')[1];
          return {
            ...acc,
            [subgroup]: [...(acc[subgroup] ?? []), api],
          };
        }, {}),
    [lowerCaseSearch, apisList, showInternal],
  );

  return (
    <>
      <h1 className="sw-mb-2">
        <Link to="." className="sw-text-[unset] sw-border-none">
          {docInfo.title}
          <Badge className="sw-align-middle sw-ml-2">{docInfo.version}</Badge>
        </Link>
      </h1>

      <InputSearch
        className="sw-w-full"
        placeholder={translate('api_documentation.v2.search')}
        onChange={setSearch}
        value={search}
      />

      <div className="sw-mt-4">
        <Checkbox checked={showInternal} onCheck={() => setShowInternal((prev) => !prev)}>
          <span className="sw-ml-2 sw-mb-1">{translate('api_documentation.show_internal')}</span>
        </Checkbox>
        <HelpTooltip className="sw-ml-2" overlay={translate('api_documentation.internal_tooltip')}>
          <HelperHintIcon aria-label="help-tooltip" />
        </HelpTooltip>
      </div>

      {Object.entries(groupedList).map(([group, apis]) => (
        <SubnavigationAccordion
          initExpanded={apis.some(
            ({ name, method }) => name === activeApi[0] && method === activeApi[1],
          )}
          className="sw-mt-2"
          header={group}
          key={group}
          id={`web-api-${group}`}
        >
          {apis.map(({ method, name, info }) => (
            <SubnavigationItem
              active={name === activeApi[0] && method === activeApi[1]}
              key={getApiEndpointKey(name, method)}
              onClick={handleApiClick}
              value={getApiEndpointKey(name, method)}
            >
              <div className="sw-flex sw-gap-2">
                <Badge className={classNames('sw-self-center', getMethodClassName(method))}>
                  {method.toUpperCase()}
                </Badge>
                <div>{info.summary ?? name}</div>

                {(info['x-internal'] || info.deprecated) && (
                  <div className="sw-flex sw-flex-col sw-justify-center sw-gap-2">
                    {info['x-internal'] && (
                      <Badge variant="new" className="sw-self-center">
                        {translate('internal')}
                      </Badge>
                    )}
                    {info.deprecated && (
                      <Badge variant="deleted" className="sw-self-center">
                        {translate('deprecated')}
                      </Badge>
                    )}
                  </div>
                )}
              </div>
            </SubnavigationItem>
          ))}
        </SubnavigationAccordion>
      ))}
    </>
  );
}
