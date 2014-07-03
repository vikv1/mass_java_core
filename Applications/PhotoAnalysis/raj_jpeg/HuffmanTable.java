import java.util.Hashtable;
import java.util.Vector;

class HuffmanTable {

	private	int [] codeLengthCounts;
	private Hashtable<CodeLengthKeyPair, Short> codes;
	private int tableClass;
	private int id;
	private HuffBinTree codeTree;

	HuffmanTable(){
	
		codeLengthCounts = new int [17];
		codes = new Hashtable<CodeLengthKeyPair, Short>();
	}

	void populateCodeTable(Vector<Short> codeList){
		codeTree = new HuffBinTree(codeList, codes, codeLengthCounts);
	}


	public void setClass(int newClass){
		tableClass = newClass;
	}

	public int getTableClass(){
		return tableClass;
	}

	/// Do a string one too

	void setID(int newID){
		id = newID;
	}

	int getID(){
		return id;
	}

	void setCodeLengthCount(int length, int count){
		////System.out.println("Setting length " + length + " to " + count);
		codeLengthCounts[length] = count;
	}

	int getCodeLengthCount(int length){
		return codeLengthCounts[length];
	}

//	void addCode(short code, int length, short value){
//		if(length == 16){
//			int a = 0;
//		}
//		int intCode = (int)(code);
//		if(intCode > 65535){
//			intCode = intCode | (0x0000FFFF);
//		}
//		codes.put(new CodeLengthKeyPair(intCode, length), value);
//	}

	Object getValue(int code, int length){
		
		CodeLengthKeyPair keys = new CodeLengthKeyPair(code, length);

		
		Object a = codes.get(keys);
		
		////System.out.println("Getting value with key: " + keys.getCode() + ", " + keys.getLength());
		return codes.get(keys);
	}
}
