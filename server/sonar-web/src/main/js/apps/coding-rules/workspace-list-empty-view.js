define(function () {

  return Marionette.ItemView.extend({
    className: 'search-navigator-no-results',

    template: function () {
      return t('coding_rules.no_results');
    }
  });

});
