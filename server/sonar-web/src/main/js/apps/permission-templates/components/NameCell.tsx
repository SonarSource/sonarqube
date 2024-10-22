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

import { CodeSnippet, ContentCell, Link } from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { PermissionTemplate } from '../../../types/types';
import { PERMISSION_TEMPLATES_PATH } from '../utils';
import Defaults from './Defaults';

interface Props {
  template: PermissionTemplate;
}

export default function NameCell({ template }: Props) {
  const pathname = PERMISSION_TEMPLATES_PATH;

  return (
    <ContentCell>
      <div className="sw-flex sw-flex-col">
        <span>
          <Link to={{ pathname, search: queryToSearchString({ id: template.id }) }}>
            <span className="js-name">{template.name}</span>
          </Link>
        </span>

        {template.defaultFor.length > 0 && (
          <div className="js-defaults sw-mt-2">
            <Defaults template={template} />
          </div>
        )}

        {!!template.description && (
          <div className="js-description sw-mt-2">{template.description}</div>
        )}

        {!!template.projectKeyPattern && (
          <div className="js-project-key-pattern sw-mt-2">
            Project Key Pattern:{' '}
            <CodeSnippet snippet={template.projectKeyPattern} isOneLine noCopy />
          </div>
        )}
      </div>
    </ContentCell>
  );
}
