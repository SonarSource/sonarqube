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
import remarkCustomBlocks from 'remark-custom-blocks';
import DocLink from './DocLink';
import DocImg from './DocImg';
import DocTooltipLink from './DocTooltipLink';
import remarkToc from './plugins/remark-toc';
import DocCollapsibleBlock from './DocCollapsibleBlock';
import { separateFrontMatter, filterContent } from '../../helpers/markdown';
import { scrollToElement } from '../../helpers/scrolling';

interface Props {
  childProps?: { [k: string]: string };
  className?: string;
  content: string | undefined;
  displayH1?: boolean;
  isTooltip?: boolean;
}

export default class DocMarkdownBlock extends React.PureComponent<Props> {
  node: HTMLElement | null = null;

  handleAnchorClick = (href: string, event: React.MouseEvent<HTMLAnchorElement>) => {
    if (this.node) {
      const element = this.node.querySelector(`#${href.substr(1)}`);
      if (element) {
        event.preventDefault();
        scrollToElement(element, { bottomOffset: window.innerHeight - 80 });
      }
    }
  };

  render() {
    const { childProps, content, className, displayH1, isTooltip } = this.props;
    const parsed = separateFrontMatter(content || '');
    return (
      <div className={classNames('markdown', className)} ref={ref => (this.node = ref)}>
        {displayH1 && <h1>{parsed.frontmatter.title}</h1>}
        {
          remark()
            .use(remarkToc, { maxDepth: 3 })
            // TODO find a way to replace these custom blocks with real Alert components
            .use(remarkCustomBlocks, {
              danger: { classes: 'alert alert-danger' },
              warning: { classes: 'alert alert-warning' },
              info: { classes: 'alert alert-info' },
              success: { classes: 'alert alert-success' },
              collapse: { classes: 'collapse' }
            })
            .use(reactRenderer, {
              remarkReactComponents: {
                div: Block,
                // use custom link to render documentation anchors
                a: isTooltip
                  ? withChildProps(DocTooltipLink, childProps)
                  : withChildProps(DocLink, { onAnchorClick: this.handleAnchorClick }),
                // use custom img tag to render documentation images
                img: DocImg
              },
              toHast: {},
              sanitize: false
            })
            .processSync(filterContent(parsed.content)).contents
        }
      </div>
    );
  }
}

function withChildProps<P>(
  WrappedComponent: React.ComponentType<P & { customProps?: { [k: string]: any } }>,
  childProps?: { [k: string]: any }
) {
  return function withChildProps(props: P) {
    return <WrappedComponent customProps={childProps} {...props} />;
  };
}

function Block(props: React.HtmlHTMLAttributes<HTMLDivElement>) {
  if (props.className) {
    if (props.className.includes('collapse')) {
      return <DocCollapsibleBlock>{props.children}</DocCollapsibleBlock>;
    } else {
      return <div className={classNames('cut-margins', props.className)}>{props.children}</div>;
    }
  } else {
    return props.children;
  }
}
