import java.util.Vector;

class Scan {

	int componentCount;
	Vector<ScanComponent> components;
	int startOfSelection;
	int endOfSelection;
	int bitHigh;
	int bitLow;

	Scan(int c){
		componentCount = c;
		components = new Vector<ScanComponent>(c);
	}

	public void addComponent(int s, int d, int a){
		components.add(new ScanComponent(s, d, a));
	}

	public void setStartOfSelection(int s){
		startOfSelection = s;
	}

	public void setEndOfSelection(int e){
		endOfSelection = e;
	}

	public void setBitHigh(int b){
		bitHigh = b;
	}

	public void setBitLow(int b){
		bitLow = b;
	}


	class ScanComponent{
		int scanSelector, dcTableSelector, acTableSelector;

		ScanComponent(int s, int d, int a){
			scanSelector = s;
			dcTableSelector = d;
			acTableSelector = a;
		}

		public int getScanSelector(){
			return scanSelector;
		}

		public int getDcTableSelector(){
			return dcTableSelector;
		}

		public int getAcTableSelector(){
			return acTableSelector;
		}

	}



}
