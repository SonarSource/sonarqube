(function() {
  var $;

  $ = jQuery;

  describe('translation "t" suite', function() {
    beforeEach(function() {
      window.messages = {
        'something': 'SOMETHING',
        'something_with_underscore': 'SOMETHING_WITH_UNDERSCORE',
        'something_with{braces}': 'SOMETHING_WITH{braces}'
      };
      return window.SS = {
        phrases: {
          'something': 'SOMETHING ANOTHER'
        }
      };
    });
    afterEach(function() {
      return window.messages = window.SS = void 0;
    });
    it('translates', function() {
      return expect(t('something')).toBe('SOMETHING');
    });
    it('translates with underscore', function() {
      return expect(t('something_with_underscore')).toBe('SOMETHING_WITH_UNDERSCORE');
    });
    it('translates with braces', function() {
      return expect(t('something_with{braces}')).toBe('SOMETHING_WITH{braces}');
    });
    it('fallbacks to "translate"', function() {
      window.messages = void 0;
      return expect(t('something')).toBe('SOMETHING ANOTHER');
    });
    return it('returns the key when no translation', function() {
      return expect(t('something_another')).toBe('something_another');
    });
  });

  describe('translation "translate" suite', function() {
    beforeEach(function() {
      return window.SS = {
        phrases: {
          'something': 'SOMETHING',
          'something_with_underscore': 'SOMETHING_WITH_UNDERSCORE',
          'something_with{braces}': 'SOMETHING_WITH{braces}'
        }
      };
    });
    afterEach(function() {
      return window.messages = window.SS = void 0;
    });
    it('translates', function() {
      return expect(translate('something')).toBe('SOMETHING');
    });
    it('translates with underscore', function() {
      return expect(translate('something_with_underscore')).toBe('SOMETHING_WITH_UNDERSCORE');
    });
    it('translates with braces', function() {
      return expect(translate('something_with{braces}')).toBe('SOMETHING_WITH{braces}');
    });
    it('returns the key when no translation', function() {
      return expect(translate('something_another')).toBe('something_another');
    });
    return it('does not fail when there is no dictionary', function() {
      window.SS = void 0;
      return expect(translate('something_another')).toBe('something_another');
    });
  });

}).call(this);
