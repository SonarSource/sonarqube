public class CommentedCode {
	
	
	public CommentedCode(){
		//This is a comment
		//if (true) {
		int i = 2;
	}
	
	/**
	 * No detection of commented code line in Javadoc bloc
	 * 
	 * public void main(){
	 * 	
	 * }
	 */
	public void analyse(){
		/*
		for(Visitor visitor : visitors){
			visitor.scan(line);
		}
		*/
		
	}
}