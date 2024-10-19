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
import { Accordion, SubTitle } from 'design-system';
import { OpenAPIV3 } from 'openapi-types';
import React from 'react';
import { translate } from '../../../helpers/l10n';
import { ExcludeReferences } from '../types';
import ApiResponseSchema from './ApiResponseSchema';
import ApiResponseTitle from './ApiResponseTitle';

interface Props {
  responses?: ExcludeReferences<OpenAPIV3.ResponsesObject>;
}

export default function ApiResponses({ responses }: Props) {
  const [openedResponses, setOpenedResponses] = React.useState<string[]>([
    Object.keys(responses ?? {})[0],
  ]);

  const toggleParameter = (name: string) => {
    if (openedResponses.includes(name)) {
      setOpenedResponses(openedResponses.filter((n) => n !== name));
    } else {
      setOpenedResponses([...openedResponses, name]);
    }
  };

  const titleId = 'api-responses';

  return (
    <>
      <SubTitle id={titleId}>{translate('api_documentation.v2.response_header')}</SubTitle>
      {!responses && <div>{translate('no_data')}</div>}
      {responses && (
        <ul aria-labelledby={titleId}>
          {Object.entries(responses).map(([code, response]) => (
            <Accordion
              className="sw-mt-2"
              key={code}
              header={<ApiResponseTitle code={code} codeDescription={response.description} />}
              data={code}
              onClick={toggleParameter}
              open={openedResponses.includes(code)}
            >
              <ApiResponseSchema content={response.content} />
            </Accordion>
          ))}
        </ul>
      )}
    </>
  );
}
