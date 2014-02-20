define(['navigator/filters/base-filters', 'navigator/filters/select-filters'], function (BaseFilters, SelectFilters) {

  var UNRESOLVED = 'UNRESOLVED';

  var ResolutionDetailFilterView = SelectFilters.DetailsSelectFilterView.extend({

    addToSelection: function(e) {
      var id = $j(e.target).val(),
          model = this.options.filterView.choices.findWhere({ id: id });

      if (this.model.get('multiple') && id !== UNRESOLVED) {
        this.options.filterView.selection.add(model);
        this.options.filterView.choices.remove(model);

        var unresolved = this.options.filterView.selection.findWhere({ id: UNRESOLVED });
        if (unresolved) {
          this.options.filterView.choices.add(unresolved);
          this.options.filterView.selection.remove(unresolved);
        }
      } else {
        this.options.filterView.choices.add(this.options.filterView.selection.models);
        this.options.filterView.choices.remove(model);
        this.options.filterView.selection.reset([model]);
      }

      this.updateValue();
      this.updateLists();
    }

  });


  return SelectFilters.SelectFilterView.extend({

    initialize: function() {
      SelectFilters.SelectFilterView.prototype.initialize.call(this, {
        detailsView: ResolutionDetailFilterView
      });

      var unresolved = this.choices.findWhere({ id: UNRESOLVED });
      unresolved.set('special', true);
    },


    restoreFromQuery: function(q) {
      var param = _.findWhere(q, { key: this.model.get('property') }),
          resolved = _.findWhere(q, { key: 'resolved' });

      if (!!resolved) {
        if (!param) {
          param = { value: UNRESOLVED };
        } else {
          param.value += ',' + UNRESOLVED;
        }
      }

      if (param && param.value) {
        this.model.set('enabled', true);
        this.restore(param.value, param);
      } else {
        this.clear();
      }
    },


    formatValue: function() {
      var q = {},
          resolutions = this.model.get('value');
      if (this.model.has('property') && resolutions) {
        var unresolved = resolutions.indexOf(UNRESOLVED) !== -1;

        if (unresolved) {
          q.resolved = false;
        } else {
          q[this.model.get('property')] = resolutions.join(',');
        }
      }
      return q;
    }

  });

});
