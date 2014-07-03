import java.util.Vector;

class QuantizationTable {

	private int length;
	private short precision;
	private int id;
	private Vector<Short> elements;

	QuantizationTable(){
		elements = new Vector<Short>(64);
	}

	public void setLength(int newLength){
		length = newLength;
	}

	public void setPrecision(short newPrecision){
		precision = newPrecision;
	}

	public short getPrecision(){
		return precision;
	}

	public void setID(int newID){
		id = newID;
	}

	public int getID(){
		return id;
	}

	public void addElement(short value){
		elements.add(value);
	}

	public short getElement(int index){
		return elements.get(index);
	}
}

