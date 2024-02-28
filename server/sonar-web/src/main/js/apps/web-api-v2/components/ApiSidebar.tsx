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

import {
  Badge,
  BasicSeparator,
  Checkbox,
  HelperHintIcon,
  InputSearch,
  SubnavigationAccordion,
  SubnavigationItem,
  SubnavigationSubheading,
} from 'design-system';
import { sortBy } from 'lodash';
import { OpenAPIV3 } from 'openapi-types';
import React, { Fragment, useContext, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { InternalExtension } from '../types';
import { URL_DIVIDER, getApiEndpointKey } from '../utils';
import ApiFilterContext from './ApiFilterContext';
import RestMethodPill from './RestMethodPill';

interface Api {
  info: OpenAPIV3.OperationObject<InternalExtension>;
  method: string;
  name: string;
}

interface Props {
  apisList: Api[];
  docInfo: OpenAPIV3.InfoObject;
}

const METHOD_ORDER: Dict<number> = {
  post: 0,
  get: 1,
  patch: 2,
  delete: 3,
};

export default function ApiSidebar({ apisList, docInfo }: Readonly<Props>) {
  const [search, setSearch] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const { showInternal, setShowInternal } = useContext(ApiFilterContext);
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
      <h1 className="sw-mb-2">{docInfo.title}</h1>

      <InputSearch
        className="sw-w-full"
        onChange={setSearch}
        placeholder={translate('api_documentation.v2.search')}
        value={search}
      />

      <div className="sw-mt-4 sw-flex sw-items-center">
        <Checkbox checked={showInternal} onCheck={() => setShowInternal((prev) => !prev)}>
          <span className="sw-ml-2">{translate('api_documentation.show_internal_v2')}</span>
        </Checkbox>

        <HelpTooltip
          className="sw-ml-2"
          overlay={translate('api_documentation.internal_tooltip_v2')}
        >
          <HelperHintIcon aria-label="help-tooltip" />
        </HelpTooltip>
      </div>

      {Object.entries(groupedList).map(([group, apis]) => (
        <SubnavigationAccordion
          className="sw-mt-2"
          header={group}
          id={`web-api-${group}`}
          initExpanded={apis.some(
            ({ name, method }) => name === activeApi[0] && method === activeApi[1],
          )}
          key={group}
        >
          {sortBy(apis, (a) => [a.name, METHOD_ORDER[a.method]]).map(
            ({ method, name, info }, index, sorted) => {
              const resourceName = getResourceFromName(name);

              const previousResourceName =
                index > 0 ? getResourceFromName(sorted[index - 1].name) : undefined;

              const isNewResource = resourceName !== previousResourceName;

              return (
                <Fragment key={getApiEndpointKey(name, method)}>
                  {index > 0 && isNewResource && <BasicSeparator />}

                  {(index === 0 || isNewResource) && (
                    <SubnavigationSubheading>{resourceName}</SubnavigationSubheading>
                  )}

                  <SubnavigationItem
                    active={name === activeApi[0] && method === activeApi[1]}
                    onClick={handleApiClick}
                    value={getApiEndpointKey(name, method)}
                  >
                    <div className="sw-flex sw-gap-2 sw-w-full sw-justify-between">
                      <div className="sw-flex sw-gap-2">
                        <RestMethodPill method={method} />

                        <div>{info.summary ?? name}</div>
                      </div>

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
                </Fragment>
              );
            },
          )}
        </SubnavigationAccordion>
      ))}
    </>
  );
}

function getResourceFromName(name: string) {
  const parts = name.split('/').slice(2); // remove domain + pre-slash empty string

  if (name.endsWith('}')) {
    parts.pop(); // remove the resource id
  }

  return parts.join('/');
}
