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
import $ from 'jquery';

$.fn.isolatedScroll = function() {
  this.on('wheel', function(event) {
    const delta = -event.originalEvent.deltaY;
    const bottomOverflow = this.scrollTop + $(this).outerHeight() - this.scrollHeight >= 0;
    const topOverflow = this.scrollTop <= 0;
    if ((delta < 0 && bottomOverflow) || (delta > 0 && topOverflow)) {
      event.preventDefault();
    }
  });
};
