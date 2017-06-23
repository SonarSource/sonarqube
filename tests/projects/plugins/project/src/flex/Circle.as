package {

  public class Circle {
    public var diameter:int = 0;

    function Circle (d:int) {
        if (d == 0) {
            this.diameter = -1;	
        } else {
            this.diameter = d;
        }
    }
  }
}
