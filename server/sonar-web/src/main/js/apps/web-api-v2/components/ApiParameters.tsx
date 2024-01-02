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
import { Accordion, Badge, SubHeading, SubTitle, TextMuted } from 'design-system';
import { groupBy } from 'lodash';
import { OpenAPIV3 } from 'openapi-types';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { ExcludeReferences } from '../types';
import { mapOpenAPISchema } from '../utils';
import ApiRequestBodyParameters from './ApiRequestParameters';
import ApiResponseSchema from './ApiResponseSchema';

interface Props {
  data: ExcludeReferences<OpenAPIV3.OperationObject>;
}

export default function ApiParameters({ data }: Props) {
  const [openParameters, setOpenParameters] = React.useState<string[]>([]);

  const toggleParameter = (name: string) => {
    if (openParameters.includes(name)) {
      setOpenParameters(openParameters.filter((n) => n !== name));
    } else {
      setOpenParameters([...openParameters, name]);
    }
  };

  const getSchemaType = (schema: ExcludeReferences<OpenAPIV3.SchemaObject>) => {
    const mappedSchema = mapOpenAPISchema(schema);

    return typeof mappedSchema === 'object' ? JSON.stringify(mappedSchema) : mappedSchema;
  };

  const requestBody = data.requestBody?.content;

  return (
    <>
      <SubTitle>{translate('api_documentation.v2.parameter_header')}</SubTitle>
      {Object.entries(groupBy(data.parameters, (p) => p.in)).map(([group, parameters]) => (
        <div key={group}>
          <SubHeading id={`api-parameters-${group}`}>
            {translate(`api_documentation.v2.request_subheader.${group}`)}
          </SubHeading>
          <ul aria-labelledby={`api-parameters-${group}`}>
            {parameters.map((parameter) => {
              return (
                <Accordion
                  className="sw-mt-2 sw-mb-4"
                  key={parameter.name}
                  header={
                    <div>
                      {parameter.name}{' '}
                      {parameter.schema && (
                        <TextMuted
                          className="sw-inline sw-ml-2"
                          text={getSchemaType(parameter.schema)}
                        />
                      )}
                      {parameter.required && (
                        <Badge className="sw-ml-2">{translate('required')}</Badge>
                      )}
                      {parameter.deprecated && (
                        <Badge variant="deleted" className="sw-ml-2">
                          {translate('deprecated')}
                        </Badge>
                      )}
                    </div>
                  }
                  data={parameter.name}
                  onClick={toggleParameter}
                  open={openParameters.includes(parameter.name)}
                >
                  <div>{parameter.description}</div>
                  {parameter.schema?.enum && (
                    <div className="sw-mt-2">
                      <FormattedMessage
                        id="api_documentation.v2.enum_description"
                        values={{
                          values: (
                            <div className="sw-body-sm-highlight">
                              {parameter.schema.enum.join(', ')}
                            </div>
                          ),
                        }}
                      />
                    </div>
                  )}
                  {parameter.schema?.maximum && (
                    <TextMuted
                      className="sw-mt-2 sw-block"
                      text={`${translate('max')}: ${parameter.schema?.maximum}`}
                    />
                  )}
                  {typeof parameter.schema?.minimum === 'number' && (
                    <TextMuted
                      className="sw-mt-2 sw-block"
                      text={`${translate('min')}: ${parameter.schema?.minimum}`}
                    />
                  )}
                  {parameter.example !== undefined && (
                    <TextMuted
                      className="sw-mt-2 sw-block"
                      text={`${translate('example')}: ${parameter.example}`}
                    />
                  )}
                  {parameter.schema?.default !== undefined && (
                    <TextMuted
                      className="sw-mt-2 sw-block"
                      text={`${translate('default')}: ${parameter.schema?.default}`}
                    />
                  )}
                </Accordion>
              );
            })}
          </ul>
        </div>
      ))}
      {!requestBody && !data.parameters?.length && <TextMuted text={translate('no_data')} />}
      {requestBody && (
        <div>
          <SubHeading id="api_documentation.v2.request_subheader.request_body">
            {translate('api_documentation.v2.request_subheader.request_body')}
          </SubHeading>
          <ApiResponseSchema content={requestBody} />
          <ApiRequestBodyParameters content={requestBody} />
        </div>
      )}
    </>
  );
}
