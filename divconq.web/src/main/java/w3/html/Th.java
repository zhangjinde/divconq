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
package w3.html;

import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.MixedElement;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.xml.XElement;

public class Th extends MixedElement implements ICodeTag {
    public Th() {
    	super();
    }
    
    public Th(Object... args) {
    	super(args);
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Th cp = new Th();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("colspan"))
			attrs.add("colspan", xel.getRawAttribute("colspan"));
		
		if (xel.hasAttribute("rowspan"))
			attrs.add("rowspan", xel.getRawAttribute("rowspan"));
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		if (xel.hasAttribute("width"))
			attrs.add("width", xel.getRawAttribute("width"));

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}

    @Override
    public void build(Object... args) {
        super.build("th", true, new Attributes("valign", "top"), args);
    }
}
