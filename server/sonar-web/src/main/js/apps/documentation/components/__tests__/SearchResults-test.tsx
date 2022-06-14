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
import { shallow } from 'enzyme';
import lunr, { LunrBuilder, LunrToken } from 'lunr';
import * as React from 'react';
import { mockDocumentationEntry } from '../../../../helpers/testMocks';
import { getUrlsList } from '../../navTreeUtils';
import { DocumentationEntry } from '../../utils';
import SearchResultEntry from '../SearchResultEntry';
import SearchResults, { tokenContextPlugin, tokenContextPluginCallback } from '../SearchResults';

jest.mock('../../navTreeUtils', () => ({
  getUrlsList: jest.fn().mockReturnValue([])
}));

jest.mock('lunr', () => {
  const lunr = jest.fn(() => ({
    search: jest.fn(() => [
      {
        ref: 'lorem/origin',
        matchData: {
          metadata: {
            simply: {
              title: { position: [[19, 5]] },
              text: {
                position: [
                  [15, 6],
                  [28, 4]
                ],
                tokenContext: ['is simply dummy', 'simply dummy text']
              }
            }
          }
        }
      },
      {
        ref: 'foobar',
        matchData: {
          metadata: {
            simply: {
              title: { position: [[23, 4]] },
              text: {
                position: [
                  [111, 6],
                  [118, 4]
                ],
                tokenContext: ['dummy simply text']
              }
            }
          }
        }
      }
    ])
  }));

  (lunr as any).Pipeline = {
    registerFunction: jest.fn()
  };

  return lunr;
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ query: '' }).type()).toBeNull();
});

describe('search engine', () => {
  class LunrIndexMock {
    plugins: Function[] = [];
    fields: Array<{ field: string; args: any }> = [];
    docs: DocumentationEntry[] = [];
    metadataWhitelist: string[] = [];

    use(fn: Function) {
      this.plugins.push(fn);
    }

    ref(_ref: string) {
      /* noop */
    }

    field(field: string, args = {}) {
      this.fields.push({ field, args });
    }

    add(doc: DocumentationEntry) {
      this.docs.push(doc);
    }
  }

  it('should correctly populate the index', () => {
    (getUrlsList as jest.Mock).mockReturnValueOnce(['/lorem/index', '/lorem/origin']);

    shallowRender();

    // Fetch the callback passed to lunr(), which serves as the index constructor.
    const indexConstructor: Function = (lunr as jest.Mock).mock.calls[0][0];

    // Apply it to our mock index.
    const lunrMock = new LunrIndexMock();
    indexConstructor.apply(lunrMock);

    expect(lunrMock.docs.length).toBe(2);
    expect(lunrMock.plugins).toContain(tokenContextPlugin);
    expect(lunrMock.metadataWhitelist).toEqual(['position', 'tokenContext']);
    expect(lunrMock.fields).toEqual([
      expect.objectContaining({ field: 'title', args: { boost: 10 } }),
      expect.objectContaining({ field: 'text' })
    ]);
  });

  it('should correctly look for an exact match', () => {
    // No exact match, should sort as the matches came in.
    const wrapper = shallowRender({ query: 'text simply' });
    expect(
      wrapper
        .find(SearchResultEntry)
        .at(0)
        .props().result.page.relativeName
    ).toBe('lorem/origin');

    // Exact match, specific page should be at the top.
    wrapper.setProps({ query: 'simply text' });
    expect(
      wrapper
        .find(SearchResultEntry)
        .at(0)
        .props().result.page.relativeName
    ).toBe('foobar');
  });

  it('should trigger a search if query is set', () => {
    const wrapper = shallowRender({ query: undefined });
    expect(wrapper.instance().index.search).not.toBeCalled();
    wrapper.setProps({ query: 'si:+mply text' });
    expect(wrapper.instance().index.search).toBeCalledWith('simply~1 simply* text~1 text*');
  });
});

describe('tokenContextPluginCallback', () => {
  class LunrTokenMock {
    str: string;
    metadata: any;

    constructor(str: string) {
      this.str = str;
      this.metadata = {};
    }

    toString() {
      return this.str;
    }
  }

  class LunrBuilderMock {
    pipeline: { before: (stemmer: any, cb: Function) => void };
    metadataWhitelist: string[];

    constructor() {
      this.pipeline = {
        before: () => {
          /* noop */
        }
      };
      this.metadataWhitelist = [];
    }
  }

  function mockLunrToken(str: string): LunrToken {
    return new LunrTokenMock(str);
  }

  function mockLunrBuilder(): LunrBuilder {
    return new LunrBuilderMock();
  }

  it('should correctly provide token context for text', () => {
    const tokens = [
      mockLunrToken('this'),
      mockLunrToken('is'),
      mockLunrToken('some'),
      mockLunrToken('text')
    ];

    expect(tokenContextPluginCallback(mockLunrToken('this'), 0, tokens).metadata).toEqual(
      expect.objectContaining({ tokenContext: 'this is' })
    );
    expect(tokenContextPluginCallback(mockLunrToken('is'), 1, tokens).metadata).toEqual(
      expect.objectContaining({ tokenContext: 'this is some' })
    );
    expect(tokenContextPluginCallback(mockLunrToken('some'), 2, tokens).metadata).toEqual(
      expect.objectContaining({ tokenContext: 'is some text' })
    );
    expect(tokenContextPluginCallback(mockLunrToken('text'), 3, tokens).metadata).toEqual(
      expect.objectContaining({ tokenContext: 'some text' })
    );
  });

  it('should only register the plugin once', () => {
    tokenContextPlugin(mockLunrBuilder());
    tokenContextPlugin(mockLunrBuilder());
    expect((lunr as any).Pipeline.registerFunction).toBeCalledTimes(1);
  });
});

function shallowRender(props: Partial<SearchResults['props']> = {}) {
  return shallow<SearchResults>(
    <SearchResults
      navigation={['lorem/index', 'lorem/origin', 'foobar']}
      pages={[
        mockDocumentationEntry({
          title: 'Lorem Ipsum',
          relativeName: 'lorem/index',
          url: '/lorem/index',
          content:
            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.",
          text:
            "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book."
        }),
        mockDocumentationEntry({
          title: 'Where does it come from?',
          relativeName: 'lorem/origin',
          url: '/lorem/origin',
          content:
            'Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words.',
          text:
            'Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words.'
        }),
        mockDocumentationEntry({
          title: 'Where does Foobar come from?',
          relativeName: 'foobar',
          url: '/foobar',
          content:
            'Foobar is a universal variable understood to represent whatever is being discussed. Now we need some keywords: simply text.',
          text:
            'Foobar is a universal variable understood to represent whatever is being discussed. Now we need some keywords: simply text.'
        })
      ]}
      query="what is 42"
      splat="foobar"
      {...props}
    />
  );
}
