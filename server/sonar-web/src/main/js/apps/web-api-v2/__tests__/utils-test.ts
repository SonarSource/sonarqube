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
import { dereferenceSchema, extractSchemaAndMediaType, mapOpenAPISchema } from '../utils';

describe('dereferenceSchema', () => {
  it('should dereference schema', () => {
    expect(
      dereferenceSchema({
        openapi: '3.0.1',
        info: {
          title: 'SonarQube Web API',
          version: '1.0.0 beta',
        },
        paths: {
          '/test': {
            delete: {
              responses: {
                '200': {
                  description: 'Internal Server Error',
                  content: {
                    'application/json': {
                      schema: {
                        $ref: '#/components/schemas/Test',
                      },
                    },
                  },
                },
              },
            },
          },
          '/test/{first}': {
            get: {
              parameters: [
                {
                  name: 'first',
                  in: 'path',
                  description: '1',
                  schema: {
                    $ref: '#/components/schemas/NestedTest',
                  },
                },
                {
                  name: 'second',
                  in: 'query',
                  description: '2',
                  schema: {
                    type: 'string',
                  },
                },
              ],
              responses: {
                '200': {
                  description: 'Internal Server Error',
                  content: {
                    'application/json': {
                      schema: {
                        $ref: '#/components/schemas/NestedTest',
                      },
                    },
                  },
                },
              },
            },
          },
        },
        components: {
          schemas: {
            NestedTest: {
              type: 'object',
              properties: {
                test: {
                  type: 'array',
                  items: {
                    $ref: '#/components/schemas/Test',
                  },
                },
              },
            },
            Test: {
              type: 'string',
            },
          },
        },
      }),
    ).toStrictEqual({
      openapi: '3.0.1',
      info: {
        title: 'SonarQube Web API',
        version: '1.0.0 beta',
      },
      paths: {
        '/test': {
          delete: {
            responses: {
              '200': {
                description: 'Internal Server Error',
                content: {
                  'application/json': {
                    schema: {
                      type: 'string',
                    },
                  },
                },
              },
            },
          },
        },
        '/test/{first}': {
          get: {
            parameters: [
              {
                name: 'first',
                in: 'path',
                description: '1',
                schema: {
                  type: 'object',
                  properties: {
                    test: {
                      type: 'array',
                      items: {
                        type: 'string',
                      },
                    },
                  },
                },
              },
              {
                name: 'second',
                in: 'query',
                description: '2',
                schema: {
                  type: 'string',
                },
              },
            ],
            responses: {
              '200': {
                description: 'Internal Server Error',
                content: {
                  'application/json': {
                    schema: {
                      type: 'object',
                      properties: {
                        test: {
                          type: 'array',
                          items: {
                            type: 'string',
                          },
                        },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      },
      components: {
        schemas: {
          NestedTest: {
            type: 'object',
            properties: {
              test: {
                type: 'array',
                items: {
                  type: 'string',
                },
              },
            },
          },
          Test: {
            type: 'string',
          },
        },
      },
    });
  });
});

describe('mapOpenAPISchema', () => {
  it('should map open api response schema', () => {
    expect(
      mapOpenAPISchema({
        type: 'object',
        properties: {
          str: {
            type: 'string',
          },
          int: {
            type: 'integer',
            format: 'int32',
          },
          num: {
            type: 'number',
            format: 'double',
          },
          bool: {
            type: 'boolean',
          },
        },
      }),
    ).toStrictEqual({
      str: 'string',
      int: 'integer (int32)',
      num: 'number (double)',
      bool: 'boolean',
    });

    expect(
      mapOpenAPISchema({
        type: 'array',
        items: {
          type: 'string',
        },
      }),
    ).toStrictEqual(['string']);

    expect(
      mapOpenAPISchema({
        type: 'string',
        enum: ['GREEN', 'YELLOW', 'RED'],
      }),
    ).toStrictEqual('Enum (string): GREEN, YELLOW, RED');
  });
});

describe('extractSchemaAndMediaType', () => {
  it('should extract the schema', () => {
    const result = extractSchemaAndMediaType({
      'application/json': {
        schema: {
          type: 'object',
          properties: {
            name: { type: 'string', description: 'username' },
            age: { type: 'number', description: 'age', minimum: 0, maximum: 130 },
          },
        },
      },
      'application/merge-patch+json': {
        schema: {
          type: 'object',
          properties: {
            name: { type: 'string', description: 'username' },
            age: { type: 'number', description: 'age', minimum: 0, maximum: 130 },
          },
        },
      },
    });

    expect(result).toHaveLength(2);
  });

  it('should handle missing schema', () => {
    const result = extractSchemaAndMediaType({
      'application/json': {},
      'application/merge-patch+json': {},
    });

    expect(result).toHaveLength(0);
  });

  it('should handle no content', () => {
    const result = extractSchemaAndMediaType();

    expect(result).toHaveLength(0);
  });
});
