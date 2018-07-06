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
import DocLink from './DocLink';
import DocParagraph from './DocParagraph';
import DocImg from './DocImg';
import DocTooltipLink from './DocTooltipLink';
import { separateFrontMatter } from '../../helpers/markdown';
import { isSonarCloud } from '../../helpers/system';

interface Props {
  childProps?: { [k: string]: string };
  className?: string;
  content: string | undefined;
  displayH1?: boolean;
  isTooltip?: boolean;
}

export default function DocMarkdownBlock({
  childProps,
  className,
  content,
  displayH1,
  isTooltip
}: Props) {
  const parsed = separateFrontMatter(content || '');
  return (
    <div className={classNames('markdown', className)}>
      {displayH1 && <h1>{parsed.frontmatter.title}</h1>}
      {
        remark()
          // .use(remarkInclude)
          .use(reactRenderer, {
            remarkReactComponents: {
              // do not render outer <div />
              div: React.Fragment,
              // use custom link to render documentation anchors
              a: isTooltip ? withChildProps(DocTooltipLink, childProps) : DocLink,
              // used to handle `@include`
              p: DocParagraph,
              // use custom img tag to render documentation images
              img: DocImg
            },
            toHast: {}
          })
          .processSync(filterContent(parsed.content)).contents
      }
    </div>
  );
}

function withChildProps<P>(
  WrappedComponent: React.ComponentType<P & { customProps?: { [k: string]: string } }>,
  childProps?: { [k: string]: string }
) {
  return function withChildProps(props: P) {
    return <WrappedComponent customProps={childProps} {...props} />;
  };
}

function filterContent(content: string) {
  const beginning = isSonarCloud() ? '<!-- sonarqube -->' : '<!-- sonarcloud -->';
  const ending = isSonarCloud() ? '<!-- /sonarqube -->' : '<!-- /sonarcloud -->';

  let newContent = content;
  let start = newContent.indexOf(beginning);
  let end = newContent.indexOf(ending);
  while (start !== -1 && end !== -1) {
    newContent = newContent.substring(0, start) + newContent.substring(end + ending.length);
    start = newContent.indexOf(beginning);
    end = newContent.indexOf(ending);
  }

  return newContent;
}
