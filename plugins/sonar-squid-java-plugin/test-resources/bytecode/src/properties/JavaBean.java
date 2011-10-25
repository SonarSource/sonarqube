package properties;

import java.util.ArrayList;

public class JavaBean {
  
  private String name;
  private boolean french;
  ArrayList<String> firstNames = new ArrayList<String>();
  private static String staticMember;
  private String FirstName;
  
  public String getName(){
    return name;
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
  
  public void iShouldBeAStaticSetter() {
    staticMember = "Hello!";
  }
  
  public String getFirstName() {
    return FirstName;
  }
  
}
