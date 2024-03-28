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
import { mapValues } from 'lodash';
import { OpenAPIV3 } from 'openapi-types';
import { isDefined } from '../../helpers/types';
import { DereferenceRecursive, ExcludeReferences } from './types';

export const URL_DIVIDER = '--';

type ConvertedSchema = string | { [Key: string]: ConvertedSchema } | ConvertedSchema[];

export const mapOpenAPISchema = (
  schema: ExcludeReferences<OpenAPIV3.SchemaObject>,
): ConvertedSchema => {
  if (schema.type === 'object') {
    const result = { ...schema.properties };
    return mapValues(result, (schema) => mapOpenAPISchema(schema)) as ConvertedSchema;
  }
  if (schema.type === 'array') {
    return [mapOpenAPISchema(schema.items)];
  }
  if (schema.enum) {
    return `Enum (${schema.type}): ${(schema.enum as ConvertedSchema[]).join(', ')}`;
  }
  if (schema.format) {
    return `${schema.type} (${schema.format})`;
  }
  return schema.type as string;
};

export const dereferenceSchema = (
  document: OpenAPIV3.Document,
): ExcludeReferences<OpenAPIV3.Document> => {
  const dereference = (ref: string) => {
    const path = ref.replace('#/', '').split('/');
    return path.reduce((acc: any, key) => {
      if (key in acc) {
        return acc[key];
      }
      return {};
    }, document);
  };

  const dereferenceRecursive = <P>(val: P | OpenAPIV3.ReferenceObject): DereferenceRecursive<P> => {
    if (typeof val === 'object' && val !== null) {
      if ('$ref' in val) {
        return dereferenceRecursive(dereference(val.$ref));
      } else if (Array.isArray(val)) {
        return val.map(dereferenceRecursive) as DereferenceRecursive<P>;
      }
      return mapValues(val, dereferenceRecursive) as DereferenceRecursive<P>;
    }
    return val as DereferenceRecursive<P>;
  };

  return dereferenceRecursive(document) as ExcludeReferences<OpenAPIV3.Document>;
};

export const getResponseCodeClassName = (code: string): string => {
  switch (code[0]) {
    case '1':
      return 'sw-bg-blue-200';
    case '2':
      return 'sw-bg-green-200';
    case '3':
      return 'sw-bg-yellow-200';
    case '4':
    case '5':
      return 'sw-bg-red-200';
    default:
      return 'sw-bg-gray-200';
  }
};

export const getApiEndpointKey = (name: string, method: string) => `${name}${URL_DIVIDER}${method}`;

const DISPLAYED_MEDIA_TYPES = ['application/json', 'application/merge-patch+json'];

export function extractSchemaAndMediaType(
  content?: Exclude<ExcludeReferences<OpenAPIV3.ResponseObject>['content'], undefined>,
) {
  if (!content) {
    return [];
  }

  const requests = Object.keys(content)
    .filter((mediaType) => DISPLAYED_MEDIA_TYPES.includes(mediaType))
    .map((requestMediaType) => {
      const schema = content[requestMediaType]?.schema;

      if (!schema) {
        return null;
      }

      return {
        requestMediaType,
        schema: JSON.stringify(mapOpenAPISchema(schema), null, 2),
      };
    })
    .filter(isDefined);

  return requests;
}
