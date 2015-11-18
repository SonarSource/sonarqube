import { expect } from 'chai';

import { getPeriodDate, getPeriodLabel } from '../../../../src/main/js/apps/overview/helpers/periods';


const PERIOD = {
  date: '2015-09-09T00:00:00+0200',
  index: '1',
  mode: 'previous_version',
  modeParam: '1.7'
};

const PERIOD_WITHOUT_VERSION = {
  date: '2015-09-09T00:00:00+0200',
  index: '1',
  mode: 'previous_version',
  modeParam: ''
};

const PERIOD_WITHOUT_DATE = {
  date: '',
  index: '1',
  mode: 'previous_version',
  modeParam: ''
};


describe('Overview Helpers', function () {
  describe('Periods', function () {

    describe('#getPeriodDate', function () {
      it('should return date', function () {
        let result = getPeriodDate([PERIOD], PERIOD.index);
        expect(result.getFullYear()).to.equal(2015);
      });

      it('should return null when can not find period', function () {
        let result = getPeriodDate([], '1');
        expect(result).to.be.null;
      });

      it('should return null when date is empty', function () {
        let result = getPeriodDate([PERIOD_WITHOUT_DATE], '1');
        expect(result).to.be.null;
      });
    });


    describe('#getPeriodLabel', function () {
      it('should return label', function () {
        let result = getPeriodLabel([PERIOD], PERIOD.index);
        expect(result).to.equal('overview.period.previous_version.1.7');
      });

      it('should return "since previous version"', function () {
        let result = getPeriodLabel([PERIOD_WITHOUT_VERSION], PERIOD_WITHOUT_VERSION.index);
        expect(result).to.equal('overview.period.previous_version_only_date');
      });

      it('should return null', function () {
        let result = getPeriodLabel([], '1');
        expect(result).to.be.null;
      });
    });

  });
});
