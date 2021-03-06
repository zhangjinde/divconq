/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.web.dcui;

import java.io.PrintStream;

import w3.html.Form;
import divconq.web.dcui.Element;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public class AdvForm extends Form {
	protected String fname = null;
	
    public AdvForm() {
    	super();
	}
    
    public AdvForm(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		AdvForm cp = new AdvForm();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((AdvForm)n).fname = this.fname;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		this.fname = xel.getAttribute("Name");
		
		super.parseElement(view, nodes, xel);
	}
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
    	this.name = "Form";
    	
    	this.attributes.put("Name", this.fname);
    	
    	return super.writeDynamic(buffer, tabs, first);
    }
}
