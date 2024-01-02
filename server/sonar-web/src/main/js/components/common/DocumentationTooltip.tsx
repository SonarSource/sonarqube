/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { first, last } from 'lodash';
import * as React from 'react';
import HelpTooltip from '../../components/controls/HelpTooltip';
import { KeyboardKeys } from '../../helpers/keycodes';
import DocLink from './DocLink';
import Link from './Link';

export interface DocumentationTooltipProps {
  children?: React.ReactNode;
  className?: string;
  content?: React.ReactNode;
  links?: Array<{ href: string; label: string; inPlace?: boolean; doc?: boolean }>;
  title?: string;
}

export default function DocumentationTooltip(props: DocumentationTooltipProps) {
  const nextSelectableNode = React.useRef<HTMLElement | undefined | null>();
  const linksRef = React.useRef<(HTMLAnchorElement | null)[]>([]);
  const helpRef = React.useRef<HTMLElement>(null);
  const { className, children, content, links, title } = props;

  function handleShowTooltip() {
    document.addEventListener('keydown', handleTabPress);
  }

  function handleHideTooltip() {
    document.removeEventListener('keydown', handleTabPress);
    nextSelectableNode.current = undefined;
  }

  function handleTabPress(event: KeyboardEvent) {
    if (event.code === KeyboardKeys.Tab) {
      if (event.shiftKey === true) {
        if (event.target === first(linksRef.current)) {
          helpRef.current?.focus();
        }
        return;
      }
      if (event.target === last(linksRef.current)) {
        nextSelectableNode.current?.focus();
        return;
      }
      if (nextSelectableNode.current === undefined) {
        nextSelectableNode.current = event.target as HTMLElement;
        event.preventDefault();
        linksRef.current[0]?.focus();
      }
    }
  }

  return (
    <HelpTooltip
      className={className}
      onShow={handleShowTooltip}
      onHide={handleHideTooltip}
      isInteractive={true}
      innerRef={helpRef}
      overlay={
        <div className="big-padded-top big-padded-bottom">
          {title && (
            <div className="spacer-bottom">
              <strong>{title}</strong>
            </div>
          )}

          {content && <p>{content}</p>}

          {links && (
            <>
              <hr className="big-spacer-top big-spacer-bottom" />

              {links.map(({ href, label, inPlace, doc = true }, index) => (
                <div className="little-spacer-bottom" key={label}>
                  {doc ? (
                    <DocLink to={href} innerRef={(ref) => (linksRef.current[index] = ref)}>
                      {label}
                    </DocLink>
                  ) : (
                    <Link
                      to={href}
                      ref={(ref) => (linksRef.current[index] = ref)}
                      target={inPlace ? undefined : '_blank'}
                    >
                      {label}
                    </Link>
                  )}
                </div>
              ))}
            </>
          )}
        </div>
      }
    >
      {children}
    </HelpTooltip>
  );
}
