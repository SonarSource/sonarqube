TestCase('com.company.CarTest', {

	testfullName : function() {
		var car = new Car('VW', 'Beatle', 1971);
		assertEquals('VW Beatle Y: 1971', car.getFullName());
	},

	testStopEngineWithCheck : function() {
		var car = new Car('VW', 'Beatle', 1971);
		assertEquals('engine was not running', car.stopEngineWithCheck());
	},

	testCalculatePrice : function() {
		var car = new Car('Volvo', 'XC70', 2012);
		assertEquals('$30000', car.calculatePrice());
	}

});