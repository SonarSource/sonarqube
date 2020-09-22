/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import DetachIcon from 'sonar-ui-common/components/icons/DetachIcon';
import { isWebUri } from 'valid-url';

export interface DocumentationTooltipProps {
  children?: React.ReactNode;
  className?: string;
  content?: React.ReactNode;
  links?: Array<{ href: string; label: string }>;
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

              {links.map(({ href, label }) => (
                <div className="little-spacer-bottom" key={label}>
                  <a
                    className="display-inline-flex-center link-with-icon"
                    href={href}
                    rel="noopener noreferrer"
                    target="_blank">
                    {isWebUri(href) && <DetachIcon size={14} className="spacer-right" />}
                    <span>{label}</span>
                  </a>
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
