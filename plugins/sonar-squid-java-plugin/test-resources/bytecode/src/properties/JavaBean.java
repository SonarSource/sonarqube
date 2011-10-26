package properties;

import java.util.ArrayList;

public class JavaBean {
  
  private String name;
  private boolean french;
  ArrayList<String> firstNames = new ArrayList<String>();
  private static String staticMember;
  private String FirstName;
  private int myIncrement = 1;
  private int myDifferentIncrement = 2;
  
  public String getName() {
    return name;
  }
  
  public String getNameIndirect() {
    return getName();
  }
  
  public String getNameOrEmpty() {
    return (getName() == null) ? "" : name;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  public boolean isFrench(){
    return french;
  }
  
  public void setFrench(boolean french){
    this.french = french;
  }
  
  public void anotherMethod(){
    
  }
  
  public void addFirstName(String firstName) {
    firstNames.add(firstName);
  }
  
  public String getNameOrDefault() {
    return (name == null) ? "Freddy" : name;
  }
  
  public static void uselessStaticMethod() {
    
  }
  
  public void accessorWithABunchOfCalls() {
    uselessStaticMethod();
    ArrayList<String> myList = new ArrayList<String>();
    myList.add("Banana");
    myList.add("Peach");
    myList.add("Strawberry");
    
    firstNames.addAll(myList);
  }
  
  public void dumpStuff() {
    System.out.println("Stuff 1");
    System.out.println("Stuff 2");
    System.out.println("Stuff 3");
  }
  
  public void accessNameAndDumpStuffSoNotAccessor() {
    this.name = "BusinessEnabler";
    dumpStuff();
  }
  
  public void iShouldBeAStaticSetter() {
    staticMember = "Hello!";
  }
  
  public String getFirstName() {
    return FirstName;
  }
  
  public String getFirstNameAndOneArgument(String argument) {
    return FirstName + " " + argument;
  }
  
  public int recursiveAbs(int value) {
    if (value < 0) {
      return recursiveAbs(value + myIncrement);
    } else return value;
  }
  
  public int recursiveAbsNotAccessor(int value) {
    if (value < 0) {
      return recursiveAbs(value + myIncrement);
    } else {
      iShouldBeAStaticSetter();
      return value;
    }
  }
  
  public int recursiveAbsSameIncrementA(int value) {
    if (value < 0) {
      return recursiveAbsSameIncrementB(value + myIncrement);
    } else return value;
  }
  
  public int recursiveAbsSameIncrementB(int value) {
    if (value < 0) {
      return recursiveAbsSameIncrementA(value + myIncrement);
    } else return value;
  }
  
  public int recursiveAbsDifferentIncrementA(int value) {
    if (value < 0) {
      return recursiveAbsDifferentIncrementB(value + myIncrement);
    } else return value;
  }
  
  public int recursiveAbsDifferentIncrementB(int value) {
    if (value < 0) {
      return recursiveAbsSameIncrementA(value + myDifferentIncrement);
    } else return value;
  }
  
  public void fakeRec() {
    int a = myDifferentIncrement;
    fakeRec();
  }
  
  public void trueRec() {
    trueRec();
  }
  
}
