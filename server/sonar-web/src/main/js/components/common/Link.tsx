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
import * as Echoes from '@sonarsource/echoes-react';
import { OpenNewTabIcon } from 'design-system';
import * as React from 'react';
import { Link as ReactRouterDomLink, LinkProps as ReactRouterDomLinkProps } from 'react-router-dom';
import { isWebUri } from 'valid-url';
import { translate } from '../../helpers/l10n';

type OriginalLinkProps = ReactRouterDomLinkProps & React.RefAttributes<HTMLAnchorElement>;

/** @deprecated Use {@link Echoes.LinkProps | LinkProps} from Echoes instead.
 *
 * Some of the props have changed or been renamed:
 * - `blurAfterClick` is now `shouldBlurAfterClick`
 * - ~`disabled`~ doesn't exist anymore, a disabled link is just a regular text
 * - `forceExternal` is now `isExternal`
 * - `icon` is now `iconLeft` and can only be used with LinkStandalone
 * - `preventDefault` is now `shouldPreventDefault`
 * - `showExternalIcon` is now `hasExternalIcon`
 * - `stopPropagation` is now `shouldStopPropagation`
 */
export interface LinkProps extends OriginalLinkProps {
  size?: number;
}

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
          <OpenNewTabIcon aria-label={translate('opens_in_new_window')} className="sw-mr-1" />
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

/** @deprecated Use either {@link Echoes.Link | Link} or {@link Echoes.LinkStandalone | LinkStandalone} from Echoes instead.
 */
export default React.forwardRef(Link);
