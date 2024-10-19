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
import { Accordion, Badge, TextMuted } from 'design-system';
import { isEmpty } from 'lodash';
import { OpenAPIV3 } from 'openapi-types';
import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { ExcludeReferences, InternalExtension } from '../types';
import ApiFilterContext from './ApiFilterContext';

interface Props {
  content?: ExcludeReferences<OpenAPIV3.ResponseObject>['content'];
}

export default function ApiRequestBodyParameters({ content }: Readonly<Props>) {
  const [openParameters, setOpenParameters] = React.useState<string[]>([]);
  const { showInternal } = useContext(ApiFilterContext);

  const toggleParameter = (parameter: string) => {
    if (openParameters.includes(parameter)) {
      setOpenParameters(openParameters.filter((n) => n !== parameter));
    } else {
      setOpenParameters([...openParameters, parameter]);
    }
  };

  const schema =
    content &&
    (content['application/json']?.schema || content['application/merge-patch+json']?.schema);

  if (!schema?.properties || schema?.type !== 'object' || isEmpty(schema?.properties)) {
    return null;
  }

  const parameters = schema.properties;
  const required = schema.required ?? [];

  const orderedKeys = Object.keys(parameters).sort((a, b) => {
    if (required?.includes(a) && !required?.includes(b)) {
      return -1;
    }
    if (!required?.includes(a) && required?.includes(b)) {
      return 1;
    }
    return 0;
  });

  return (
    <ul aria-labelledby="api_documentation.v2.request_subheader.request_body">
      {orderedKeys
        .filter((key) => showInternal || !(parameters[key] as InternalExtension)['x-internal'])
        .map((key) => {
          return (
            <Accordion
              className="sw-mt-2 sw-mb-4"
              key={key}
              header={
                <div>
                  {key}{' '}
                  {schema.required?.includes(key) && (
                    <Badge className="sw-ml-2">{translate('required')}</Badge>
                  )}
                  {parameters[key].deprecated && (
                    <Badge variant="deleted" className="sw-ml-2">
                      {translate('deprecated')}
                    </Badge>
                  )}
                  {parameters[key].deprecated && (
                    <Badge variant="deleted" className="sw-ml-2">
                      {translate('deprecated')}
                    </Badge>
                  )}
                  {(parameters[key] as InternalExtension)['x-internal'] && (
                    <Badge variant="new" className="sw-ml-2">
                      {translate('internal')}
                    </Badge>
                  )}
                </div>
              }
              data={key}
              onClick={() => toggleParameter(key)}
              open={openParameters.includes(key)}
            >
              <div>{parameters[key].description}</div>
              {parameters[key].enum && (
                <div className="sw-mt-2">
                  <FormattedMessage
                    id="api_documentation.v2.enum_description"
                    values={{
                      values: <i>{parameters[key].enum?.join(', ')}</i>,
                    }}
                  />
                </div>
              )}
              {parameters[key].maxLength && (
                <TextMuted
                  className="sw-mt-2 sw-block"
                  text={`${translate('max')}: ${parameters[key].maxLength}`}
                />
              )}
              {typeof parameters[key].minLength === 'number' && (
                <TextMuted
                  className="sw-mt-2 sw-block"
                  text={`${translate('min')}: ${parameters[key].minLength}`}
                />
              )}
              {parameters[key].default !== undefined && (
                <TextMuted
                  className="sw-mt-2 sw-block"
                  text={`${translate('default')}: ${parameters[key].default}`}
                />
              )}
            </Accordion>
          );
        })}
    </ul>
  );
}
