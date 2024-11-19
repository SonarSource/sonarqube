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

import { Button, ButtonVariety, IconQuestionMark, Popover } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';

type Props =
  | {
      description?: never;
      link: DocLink;
      linkText?: never;
      noDescription?: boolean;
      property: string;
      title?: never;
    }
  | {
      description?: string | React.ReactNode;
      link: DocLink;
      linkText: string;
      noDescription?: never;
      property?: never;
      title: string;
    };

export function FacetHelp({ property, title, description, noDescription, link, linkText }: Props) {
  const intl = useIntl();
  return (
    <Popover
      title={
        property !== undefined
          ? intl.formatMessage({ id: `issues.facet.${property}.help.title` })
          : title
      }
      description={
        ((property !== undefined && !noDescription) || description) && property
          ? intl.formatMessage(
              { id: `issues.facet.${property}.help.description` },
              { p1: (text) => <p>{text}</p>, p: (text) => <p className="sw-mt-4">{text}</p> },
            )
          : description
      }
      footer={
        <DocumentationLink standalone to={link}>
          {property ? intl.formatMessage({ id: `issues.facet.${property}.help.link` }) : linkText}
        </DocumentationLink>
      }
    >
      <Button
        className="sw-p-0 sw-h-fit sw-min-h-fit"
        aria-label={intl.formatMessage({ id: 'help' })}
        variety={ButtonVariety.DefaultGhost}
      >
        <IconQuestionMark color="echoes-color-icon-subdued" />
      </Button>
    </Popover>
  );
}
