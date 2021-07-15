/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
const CLASS_SIDEBAR_PAGE = 'sidebar-page';
const CLASS_WHITE_PAGE = 'white-page';
const CLASS_NO_FOOTER_PAGE = 'no-footer-page';

export function addSideBarClass() {
  toggleBodyClass(CLASS_SIDEBAR_PAGE, true);
}

export function addWhitePageClass() {
  toggleBodyClass(CLASS_WHITE_PAGE, true);
}

export function addNoFooterPageClass() {
  /* eslint-disable-next-line no-console */
  console.warn('DEPRECATED: addNoFooterPageClass() was deprecated.');
  toggleBodyClass(CLASS_NO_FOOTER_PAGE, true);
}

export function removeSideBarClass() {
  toggleBodyClass(CLASS_SIDEBAR_PAGE, false);
}

export function removeWhitePageClass() {
  toggleBodyClass(CLASS_WHITE_PAGE, false);
}

export function removeNoFooterPageClass() {
  /* eslint-disable-next-line no-console */
  console.warn('DEPRECATED: removeNoFooterPageClass() was deprecated.');
  toggleBodyClass(CLASS_NO_FOOTER_PAGE, false);
}

function toggleBodyClass(className: string, force?: boolean) {
  document.body.classList.toggle(className, force);
  if (document.documentElement) {
    document.documentElement.classList.toggle(className, force);
  }
}
