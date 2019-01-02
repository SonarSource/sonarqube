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
import remark from 'remark';
import visit from 'unist-util-visit';
import { DocumentationEntry, DocumentationEntryScope } from './utils';
import * as Docs from './documentation.directory-loader';
import { separateFrontMatter, filterContent } from '../../helpers/markdown';

export default function getPages(): DocumentationEntry[] {
  return ((Docs as unknown) as Array<{ content: string; path: string }>).map(file => {
    const parsed = separateFrontMatter(file.content);
    const content = filterContent(parsed.content);
    const text = getText(content);

    return {
      relativeName: file.path,
      url: parsed.frontmatter.url || `/${file.path}`,
      title: parsed.frontmatter.title,
      navTitle: parsed.frontmatter.nav || undefined,
      order: Number(parsed.frontmatter.order || -1),
      scope: parsed.frontmatter.scope
        ? (parsed.frontmatter.scope.toLowerCase() as DocumentationEntryScope)
        : undefined,
      text,
      content: file.content
    };
  });
}

function getText(content: string) {
  const ast = remark().parse(content);
  const texts: string[] = [];
  visit(ast, node => {
    if (node.type === `text` || node.type === `inlineCode`) {
      texts.push(node.value);
    }
  });
  return texts.join(' ').replace(/\s+/g, ' ');
}
