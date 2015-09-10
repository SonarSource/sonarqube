define([
  './change-password-view'
], function (ChangePasswordView) {

  var $ = jQuery;
  var shouldShowAvatars = window.SS && window.SS.lf && window.SS.lf.enableGravatar;
  var favorites = $('.js-account-favorites tr');

  function showExtraFavorites () {
    favorites.removeClass('hidden');
  }

  return {
    start: function () {
      $('html').addClass('dashboard-page');

      if (shouldShowAvatars) {
        var avatarHtml = Handlebars.helpers.avatarHelper(window.SS.userEmail, 100).string;
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
    }
  };

});
