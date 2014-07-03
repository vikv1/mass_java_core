import java.util.Vector;

class Frame{

	private int length;
	private int precision;
	private int lines;
	private int samplesPerLine;
	private int componentCount;
	private Vector<Component> components;


	Frame(int p, int l, int s, int c){
		precision = p;
		lines = l;
		samplesPerLine = s;
		componentCount = c;
		components = new Vector<Component>(c);
	}

	public void addComponent(int newId, int h, int v, int q){
		components.add(new Component(newId, h, v, q));
	}

	public void setLength(int newLength){
		length = newLength;
	}

	public void setPrecision(int newPrecision){
		precision = newPrecision;
	}

	public void setLines(int newLines){
		lines = newLines;
	}

	public void setSamplesPerLine(int newSamples){
		samplesPerLine = newSamples;
	}

	public void setComponentCount(int newCount){
		componentCount = newCount;
		components.setSize(newCount);
	}

	public int getComponentHFactor(int id){
		if(id > components.size())
			return -1;
		return components.get(id).horizontalFactor;
	}

	public int getComponentVFactor(int id){
		if(id > components.size())
			return -1;
		return components.get(id).verticalFactor;
	}

	class Component {
		private int id;
		private int horizontalFactor;
		private int verticalFactor;
		private int quantTableDest;

		Component (int newId, int h, int v, int q){
			id = newId;
			horizontalFactor = h;
			verticalFactor = v;
			quantTableDest = q;
		}

		public int getId(){
			return id;
		}

		public int getHorizontalFactor(){
			return horizontalFactor;
		}

		public int getVerticalFactor(){
			return verticalFactor;
		}

		public int getQuantTable(){
			return quantTableDest;
		}

	}
}
