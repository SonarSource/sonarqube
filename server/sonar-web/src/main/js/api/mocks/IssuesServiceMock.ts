/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { cloneDeep } from 'lodash';
import { RequestData } from '../../helpers/request';
import { getStandards } from '../../helpers/security-standard';
import { mockPaging } from '../../helpers/testMocks';
import { RawFacet, RawIssuesResponse, ReferencedComponent } from '../../types/issues';
import { Standards } from '../../types/security';
import { searchIssues } from '../issues';

function mockReferenceComponent(override?: Partial<ReferencedComponent>) {
  return {
    key: 'component1',
    name: 'Component1',
    uuid: 'id1',
    ...override
  };
}

export default class IssuesServiceMock {
  isAdmin = false;
  standards?: Standards;

  constructor() {
    (searchIssues as jest.Mock).mockImplementation(this.listHandler);
  }

  reset() {
    this.setIsAdmin(false);
  }

  async getStandards(): Promise<Standards> {
    if (this.standards) {
      return this.standards;
    }
    this.standards = await getStandards();
    return this.standards;
  }

  owasp2021FacetList(): RawFacet {
    return {
      property: 'owaspTop10-2021',
      values: [{ val: 'a1', count: 0 }]
    };
  }

  setIsAdmin(isAdmin: boolean) {
    this.isAdmin = isAdmin;
  }

  listHandler = (query: RequestData): Promise<RawIssuesResponse> => {
    const facets = query.facets.split(',').map((name: string) => {
      if (name === 'owaspTop10-2021') {
        return this.owasp2021FacetList();
      }
      return {
        property: name,
        values: []
      };
    });
    return this.reply({
      components: [mockReferenceComponent()],
      effortTotal: 199629,
      facets,
      issues: [],
      languages: [],
      paging: mockPaging()
    });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
