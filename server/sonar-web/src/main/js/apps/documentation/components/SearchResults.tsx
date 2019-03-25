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
import * as React from 'react';
import lunr, { LunrBuilder, LunrIndex, LunrToken } from 'lunr';
import { sortBy } from 'lodash';
import SearchResultEntry, { SearchResult } from './SearchResultEntry';
import { DocumentationEntry, getUrlsList, DocsNavigationItem } from '../utils';

interface Props {
  navigation: DocsNavigationItem[];
  pages: DocumentationEntry[];
  query: string;
  splat: string;
}

export default class SearchResults extends React.PureComponent<Props> {
  index: LunrIndex;

  constructor(props: Props) {
    super(props);
    this.index = lunr(function() {
      this.use(tokenContextPlugin);
      this.ref('relativeName');
      this.field('title', { boost: 10 });
      this.field('text');

      this.metadataWhitelist = ['position', 'tokenContext'];

      props.pages
        .filter(page => getUrlsList(props.navigation).includes(page.url))
        .forEach(page => this.add(page));
    });
  }

  render() {
    const query = this.props.query.toLowerCase();
    const results = this.index
      .search(
        query
          .split(/\s+/)
          .map(s => `${s}~1 ${s}*`)
          .join(' ')
      )
      .map(match => {
        const page = this.props.pages.find(page => page.relativeName === match.ref);
        const highlights: T.Dict<[number, number][]> = {};
        let longestTerm = '';
        let exactMatch = false;

        // Loop over all matching terms/tokens.
        Object.keys(match.matchData.metadata).forEach(term => {
          // Remember the longest term that matches the query as close as possible.
          if (query.includes(term.toLowerCase()) && longestTerm.length < term.length) {
            longestTerm = term;
          }

          Object.keys(match.matchData.metadata[term]).forEach(fieldName => {
            const { position: positions, tokenContext: tokenContexts } = match.matchData.metadata[
              term
            ][fieldName];

            highlights[fieldName] = [...(highlights[fieldName] || []), ...positions];

            // Check if we have an *exact match*.
            if (!exactMatch && tokenContexts) {
              tokenContexts.forEach((tokenContext: string) => {
                if (!exactMatch && tokenContext.includes(query)) {
                  exactMatch = true;
                }
              });
            }
          });
        });

        return { exactMatch, highlights, longestTerm, page, query };
      })
      .filter(result => result.page) as SearchResult[];

    // Re-order results by the length of the longest matched term and by exact
    // match (if applicable). The longer the matched term is, the higher the
    // chance the result is more relevant.
    const sortedResults = sortBy(
      // Sort by longest term.
      sortBy(results, result => -result.longestTerm.length),
      // Sort by exact match.
      result => result.exactMatch && -1
    );

    return (
      <>
        {sortedResults.map(result => (
          <SearchResultEntry
            active={result.page.relativeName === this.props.splat}
            key={result.page.relativeName}
            result={result}
          />
        ))}
      </>
    );
  }
}

// Lunr doesn't support exact multiple-term matching. Meaning "foo bar" will not
// boost a sentence like "Foo bar baz" more than "Baz bar foo". In order to
// provide more accurate results, we store the token context, to see if we can
// perform an "exact match". Unfortunately, we cannot extend the search logic,
// only the tokenizer at *index time*. This is why we store the context as
// meta-data, and post-process the matches before rendering (see above). For
// performance reasons, we only add 2 extra tokens, one in front, one after.
// This means we support "exact macthing" for up to 3 terms. More search terms
// would fallback to the regular matching algorithm, which is OK: the more terms
// searched for, the better the standard algorithm will perform anyway. In the
// end, the best would be for Lunr to support multi-term matching, as extending
// the search algorithm for this would be way too complicated.
function tokenContextPlugin(builder: LunrBuilder) {
  const pipelineFunction = (token: LunrToken, index: number, tokens: LunrToken[]) => {
    const prevToken = tokens[index - 1] || '';
    const nextToken = tokens[index + 1] || '';
    token.metadata['tokenContext'] = [prevToken.toString(), token.toString(), nextToken.toString()]
      .filter(s => s.length)
      .join(' ')
      .toLowerCase();
    return token;
  };

  (lunr as any).Pipeline.registerFunction(pipelineFunction, 'tokenContext');
  builder.pipeline.before((lunr as any).stemmer, pipelineFunction);
  builder.metadataWhitelist.push('tokenContext');
}
