/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { decodeJwt, latinize, slugify } from '../strings';

describe('#decodeJwt', () => {
  it('should correctly decode a jwt token', () => {
    const claims = {
      aud: 'ari:cloud:bitbucket::app/{327713ed-f1b2-4659-9c91-c8ecf8be7f3e}/sonarcloud-greg',
      exp: 1541062205,
      iat: 1541058605,
      iss: 'ari:cloud:bitbucket::app/{327713ed-f1b2-4659-9c91-c8ecf8be7f3e}/sonarcloud-greg',
      qsh: 'a6c93addd971c05d08da1e1669c2640fba529e98fbb5b2b9effadf00bf484277'
    };
    expect(
      decodeJwt(
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcmk6Y2xvdWQ6Yml0YnVja2V0OjphcHAvezMyNzcxM2VkLWYxYjItNDY1OS05YzkxLWM4ZWNmOGJlN2YzZX0vc29uYXJjbG91ZC1ncmVnIiwiaWF0IjoxNTQxMDU4NjA1LCJxc2giOiJhNmM5M2FkZGQ5NzFjMDVkMDhkYTFlMTY2OWMyNjQwZmJhNTI5ZTk4ZmJiNWIyYjllZmZhZGYwMGJmNDg0Mjc3IiwiYXVkIjoiYXJpOmNsb3VkOmJpdGJ1Y2tldDo6YXBwL3szMjc3MTNlZC1mMWIyLTQ2NTktOWM5MS1jOGVjZjhiZTdmM2V9L3NvbmFyY2xvdWQtZ3JlZyIsImV4cCI6MTU0MTA2MjIwNX0.5_0dFh_TPT_UorDewu2JEErgQE2ZnzBjvCDrOThseRo'
      )
    ).toEqual(claims);
    expect(
      decodeJwt(
        'eyJpc3MiOiJhcmk6Y2xvdWQ6Yml0YnVja2V0OjphcHAvezMyNzcxM2VkLWYxYjItNDY1OS05YzkxLWM4ZWNmOGJlN2YzZX0vc29uYXJjbG91ZC1ncmVnIiwiaWF0IjoxNTQxMDU4NjA1LCJxc2giOiJhNmM5M2FkZGQ5NzFjMDVkMDhkYTFlMTY2OWMyNjQwZmJhNTI5ZTk4ZmJiNWIyYjllZmZhZGYwMGJmNDg0Mjc3IiwiYXVkIjoiYXJpOmNsb3VkOmJpdGJ1Y2tldDo6YXBwL3szMjc3MTNlZC1mMWIyLTQ2NTktOWM5MS1jOGVjZjhiZTdmM2V9L3NvbmFyY2xvdWQtZ3JlZyIsImV4cCI6MTU0MTA2MjIwNX0'
      )
    ).toEqual(claims);
  });
});

describe('#latinize', () => {
  it('should remove diacritics and replace them with normal letters', () => {
    expect(latinize('âêîôûŵŷäëïöüẅÿàèìòùẁỳáéíóúẃýøāēīūčģķļņšž')).toBe(
      'aeiouwyaeiouwyaeiouwyaeiouwyoaeiucgklnsz'
    );
    expect(latinize('ASDFGhjklQWERTz')).toBe('ASDFGhjklQWERTz');
  });
});

describe('#slugify', () => {
  it('should transform text into a slug', () => {
    expect(slugify('Luke Sky&Walker')).toBe('luke-sky-and-walker');
    expect(slugify('tèst_:-ng><@')).toBe('test-ng');
    expect(slugify('my-valid-slug-1')).toBe('my-valid-slug-1');
  });
});
