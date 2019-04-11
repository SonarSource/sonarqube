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
import remark from 'remark';
import reactRenderer from 'remark-react';
import { findDOMNode } from 'react-dom';
import * as classNames from 'classnames';
import { debounce, memoize } from 'lodash';
import onlyToc from './plugins/remark-only-toc';
import { translate } from '../../helpers/l10n';

interface Props {
  content: string;
  onAnchorClick: (href: string, event: React.MouseEvent<HTMLAnchorElement>) => void;
}

interface State {
  anchors: AnchorObject[];
  highlightAnchor?: string;
}

interface AnchorObject {
  href: string;
  title: string;
}

export default class DocToc extends React.PureComponent<Props, State> {
  debouncedScrollHandler: () => void;

  node: HTMLDivElement | null = null;

  state: State = { anchors: [] };

  static getAnchors = memoize((content: string) => {
    const file: { contents: JSX.Element } = remark()
      .use(reactRenderer)
      .use(onlyToc)
      .processSync('\n## doctoc\n' + content);

    if (file && file.contents.props.children) {
      let list = file.contents;
      let limit = 10;
      while (limit && list.props.children.length && list.type !== 'ul') {
        list = list.props.children[0];
        limit--;
      }

      if (list.type === 'ul' && list.props.children.length) {
        return list.props.children
          .map((li: JSX.Element | string) => {
            if (typeof li === 'string') {
              return null;
            }

            const anchor = li.props.children[0];
            return {
              href: anchor.props.href,
              title: anchor.props.children[0]
            } as AnchorObject;
          })
          .filter((item: AnchorObject | null) => item);
      }
    }
    return [];
  });

  static getDerivedStateFromProps(props: Props) {
    const { content } = props;
    return { anchors: DocToc.getAnchors(content) };
  }

  constructor(props: Props) {
    super(props);
    this.debouncedScrollHandler = debounce(this.scrollHandler);
  }

  componentDidMount() {
    window.addEventListener('scroll', this.debouncedScrollHandler, true);
    this.scrollHandler();
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.debouncedScrollHandler, true);
  }

  scrollHandler = () => {
    // eslint-disable-next-line react/no-find-dom-node
    const node = findDOMNode(this) as HTMLElement;

    if (!node || !node.parentNode) {
      return;
    }

    const headings: NodeListOf<HTMLHeadingElement> = node.parentNode.querySelectorAll('h2[id]');
    const scrollTop = window.pageYOffset || document.body.scrollTop;
    let highlightAnchor;

    for (let i = 0, len = headings.length; i < len; i++) {
      if (headings.item(i).offsetTop > scrollTop + 120) {
        break;
      }
      highlightAnchor = `#${headings.item(i).id}`;
    }

    this.setState({
      highlightAnchor
    });
  };

  render() {
    const { anchors, highlightAnchor } = this.state;

    if (anchors.length === 0) {
      return null;
    }

    return (
      <div className="markdown-toc">
        <div className="markdown-toc-content">
          <h4>{translate('documentation.on_this_page')}</h4>
          {anchors.map(anchor => {
            return (
              <a
                className={classNames({ active: highlightAnchor === anchor.href })}
                href={anchor.href}
                key={anchor.title}
                onClick={(event: React.MouseEvent<HTMLAnchorElement>) => {
                  this.props.onAnchorClick(anchor.href, event);
                }}>
                {anchor.title}
              </a>
            );
          })}
        </div>
      </div>
    );
  }
}
