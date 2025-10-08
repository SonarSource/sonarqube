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

import { useEffect, useState, useMemo } from 'react';
import { Helmet } from 'react-helmet-async';
import { LargeCenteredLayout, PageContentFontWrapper, InputSearch, Spinner } from '~design-system';
import { translate } from '../../helpers/l10n';
import { ArchivedOrganization } from '../../types/types';
import { getArchivedOrganizations } from '../../api/organizations';
import { PAGE_SIZE } from './constants';
import Header from './components/Header';
import ListFooter from '../../components/controls/ListFooter';
import OrganizationsList from './components/OrganizationsList';

export default function OrganizationsArchiveApp() {
  const [data, setData] = useState<ArchivedOrganization[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [isLoading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const data = await getArchivedOrganizations();
      setData(data);
      setLoading(false);
    })();
  }, []);

  const filtered = useMemo(() => {
    return data.filter(org =>
      org.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [data, searchTerm]);

  const organizations = useMemo(() => {
    return filtered.slice(0, visibleCount);
  }, [filtered, visibleCount]);

  const showMore = ()=>{
    setVisibleCount(prev => prev + PAGE_SIZE);
  };

  return (
    <LargeCenteredLayout as="main" id="org-archived-page">
      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <Helmet defer={false} title={translate('organization.archived.page')} />
        <Header />
        <Spinner loading={isLoading}>
        {
          data.length === 0 ?
            (<> <span>{ translate('organization.archived.not_found') }</span> </>) :
            (<>
              <div className="sw-flex sw-my-4">
                <InputSearch
                    id="org-archived-search"
                    minLength={2}
                    onChange={(searchTerm: string) => setSearchTerm(searchTerm)}
                    placeholder={translate('search.search_by_name')}
                    value={searchTerm}
                />
              </div>
              <OrganizationsList
                organizations={organizations}
              />
              <ListFooter
                  count={organizations.length}
                  loadMore={showMore}
                  ready={!isLoading}
                  total={data.length}
              />
            </>)
        }
        </Spinner>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
