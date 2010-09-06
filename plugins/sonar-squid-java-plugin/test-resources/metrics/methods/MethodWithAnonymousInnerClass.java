public class MethodWithAnonymousInnerClass {

  public double doJob() {
        Job job = new Job(){
            public double doJob(){
              return 0;
            }
        };
        return job.doJob();
    }

  public double doAnotherJob() {
      Job job = new Job(){
          public double doJob(){
            return 0;
          }
      };
      return job.doJob();
  }
}