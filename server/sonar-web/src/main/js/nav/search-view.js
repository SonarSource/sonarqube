define([
  'common/selectable-collection-view',
  'templates/nav'
], function (SelectableCollectionView) {

  var $ = jQuery,

      SearchItemView = Marionette.ItemView.extend({
        tagName: 'li',
        template: Templates['nav-search-item'],

        select: function () {
          this.$el.addClass('active');
        },

        deselect: function () {
          this.$el.removeClass('active');
        },

        submit: function () {
          this.$('a')[0].click();
        },

        serializeData: function () {
          return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
            index: this.options.index
          });
        }
      }),

      SearchEmptyView = Marionette.ItemView.extend({
        tagName: 'li',
        template: Templates['nav-search-empty']
      }),

      SearchResultsView = SelectableCollectionView.extend({
        className: 'menu',
        tagName: 'ul',
        itemView: SearchItemView,
        emptyView: SearchEmptyView
      });

  return Marionette.Layout.extend({
    className: 'navbar-search',
    tagName: 'form',
    template: Templates['nav-search'],

    regions: {
      resultsRegion: '.js-search-results'
    },

    events: {
      'submit': 'onSubmit',
      'keydown .js-search-input': 'onKeyDown',
      'keyup .js-search-input': 'debouncedOnKeyUp'
    },

    initialize: function () {
      this.results = new Backbone.Collection();
      this.resetResultsToDefault();
      this.resultsView = new SearchResultsView({ collection: this.results });
      this.debouncedOnKeyUp = _.debounce(this.onKeyUp, 400);
      this._bufferedValue = '';
    },

    onRender: function () {
      var that = this;
      this.resultsRegion.show(this.resultsView);
      setTimeout(function () {
        that.$('.js-search-input').focus();
      }, 0);
    },

    onKeyDown: function (e) {
      if (e.keyCode === 38) {
        this.resultsView.selectPrev();
        return false;
      }
      if (e.keyCode === 40) {
        this.resultsView.selectNext();
        return false;
      }
      if (e.keyCode === 13) {
        this.resultsView.submitCurrent();
        return false;
      }
      if (e.keyCode === 27) {
        this.options.hide();
        return false;
      }
    },

    onKeyUp: function () {
      var value = this.$('.js-search-input').val();
      if (value === this._bufferedValue) {
        return;
      }
      this._bufferedValue = this.$('.js-search-input').val();
      this.search(value);
    },

    onSubmit: function () {
      return false;
    },

    resetResultsToDefault: function () {
      var recentHistory = JSON.parse(localStorage.getItem('sonar_recent_history')),
          history = (recentHistory || []).map(function (historyItem, index) {
            return {
              url: baseUrl + '/dashboard/index?id=' + encodeURIComponent(historyItem.key) + dashboardParameters(true),
              name: historyItem.name,
              q: historyItem.icon,
              extra: index === 0 ? t('recent_history') : null
            };
          }),
          qualifiers = this.model.get('qualifiers').map(function (q, index) {
            return {
              url: baseUrl + '/all_projects?qualifier=' + encodeURIComponent(q),
              name: t('qualifiers.all', q),
              extra: index === 0 ? '' : null
            };
          });
      this.results.reset(history.concat(qualifiers));
    },

    search: function (q) {
      if (q.length < 2) {
        this.resetResultsToDefault();
        return;
      }
      var that = this,
          url = baseUrl + '/api/components/suggestions',
          options = { s: q },
          p = window.process.addBackgroundProcess();
      return $.get(url, options).done(function (r) {
        var collection = [];
        r.results.forEach(function (domain) {
          domain.items.forEach(function (item, index) {
            collection.push(_.extend(item, {
              q: domain.q,
              extra: index === 0 ? domain.name : null,
              url: baseUrl + '/dashboard/index?id=' + encodeURIComponent(item.key) + dashboardParameters(true)
            }));
          });
        });
        that.results.reset(collection);
        window.process.finishBackgroundProcess(p);
      }).fail(function() {
        window.process.failBackgroundProcess(p);
      });
    }
  });

});
