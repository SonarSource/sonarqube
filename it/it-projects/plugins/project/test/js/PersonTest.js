TestCase('PersonTest', {

    testWhoAreYou : function() {
        var p = new Person('John', 'Doe', 'P.');
        assertEquals('Should have responded with full name', 'John P. Doe', p.whoAreYou());
    },

    testWhoAreYouWithNoMiddleName : function() {
        var p = new Person('John', 'Doe');
        assertEquals('Should have used only first and last name', 'John Doe', p.whoAreYou());
    }

});