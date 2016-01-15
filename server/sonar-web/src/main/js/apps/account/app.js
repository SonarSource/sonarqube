/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import Backbone from 'backbone';
import ChangePasswordView from './change-password-view';
import TokensView from './tokens-view';
import avatarHelper from '../../helpers/handlebars/avatarHelper';

var shouldShowAvatars = window.SS && window.SS.lf && window.SS.lf.enableGravatar;
var favorites = $('.js-account-favorites tr');

function showExtraFavorites () {
  favorites.removeClass('hidden');
}

class App {
  start () {
    $('html').addClass('dashboard-page');

    if (shouldShowAvatars) {
      var avatarHtml = avatarHelper(window.SS.userEmail, 100).string;
      $('.js-avatar').html(avatarHtml);
    }

    $('.js-show-all-favorites').on('click', function (e) {
      e.preventDefault();
      $(e.currentTarget).hide();
      showExtraFavorites();
    });

    $('#account-change-password-trigger').on('click', function (e) {
      e.preventDefault();
      new ChangePasswordView().render();
    });

    const account = new Backbone.Model({
      id: window.SS.user
    });

    new TokensView({
      el: '#account-tokens',
      model: account
    }).render();
  }
}

window.sonarqube.appStarted.then(options => new App().start(options));
