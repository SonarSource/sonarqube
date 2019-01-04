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
import { getFrontMatter, separateFrontMatter, filterContent } from '../markdown';
import { isSonarCloud } from '../system';

jest.mock('../system', () => ({
  getInstance: () => 'SonarQube',
  isSonarCloud: jest.fn().mockReturnValue(false)
}));

it('returns parsed frontmatter of one item', () => {
  expect(
    getFrontMatter(`
      ---
      title: Foo
      ---
      
      some content here
    `)
  ).toEqual({ title: 'Foo' });
});

it('returns parsed frontmatter of two items', () => {
  expect(
    getFrontMatter(`
      ---
      title: Foo
      scope: sonarcloud
      ---
      
      some content here
    `)
  ).toEqual({ title: 'Foo', scope: 'sonarcloud' });
});

it('returns empty object when frontmatter is missing', () => {
  expect(
    getFrontMatter(`
      some content here
    `)
  ).toEqual({});
});

it('returns empty object when frontmatter is unfinished', () => {
  expect(
    getFrontMatter(`
      --- 
      title: Foo

      some content here
    `)
  ).toEqual({});
});

it('ignores frontmatter in wrong format', () => {
  expect(
    getFrontMatter(`
      --- 
      title: Foo
      scope: sonarcloud: sonarqube
      ---

      some content here
    `)
  ).toEqual({ title: 'Foo' });
});

it('returns parsed frontmatter and the rest of the content', () => {
  expect(
    separateFrontMatter(`
---
title: Foo
---

some content here`)
  ).toEqual({ content: '\nsome content here', frontmatter: { title: 'Foo' } });
});

it('returns empty object and content when  frontmatter is missing', () => {
  expect(separateFrontMatter('some content here')).toEqual({
    content: 'some content here',
    frontmatter: {}
  });
});

it('returns full content when frontmatter has bad formatting', () => {
  const content = `
    ----
    title: Foo
    scope: sonarcloud
    ---

    some content here`;

  expect(separateFrontMatter(content)).toEqual({ content, frontmatter: {} });
});

it('replaces {instance}', () => {
  expect(
    filterContent('This is {instance} content. It replaces all {instance}{instance} messages')
  ).toBe('This is SonarQube content. It replaces all SonarQubeSonarQube messages');
});

it('should cut sonarqube/sonarcloud/static content', () => {
  const content = `
This text has inline text for <!-- sonarqube -->SonarQube<!-- /sonarqube --><!-- sonarcloud -->SonarCloud<!-- /sonarcloud -->. Donec sed nulla magna.

<!-- sonarqube -->
This is text for SonarQube, multi-line. Consectetur adipiscing elit. Duis dignissim nulla at massa iaculis interdum.
Aenean sit amet lacus a tortor ullamcorper interdum. Donec sed nulla magna.
<!-- /sonarqube -->

<!-- sonarcloud -->
This is text for SonarCloud, multi-line. In hac habitasse platea dictumst. Duis sagittis semper sapien nec tempor. Nullam vehicula nisi vitae nisi interdum aliquam. Mauris volutpat nunc non fermentum rhoncus. Aenean laoreet, orci vitae tempor bibendum,
metus nisl euismod neque, vitae euismod nibh nisl eu velit. Vivamus luctus suscipit elit vel semper.
<!-- /sonarcloud -->

<!-- static -->
This is static text.
<!-- /static -->

<!-- sonarqube -->
This is text for SonarQube, single line.
<!-- /sonarqube -->

* In hac habitasse
* Duis sagittis semper sapien nec tempor
<!-- sonarqube -->* This is a bullet point for SonarQube<!-- /sonarqube -->
<!-- sonarcloud -->* This is a bullet point for SonarCloud<!-- /sonarcloud -->
* Platea dictumst

Duis sagittis semper sapien nec tempor. Nullam vehicula nisi vitae nisi interdum aliquam.

| Parameter Name        | Description |
| --------------------- | ------------------ |
| sonar.pullrequest.github.repository | SLUG of the GitHub Repo |
<!-- sonarqube -->
| sonar.pullrequest.github.endpoint | The API url for your GitHub instance. |
<!-- /sonarqube -->
`;

  expect(filterContent(content)).toMatchSnapshot();

  (isSonarCloud as jest.Mock).mockReturnValueOnce(true);
  expect(filterContent(content)).toMatchSnapshot();
});
