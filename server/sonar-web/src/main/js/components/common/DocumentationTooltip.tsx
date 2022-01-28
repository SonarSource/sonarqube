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
import * as React from 'react';
import { Link } from 'react-router';
import { isWebUri } from 'valid-url';
import HelpTooltip from '../../components/controls/HelpTooltip';
import DetachIcon from '../../components/icons/DetachIcon';

export interface DocumentationTooltipProps {
  children?: React.ReactNode;
  className?: string;
  content?: React.ReactNode;
  links?: Array<{ href: string; label: string; inPlace?: boolean }>;
  title?: string;
}

export default function DocumentationTooltip(props: DocumentationTooltipProps) {
  const { className, content, links, title } = props;

  return (
    <HelpTooltip
      className={className}
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

              {links.map(({ href, label, inPlace }) => (
                <div className="little-spacer-bottom" key={label}>
                  {inPlace ? (
                    <Link to={href}>
                      <span>{label}</span>
                    </Link>
                  ) : (
                    <Link
                      className="display-inline-flex-center link-with-icon"
                      to={href}
                      rel="noopener noreferrer"
                      target="_blank">
                      {isWebUri(href) && <DetachIcon size={14} className="spacer-right" />}
                      <span>{label}</span>
                    </Link>
                  )}
                </div>
              ))}
            </>
          )}
        </div>
      }>
      {props.children}
    </HelpTooltip>
  );
}
