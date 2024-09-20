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
import styled from '@emotion/styled';
import { Spinner } from '@sonarsource/echoes-react';
import { LargeCenteredLayout, PageContentFontWrapper, Title } from 'design-system';
import { omit } from 'lodash';
import React, { useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useLocation } from 'react-router-dom';
import { translate } from '../../helpers/l10n';
import { useOpenAPI } from '../../queries/web-api';
import ApiFilterContext from './components/ApiFilterContext';
import ApiInformation from './components/ApiInformation';
import ApiSidebar from './components/ApiSidebar';
import { URL_DIVIDER, dereferenceSchema } from './utils';

export default function WebApiApp() {
  const [showInternal, setShowInternal] = useState(false);
  const { data, isLoading } = useOpenAPI();
  const location = useLocation();
  const activeApi = location.hash.replace('#', '').split(URL_DIVIDER);

  const apis = useMemo(() => {
    if (!data) {
      return [];
    }
    return Object.entries(dereferenceSchema(data).paths ?? {}).reduce(
      (acc, [name, methods]) => [
        ...acc,
        ...Object.entries(
          omit(methods, 'summary', '$ref', 'description', 'servers', 'parameters') ?? {},
        ).map(([method, info]) => ({ name, method, info })),
      ],
      [],
    );
  }, [data]);

  const activeData =
    activeApi.length > 1 &&
    apis.find((api) => api.name === activeApi[0] && api.method === activeApi[1]);

  const contextValue = useMemo(
    () => ({
      showInternal,
      setShowInternal,
    }),
    [showInternal],
  );

  return (
    <ApiFilterContext.Provider value={contextValue}>
      <LargeCenteredLayout>
        <PageContentFontWrapper className="sw-typo-default">
          <Helmet defer={false} title={translate('api_documentation.page')} />
          <Spinner isLoading={isLoading}>
            {data && (
              <div className="sw-w-full sw-flex">
                <NavContainer aria-label={translate('api_documentation.page')} className="sw--mx-2">
                  <div className="sw-w-[300px] lg:sw-w-[390px] sw-mx-2">
                    <ApiSidebar
                      docInfo={data.info}
                      apisList={apis.map(({ name, method, info }) => ({
                        method,
                        name,
                        info,
                      }))}
                    />
                  </div>
                </NavContainer>
                <main
                  className="sw-relative sw-ml-12 sw-flex-1 sw-overflow-y-auto sw-py-6"
                  style={{ height: 'calc(100vh - 160px)' }}
                >
                  <Spinner isLoading={isLoading}>
                    {!activeData && (
                      <>
                        <Title>{translate('about')}</Title>
                        <p>{data.info.description}</p>
                      </>
                    )}
                    {data && activeData && (
                      <ApiInformation
                        apiUrl={data.servers?.[0]?.url ?? ''}
                        name={activeData.name}
                        data={activeData.info}
                        method={activeData.method}
                      />
                    )}
                  </Spinner>
                </main>
              </div>
            )}
          </Spinner>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    </ApiFilterContext.Provider>
  );
}

const NavContainer = styled.nav`
  scrollbar-gutter: stable;
  overflow-y: auto;
  overflow-x: hidden;
  height: calc(100vh - 160px);
  padding-top: 1.5rem;
  padding-bottom: 1.5rem;
`;
