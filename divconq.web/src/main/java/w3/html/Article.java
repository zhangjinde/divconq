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

public class Article extends MixedElement implements ICodeTag {
	protected String id = null;
	protected String cssclass = null;
	
	public Article() {
		super();
	}
	
	public Article(Object... args) {
		super(args);
	}
	
    public Article(String id, String cssclass, Object... args) {
    	super(args);
    	this.id = id;
    	this.cssclass = cssclass;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Article nn = (Article)n;
    	nn.id = this.id;
    	nn.cssclass = this.cssclass;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Article cp = new Article();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
		Attributes extra = new Attributes();
		
		if (this.id != null) 
			extra.add("id", this.id);
		
		if (this.cssclass != null) 
			extra.add("class", this.cssclass);
    	
	    super.build("article", true, extra, args);
	}
}
