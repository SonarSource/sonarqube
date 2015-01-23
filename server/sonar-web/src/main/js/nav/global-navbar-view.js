define([
  'nav/search-view',
  'templates/nav'
], function (SearchView) {

  var $ = jQuery;

  return Marionette.Layout.extend({
    template: Templates['nav-global-navbar'],

    regions: {
      searchRegion: '.js-search-region'
    },

    events: {
      'click .js-login': 'onLoginClick',
      'click .js-favorite': 'onFavoriteClick',
      'show.bs.dropdown .js-search-dropdown': 'onSearchDropdownShow',
      'hidden.bs.dropdown .js-search-dropdown': 'onSearchDropdownHidden'
    },

    onRender: function () {
      var that = this;
      this.$el.addClass('navbar-' + window.navbarSpace);
      this.$el.addClass('navbar-fade');
      setTimeout(function () {
        that.$el.addClass('in');
      }, 0);
    },

    onLoginClick: function () {
      var returnTo = window.location.pathname + window.location.search;
      window.location = baseUrl + '/sessions/new?return_to=' + encodeURIComponent(returnTo) + window.location.hash;
      return false;
    },

    onFavoriteClick: function () {
      var that = this,
          p = window.process.addBackgroundProcess(),
          url = baseUrl + '/favourites/toggle/' + window.navbarProjectId;
      return $.post(url).done(function () {
        that.isProjectFavorite = !that.isProjectFavorite;
        that.render();
        window.process.finishBackgroundProcess(p);
      }).fail(function () {
        window.process.failBackgroundProcess(p);
      });
    },

    onSearchDropdownShow: function () {
      var that = this;
      this.searchRegion.show(new SearchView({
        model: this.model,
        hide: function () {
          that.$('.js-search-dropdown-toggle').dropdown('toggle');
        }
      }));
    },

    onSearchDropdownHidden: function () {
      this.searchRegion.reset();
    },

    serializeData: function () {
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        user: window.SS.user,
        userName: window.SS.userName,
        isUserAdmin: window.SS.isUserAdmin,

        canManageGlobalDashboards: window.SS.user != null,
        canManageIssueFilters: window.SS.user != null,
        canManageMeasureFilters: window.SS.user != null
      });
    }
  });

});
