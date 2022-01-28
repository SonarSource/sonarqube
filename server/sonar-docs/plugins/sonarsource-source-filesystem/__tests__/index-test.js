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
const { cutAdditionalContent, cleanContent } = require('../index.js');

it('should cut additional content properly', () => {
  const tag = 'special';
  const content = 'This section contains a <!-- special -->special content<!-- /special -->.';
  expect(cutAdditionalContent(content, tag)).toBe('This section contains a .');
});

it('should clean the content', () => {
  const content = `
<!-- sonarcloud -->This is a sonarcloud content to cut<!-- /sonarcloud -->
<!-- sonarqube -->This is a sonarqube content to preserve<!-- /sonarqube -->
<!-- embedded -->This is an embedded content to cut<!-- /embedded -->
<!-- static -->This a static content to preserve<!-- /static -->
This is a {instance} instance
`;
  expect(cleanContent(content)).toBe(`

This is a sonarqube content to preserve

This a static content to preserve
This is a SonarQube instance
`);
});
