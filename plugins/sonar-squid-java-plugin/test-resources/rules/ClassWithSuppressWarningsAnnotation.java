@SuppressWarnings("all")
class ClassWithSuppressWarningsAnnotation {

    @java.lang.SuppressWarnings("all")
    public void fullyQualifiedName() {
    }

    @SuppressWarnings("all")
    public void singleValue() {
    }

    @SuppressWarnings(value = { "all" })
    public void arrayWithSingleValue() {
    }

    @SuppressWarnings(value = { "null", "all" })
    public void arrayWithMultipleValues() {
    }

    public void doJob() {
      Object o = new Object() {
        @SuppressWarnings("all")
        public void methodInAnonymousInnerClass() {
        }
      };
    }

    // Currently Sonar is unable to properly handle following cases

    @SuppressWarnings("a" + "ll")
    public void notHandled() {
    }

    @SuppressWarnings(false ? "null" : "all")
    public void notHandled2() {
    }

    private static final String SUPPRESS = "all";
    @SuppressWarnings(SUPPRESS)
    public void notHandled3() {
    }
}
