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

import { OpenAPIV3 } from 'openapi-types';
import { Badge, SubHeading, Title } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { ExcludeReferences, InternalExtension } from '../types';
import { getApiEndpointKey } from '../utils';
import ApiParameters from './ApiParameters';
import ApiResponses from './ApiResponses';
import RestMethodPill from './RestMethodPill';

interface Props {
  apiUrl: string;
  data: ExcludeReferences<OpenAPIV3.OperationObject<InternalExtension>>;
  method: string;
  name: string;
}

export default function ApiInformation({ name, data, method, apiUrl }: Readonly<Props>) {
  return (
    <>
      {data.summary && <Title>{data.summary}</Title>}
      <SubHeading>
        <RestMethodPill method={method} />
        <span className="sw-ml-4">{apiUrl.replace(/.*(?=\/api)/, '') + name}</span>
        {data['x-internal'] && (
          <Badge variant="new" className="sw-ml-3">
            {translate('internal')}
          </Badge>
        )}
        {data.deprecated && (
          <Badge variant="deleted" className="sw-ml-3">
            {translate('deprecated')}
          </Badge>
        )}
      </SubHeading>
      {data.description && <div>{data.description}</div>}
      <div className="sw-grid sw-grid-cols-2 sw-gap-4 sw-mt-4">
        <div>
          <ApiParameters key={getApiEndpointKey(name, method)} data={data} />
        </div>
        <div>
          <ApiResponses key={getApiEndpointKey(name, method)} responses={data.responses} />
        </div>
      </div>
    </>
  );
}
