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

interface PrismicResponse {
  page: number;
  results: PrismicResult[];
  results_per_page: number;
  total_results_size: number;
}

interface PrismicResult {
  data: {
    notification: string;
    publication_date: string;
    body: PrismicResultFeature[];
  };
}

interface PrismicResultFeature {
  items: Array<{
    category: {
      data: {
        color: string;
        name: string;
      };
    };
  }>;
  primary: {
    description: string;
    read_more_link: {
      url?: string;
    };
  };
}

export interface PrismicFeatureNews {
  notification: string;
  publicationDate: string;
  features: Array<{
    categories: Array<{
      color: string;
      name: string;
    }>;
    description: string;
    readMore?: string;
  }>;
}

const PRISMIC_API_URL = 'https://sonarsource.cdn.prismic.io/api/v2';

export function fetchPrismicRefs() {
  return getCorsJSON(PRISMIC_API_URL).then((response: { refs: PrismicRef[] }) => {
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
  }).then(({ results }: { results: PrismicNews[] }) => results);
}

export function fetchPrismicFeatureNews(data: {
  accessToken: string;
  p?: number;
  ps?: number;
  ref: string;
}): Promise<{ news: PrismicFeatureNews[]; paging: T.Paging }> {
  return getCorsJSON(PRISMIC_API_URL + '/documents/search', {
    access_token: data.accessToken,
    fetchLinks: 'sc_category.color,sc_category.name',
    orderings: '[my.sc_product_news.publication_date desc]',
    page: data.p || 1,
    pageSize: data.ps || 1,
    q: ['[[at(document.type, "sc_product_news")]]'],
    ref: data.ref
  }).then(({ page, results, results_per_page, total_results_size }: PrismicResponse) => ({
    news: results.map(result => ({
      notification: result.data.notification,
      publicationDate: result.data.publication_date,
      features: result.data.body.map(feature => ({
        categories: feature.items.map(item => item.category.data).filter(Boolean),
        description: feature.primary.description,
        readMore: feature.primary.read_more_link.url
      }))
    })),
    paging: {
      pageIndex: page,
      pageSize: results_per_page,
      total: total_results_size
    }
  }));
}
