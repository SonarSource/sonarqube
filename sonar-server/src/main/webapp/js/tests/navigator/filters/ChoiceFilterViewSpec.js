define(['navigator/filters/base-filters', 'navigator/filters/choice-filters'], function(BaseFilters, ChoiceFilters) {

  describe('BaseFilterView', function() {
    var choices, choiceFilter, choiceFilterView;

    beforeEach(function() {
      choices = {
        'ONE': 'one',
        'TWO': 'two',
        'THREE': 'three',
        '!OPPOSITE': 'opposite'
      };

      choiceFilter = new BaseFilters.Filter({
        name: 'Choice Filter Name',
        property: 'choiceFilterProperty',
        type: ChoiceFilters.ChoiceFilterView,
        enabled: true,
        optional: false,
        choices: choices
      });

      choiceFilterView = new ChoiceFilters.ChoiceFilterView({
        model: choiceFilter
      });
    });

    it('creates choices', function() {
      expect(choiceFilterView.choices).toBeDefined();
      expect(choiceFilterView.choices.length).toBe(Object.keys(choices).length);
    });

    it('does not have selected by default', function() {
      expect(choiceFilterView.getSelected().length).toBe(0);
    });

  });

});
