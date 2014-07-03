import java.util.Iterator;
import java.util.Hashtable;
import java.util.Vector;
class HuffBinTree{



	Node root;
	Vector<Short> values;
	Hashtable<CodeLengthKeyPair, Short> codeTable;
	int [] codeLengthCounts;


	HuffBinTree(Vector<Short> newValues, Hashtable<CodeLengthKeyPair, Short> newCodeTable, int newCodeLengthCounts []){
		root = new Node((short)(0));
		values = newValues;
		codeTable = newCodeTable;
		codeLengthCounts = newCodeLengthCounts;
		populateCodeTable();
	}

	private void populateCodeTable(){
		Iterator<Short> i = values.iterator();
		short codeLengthCount;
		int currentLength = 1;

		// Stores the # of codes processed of a given length
		int codesProcessed = 0;

		root.addNodes();
		// While we have more values to iterate over, keep creating and setting code/value pairs
		while(i.hasNext()){

			// Checks to see if we have processed all the codes of the current length. The
			// loop will end when we have processed less codes than there are for the given
			// length. During each execution, it will create new nodes in the tree.
			while(codeLengthCounts[currentLength] == codesProcessed){
				currentLength += 1;
				codesProcessed = 0;
				root.addNodes();
				//System.out.println("Adding a level, currentLength = " + currentLength);
			}	
			// NO WAY
			int temp = getNewCode(currentLength) & 0x0000FFFF;
			
			//System.out.println("Code is " + temp);
			codeTable.put(new CodeLengthKeyPair(temp, currentLength), i.next());
			codesProcessed++;
		}	
	}

	private short getNewCode(int length){
		return (root.getNewCode(length));

	}

	public void printCodes(){
		//System.out.println(codeTable);
	}
	
	
	
	class Node {
		private short code;
		private boolean isCode;
		private Node left;
		private Node right;

		Node(short newCode){
			code = newCode;
			isCode = false;
		}

		public boolean isCodeNode(){
			return isCode;
		}

		public void addNodes(){
			if(isCode){
				return;
			}
			if (left == null){
				left = new Node((short)(code * 2));
				////System.out.println("Creating a left child. Parent code = " + code);
			} else {
				left.addNodes();
			}
			if (right == null){
				right = new Node((short)((code * 2) + 1));
				////System.out.println("Creating a right child. Parent code = " + code);
			} else {
				right.addNodes();
			}

		}

		public short getNewCode(int length){
	
			// Code 0xFFFF isn't allowed, so we'll use it as a flag indicating the code
			// hasn't been found
			short result = (byte)(0xFFFF);
	
			// If this node is a code, then we can't use it, and the children are null
			// so return the 0xFFFF flag
			if(isCode){
				////System.out.println("Found a code : " + code);
				return result;
			}
	
			// If length > 0, then we haven't gone down far enough, so recurse left, then right
			if (length > 0){
				////System.out.println("Length = " + length + " so we're recursing left");
				result = left.getNewCode(length -1);
			
				// Result now equals the value of the code on the left side. If it isn't the
				// not found value, then return it
				if(result != (byte)(0xFFFF)){
					////System.out.println("The code didn't equal FFFF, so we're returning it");
					return result;
	
				// otherwise, get the code value from the right side, and return it
				} else {
					////System.out.println("Recuring right");
					result = right.getNewCode(length - 1);
					return result;
				}
				//return result;
			}
	
			// length = 0, so we're at the code node of correct length, and it isn't already
			// a code, so that's our value
			////System.out.println("Length = " + length + " which should be 0, so we're returning a code");
			isCode = true;
			return code;
		}
	}
}
