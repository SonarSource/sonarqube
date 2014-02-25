define(
    [
      'backbone.marionette',
      'navigator/filters/base-filters',
      'navigator/filters/more-criteria-filters',
      'navigator/filters/favorite-filters'
    ],
    function (Marionette, BaseFilters) {

      return Marionette.CompositeView.extend({
        template: '#filterBarTemplate',
        itemViewContainer: '.navigator-filters-list',


        collectionEvents: {
          'change:enabled': 'changeEnabled'
        },


        getItemView: function (item) {
          return item.get('type') || BaseFilters.BaseFilterView;
        },


        itemViewOptions: function () {
          return {
            filterBarView: this,
            app: this.options.app
          };
        },


        initialize: function () {
          Marionette.CompositeView.prototype.initialize.apply(this, arguments);

          var that = this;
          $j('body').on('click', function () {
            that.hideDetails();
          });
          this.addMoreCriteriaFilter();
        },


        addMoreCriteriaFilter: function() {
          var disabledFilters = this.collection.where({ enabled: false });
          this.moreCriteriaFilter = new BaseFilters.Filter({
            type: require('navigator/filters/more-criteria-filters').MoreCriteriaFilterView,
            enabled: true,
            optional: false,
            filters: disabledFilters
          });
          this.collection.add(this.moreCriteriaFilter);
        },


        onAfterItemAdded: function (itemView) {
          if (itemView.model.get('type') === require('navigator/filters/favorite-filters').FavoriteFilterView) {
            this.$el.addClass('navigator-filter-list-favorite');
          }
        },


        restoreFromQuery: function (q) {
          this.collection.each(function (item) {
            item.set('enabled', !item.get('optional'));
            item.view.clear();
            item.view.restoreFromQuery(q);
          });
        },


        hideDetails: function () {
          if (_.isObject(this.showedView)) {
            this.showedView.hideDetails();
          }
        },


        enableFilter: function (id) {
          var filter = this.collection.get(id),
              filterView = filter.view;

          filterView.$el.detach().insertBefore(this.$('.navigator-filter-more-criteria'));
          filter.set('enabled', true);
          filterView.showDetails();
        },


        changeEnabled: function () {
          var disabledFilters = this.collection
              .where({ enabled: false })
              .reject(function (filter) {
                return filter.get('type') === require('navigator/filters/more-criteria-filters').MoreCriteriaFilterView;
              });

          if (disabledFilters.length === 0) {
            this.moreCriteriaFilter.set({ enabled: false }, { silent: true });
          } else {
            this.moreCriteriaFilter.set({ enabled: true }, { silent: true });
          }
          this.moreCriteriaFilter.set('filters', disabledFilters);
        }

      });

    });
