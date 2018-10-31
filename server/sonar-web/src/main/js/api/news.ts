/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getCorsJSON } from '../helpers/request';

interface PrismicRef {
  id: string;
  ref: string;
}

export interface PrismicNews {
  data: { title: string };
  last_publication_date: string;
  uid: string;
}

const PRISMIC_API_URL = 'https://sonarsource.cdn.prismic.io/api/v2';

export function fetchPrismicRefs() {
  return getCorsJSON(PRISMIC_API_URL).then((response: { refs: Array<PrismicRef> }) => {
    const master = response && response.refs.find(ref => ref.id === 'master');
    if (!master) {
      return Promise.reject('No master ref found');
    }
    return Promise.resolve(master);
  });
}

export function fetchPrismicNews(data: {
  accessToken: string;
  ps?: number;
  ref: string;
  tag?: string;
}) {
  const q = ['[[at(document.type, "blog_sonarsource_post")]]'];
  if (data.tag) {
    q.push(`[[at(document.tags,["${data.tag}"])]]`);
  }
  return getCorsJSON(PRISMIC_API_URL + '/documents/search', {
    access_token: data.accessToken,
    orderings: '[document.first_publication_date desc]',
    pageSize: data.ps || 1,
    q,
    ref: data.ref
  }).then(({ results }: { results: Array<PrismicNews> }) => results);
}
