/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.graph;

import java.util.function.Function;

public class HIPPIETABEdge {
    private String proteinKey;
    private int proteinId;
    private String interactionKey;
    private int interactionId;
    private double interactionAttribute;

    private String extendedAttribute;

    public String getProteinKey() {
        return proteinKey;
    }

    public void setProteinKey(String proteinKey) {
        this.proteinKey = proteinKey;
    }

    public int getProteinId() {
        return proteinId;
    }

    public void setProteinId(int proteinId) {
        this.proteinId = proteinId;
    }

    public String getInteractionKey() {
        return interactionKey;
    }

    public void setInteractionKey(String interactionKey) {
        this.interactionKey = interactionKey;
    }

    public int getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(int interactionId) {
        this.interactionId = interactionId;
    }

    public double getInteractionAttribute() {
        return interactionAttribute;
    }

    public void setInteractionAttribute(double interactionAttribute) {
        this.interactionAttribute = interactionAttribute;
    }

    public String getExtendedAttribute() {
        return extendedAttribute;
    }

    public void setExtendedAttribute(String extendedAttribute) {
        this.extendedAttribute = extendedAttribute;
    }

    public static HIPPIETABEdge fromParts(String[] parts) {
        HIPPIETABEdge edge = new HIPPIETABEdge();

        Function<HIPPIETABFormatLineParts, String> getPart = (HIPPIETABFormatLineParts part) -> HIPPIETABFormatLineParts.getPart(parts, part);

        edge.proteinKey = getPart.apply(HIPPIETABFormatLineParts.PROTEIN_KEY);
        edge.proteinId = Integer.parseInt(getPart.apply(HIPPIETABFormatLineParts.PROTEIN_ID));

        edge.interactionKey = getPart.apply(HIPPIETABFormatLineParts.INTERACTION_KEY);
        edge.interactionId = Integer.parseInt(getPart.apply(HIPPIETABFormatLineParts.INTERACTION_ID));

        edge.interactionAttribute = Double.parseDouble(getPart.apply(HIPPIETABFormatLineParts.INTERACTION_ATTRIBUTE));

        edge.extendedAttribute = getPart.apply(HIPPIETABFormatLineParts.EXTENDED_ATTRIBUTES);

        return edge;
    }
}
