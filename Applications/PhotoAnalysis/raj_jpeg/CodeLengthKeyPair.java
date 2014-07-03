
class CodeLengthKeyPair{
	
	private int code;
	private int length;

	CodeLengthKeyPair(int c, int l){
		code = c;
		length = l;
	}

	public int getCode(){
		return code;
	}

	public int getLength(){
		return length;
	}

	public int hashCode(){
		//if(length == 16){
			//int test = code + length;
			////System.out.println("Test hashcode for (c,l) : (" + code + ", " + length + ") : " + test);
	//	}
		return code + length;
	}

	public boolean equals(Object rhs){
		if(this == rhs){
			return true;
		}

		if(!(rhs instanceof CodeLengthKeyPair)){
			return false;
		}

		CodeLengthKeyPair right = (CodeLengthKeyPair)(rhs);

		if(this.code != right.code || this.length != right.length){
			return false;
		}

		return true;

	}
}
