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
import * as React from 'react';
import * as classNames from 'classnames';
import remark from 'remark';
import reactRenderer from 'remark-react';
import * as matter from 'gray-matter';
import DocLink from './DocLink';
import DocParagraph from './DocParagraph';
import DocImg from './DocImg';

interface Props {
  className?: string;
  content: string | undefined;
  displayH1?: boolean;
}

export default function DocMarkdownBlock({ className, content, displayH1 }: Props) {
  const parsed = matter(content || '');
  return (
    <div className={classNames('markdown', className)}>
      {displayH1 && <h1>{parsed.data.title}</h1>}
      {
        remark()
          // .use(remarkInclude)
          .use(reactRenderer, {
            remarkReactComponents: {
              // do not render outer <div />
              div: React.Fragment,
              // use custom link to render documentation anchors
              a: DocLink,
              // used to handle `@include`
              p: DocParagraph,
              // use custom img tag to render documentation images
              img: DocImg
            },
            toHast: {}
          })
          .processSync(parsed.content).contents
      }
    </div>
  );
}
