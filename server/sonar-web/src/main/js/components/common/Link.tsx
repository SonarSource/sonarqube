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
import * as React from 'react';
import { Link as ReactRouterDomLink, LinkProps as ReactRouterDomLinkProps } from 'react-router-dom';
import { isWebUri } from 'valid-url';
import { translate } from '../../helpers/l10n';
import DetachIcon from '../icons/DetachIcon';

type OriginalLinkProps = ReactRouterDomLinkProps & React.RefAttributes<HTMLAnchorElement>;

export interface LinkProps extends OriginalLinkProps {
  size?: number;
}

const DEFAULT_ICON_SIZE = 14;

function Link({ children, size, ...props }: LinkProps, ref: React.ForwardedRef<HTMLAnchorElement>) {
  if (typeof props.to === 'string' && isWebUri(props.to)) {
    // The new React Router DOM's <Link> component no longer supports external links.
    // We have to use the <a> element instead.
    const { to, ...anchorProps } = props;
    return (
      <a
        ref={ref}
        href={to}
        rel={anchorProps.target === '_blank' ? 'noopener noreferrer' : undefined}
        {...anchorProps}
      >
        {anchorProps.target === '_blank' && (
          <DetachIcon
            label={translate('opens_in_new_window')}
            size={size || DEFAULT_ICON_SIZE}
            className="little-spacer-right"
          />
        )}
        {children}
      </a>
    );
  }

  return (
    <ReactRouterDomLink
      ref={ref}
      rel={props.target === '_blank' ? 'noopener noreferrer' : undefined}
      {...props}
    >
      {children}
    </ReactRouterDomLink>
  );
}

export default React.forwardRef(Link);
