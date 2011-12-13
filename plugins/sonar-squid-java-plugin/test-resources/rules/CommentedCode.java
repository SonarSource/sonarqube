/**
 * No detection of commented-out code in Javadoc for class
 * for (Visitor visitor : visitors) {
 *   continue;
 * }
 */
public class CommentedCode {

  /**
   * No detection of commented-out code in Javadoc for field
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  private int field;

  /**
   * No detection of commented-out code in Javadoc for constructor
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  public CommentedCode(int field) {
    this.field = field;
    // This is a comment, but next line is a commented-out code
    // for (Visitor visitor : visitors) {
    //   continue;
    // }

    /*
    This is a comment, but next line is a commented-out code
    for (Visitor visitor : visitors) {
      continue;
    }
    */

    /* This is a comment, but next line is a commented-out code */
    /* for (Visitor visitor : visitors) { */
    /*   continue; */
    /* } */

    /**
     * This is not Javadoc, even if it looks like Javadoc and before declaration of variable
     * for (Visitor visitor : visitors) {
     *   continue;
     * }
     */
    int a;
  }

  /**
   * No detection of commented-out code in Javadoc for method
   * for (Visitor visitor : visitors) {
   *   continue;
   * }
   */
  public int getField() {
    return field;
  }
}
