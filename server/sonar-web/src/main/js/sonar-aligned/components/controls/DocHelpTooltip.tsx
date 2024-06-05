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

import classNames from 'classnames';
import { HelperHintIcon } from 'design-system';
import { first, last } from 'lodash';
import * as React from 'react';
import DocumentationLink from '../../../components/common/DocumentationLink';
import Link from '../../../components/common/Link';
import Tooltip, { Placement } from '../../../components/controls/Tooltip';
import { DocLink } from '../../../helpers/doc-links';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';

export interface DocHelpTooltipProps {
  children?: React.ReactNode;
  className?: string;
  content?: React.ReactNode;
  linkTextLabel?: string;
  links?: Array<
    { inPlace?: boolean; label?: string } & (
      | { doc?: true; href: DocLink }
      | { doc: false; href: string }
    )
  >;
  placement?: Placement;
  title?: string;
}

export default function DocHelpTooltip(props: Readonly<DocHelpTooltipProps>) {
  const nextSelectableNode = React.useRef<HTMLElement | undefined | null>();
  const linksRef = React.useRef<Array<HTMLAnchorElement | null>>([]);
  const helpRef = React.useRef<HTMLElement>(null);
  const { className, children, content, links, title, placement, linkTextLabel } = props;

  function handleShowTooltip() {
    document.addEventListener('keydown', handleTabPress);
  }

  function handleHideTooltip() {
    document.removeEventListener('keydown', handleTabPress);
    nextSelectableNode.current = undefined;
  }

  function handleTabPress(event: KeyboardEvent) {
    if (event.code === KeyboardKeys.Tab) {
      if (event.shiftKey) {
        if (event.target === first(linksRef.current)) {
          helpRef.current?.focus();
        }
        return;
      }
      if (event.target === last(linksRef.current)) {
        event.preventDefault();
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

  const overlay = (
    <div className="sw-py-4">
      {title !== undefined && (
        <div className="sw-mb-2">
          <strong>{title}</strong>
        </div>
      )}

      {content && <div>{content}</div>}

      {links && (
        <>
          <hr className="sw-my-4" />

          {links.map(({ href, label = translate('learn_more'), inPlace, doc = true }, index) => (
            <div className="sw-mb-1" key={label}>
              {index === 0 && linkTextLabel && `${linkTextLabel}: `}
              {doc ? (
                <DocumentationLink
                  to={href as DocLink} // the map above messed up type inference
                  innerRef={(ref) => (linksRef.current[index] = ref)}
                >
                  {label}
                </DocumentationLink>
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
  );

  return (
    <div
      className={classNames(
        'it__help-tooltip sw-inline-flex sw-items-center sw-align-middle',
        className,
      )}
    >
      <Tooltip
        mouseLeaveDelay={0.25}
        onShow={handleShowTooltip}
        onHide={handleHideTooltip}
        content={overlay}
        side={placement}
        isInteractive
      >
        <span
          className="sw-inline-flex sw-items-center"
          data-testid="help-tooltip-activator"
          ref={helpRef}
        >
          {children ?? (
            <HelperHintIcon
              aria-label={translate('tooltip_is_interactive')}
              description={
                <>
                  {translate('tooltip_is_interactive')}
                  {overlay}
                </>
              }
            />
          )}
        </span>
      </Tooltip>
    </div>
  );
}
