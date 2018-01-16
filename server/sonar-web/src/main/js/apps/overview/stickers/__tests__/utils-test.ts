/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getStickerUrl, StickerType } from '../utils';

jest.mock('../../../../helpers/urls', () => ({
  getHostUrl: () => 'host'
}));

describe('#getStickerUrl', () => {
  it('it should generate correct marketing sticker links', () => {
    expect(getStickerUrl(StickerType.marketing, { color: 'white', metric: 'alert_status' })).toBe(
      'host/images/stickers/sonarcloud-white.svg'
    );
    expect(
      getStickerUrl(StickerType.marketing, {
        color: 'orange',
        component: 'foo',
        metric: 'alert_status'
      })
    ).toBe('host/images/stickers/sonarcloud-orange.svg');
  });

  it('it should generate correct quality gates sticker links', () => {
    expect(
      getStickerUrl(StickerType.measure, {
        color: 'white',
        component: 'foo',
        metric: 'alert_status'
      })
    ).toBe('host/api/stickers/measure?component=foo&metric=alert_status');
  });
});
