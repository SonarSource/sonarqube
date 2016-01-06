import { expect } from 'chai';
import { resetBundle, translate, translateWithParameters } from '../../src/main/js/helpers/l10n';

describe('l10n', () => {
  afterEach(() => {
    resetBundle({});
  });

  describe('#translate', () => {
    it('should translate simple message', () => {
      resetBundle({ 'my_key': 'my message' });
      expect(translate('my_key')).to.equal('my message');
    });

    it('should translate message with composite key', () => {
      resetBundle({ 'my.composite.message': 'my message' });
      expect(translate('my', 'composite', 'message')).to.equal('my message');
      expect(translate('my.composite', 'message')).to.equal('my message');
      expect(translate('my', 'composite.message')).to.equal('my message');
      expect(translate('my.composite.message')).to.equal('my message');
    });

    it('should not translate message but return its key', () => {
      expect(translate('random')).to.equal('random');
      expect(translate('random', 'key')).to.equal('random.key');
      expect(translate('composite.random', 'key')).to.equal('composite.random.key');
    });
  });

  describe('#translateWithParameters', () => {
    it('should translate message with one parameter in the beginning', () => {
      resetBundle({ 'x_apples': '{0} apples' });
      expect(translateWithParameters('x_apples', 5)).to.equal('5 apples');
    });

    it('should translate message with one parameter in the middle', () => {
      resetBundle({ 'x_apples': 'I have {0} apples' });
      expect(translateWithParameters('x_apples', 5)).to.equal('I have 5 apples');
    });

    it('should translate message with one parameter in the end', () => {
      resetBundle({ 'x_apples': 'Apples: {0}' });
      expect(translateWithParameters('x_apples', 5)).to.equal('Apples: 5');
    });

    it('should translate message with several parameters', () => {
      resetBundle({ 'x_apples': '{0}: I have {2} apples in my {1} baskets - {3}' });
      expect(translateWithParameters('x_apples', 1, 2, 3, 4)).to.equal('1: I have 3 apples in my 2 baskets - 4');
    });

    it('should not translate message but return its key', () => {
      expect(translateWithParameters('random', 5)).to.equal('random.5');
      expect(translateWithParameters('random', 1, 2, 3)).to.equal('random.1.2.3');
      expect(translateWithParameters('composite.random', 1, 2)).to.equal('composite.random.1.2');
    });
  });
});
