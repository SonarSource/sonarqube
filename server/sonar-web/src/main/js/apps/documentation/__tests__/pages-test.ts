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
import { ParsedContent } from '../../../helpers/markdown';
import { mockDocumentationMarkdown } from '../../../helpers/testMocks';

jest.mock('remark', () => ({
  default: () => ({
    parse: jest.fn().mockReturnValue({})
  })
}));

jest.mock('unist-util-visit', () => ({
  default: (_: any, cb: (node: { type: string; value: string }) => void) => {
    cb({ type: 'text', value: 'Text content' });
  }
}));

const lorem = {
  url: 'analysis/languages/lorem',
  title: 'toto doc',
  key: 'toto',
  content: 'TOTO CONTENT'
};
const foo = {
  url: `analysis/languages/foo`,
  title: 'foo doc',
  key: 'foo'
};
const loremDoc = mockDocumentationMarkdown(lorem);
const fooDoc = mockDocumentationMarkdown(foo);

jest.mock('../documentation.directory-loader', () => [
  { content: loremDoc, path: lorem.url },
  { content: fooDoc, path: foo.url }
]);

it('should correctly parse files', () => {
  const pages = getPages();
  expect(pages.length).toBe(2);

  expect(pages[0]).toMatchObject({
    relativeName: lorem.url,
    url: `/${lorem.url}/`,
    title: lorem.title,
    navTitle: undefined,
    order: -1,
    scope: undefined,
    content: lorem.content
  });

  expect(pages[1]).toMatchObject({
    relativeName: foo.url,
    url: `/${foo.url}/`,
    title: foo.title,
    navTitle: undefined,
    order: -1,
    scope: undefined
  });
});

it('should correctly handle overrides (replace & add)', () => {
  const overrideFooDoc = {
    content: 'OVERRIDE_FOO',
    title: 'OVERRIDE_TITLE_FOO',
    key: foo.key
  };
  const newDoc = {
    content: 'TATA',
    title: 'TATA_TITLE',
    key: 'tata'
  };

  const overrides: T.Dict<ParsedContent> = {};
  overrides[foo.url] = { frontmatter: overrideFooDoc, content: overrideFooDoc.content };
  overrides[`analysis/languages/${newDoc.key}`] = { frontmatter: newDoc, content: newDoc.content };
  const pages = getPages(overrides);

  expect(pages.length).toBe(3);
  expect(pages[1].content).toBe(overrideFooDoc.content);
  expect(pages[1].title).toBe(overrideFooDoc.title);
  expect(pages[2].content).toBe(newDoc.content);
  expect(pages[2].title).toBe(newDoc.title);
});

function getPages(overrides: T.Dict<ParsedContent> = {}) {
  // This allows the use of out-of-scope data inside jest.mock
  // Usually, it is impossible as jest.mock'ed module is hoisted on the top of the file
  return require.requireActual('../pages').default(overrides);
}
