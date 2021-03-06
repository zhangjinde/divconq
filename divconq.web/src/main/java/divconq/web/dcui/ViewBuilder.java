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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationResult;
import divconq.util.StringUtil;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class ViewBuilder extends Fragment implements IViewExecutor {
	protected WebContext context = null;
	protected boolean dynamic = false;
	
	public ViewBuilder() {
		super(null);
	}
	
    @Override
    public void doBuild() {
    	try {
			Nodes content = this.view.getOutput(this, this.context, this.dynamic);  
	
			if (this.context.isCompleted())
				return;
		
			this.build(content);
		} 
		catch (Exception x) {
			// TODO 
			System.out.println("View builder build error: " + x);
		}  
    }

	@Override
    public void write() throws IOException {
		if (this.dynamic) {
			PrintStream ps = this.context.getResponse().getPrintStream();

			ps.println("dc.pui.Loader.addPageDefinition('" + this.context.getRequest().getOriginalPath() + "', {");
			
			if (this.view.source.hasAttribute("Title")) {
				ps.print("\tTitle: '");
				Node.writeDynamicJsString(ps, this.view.source.getAttribute("Title"));
				ps.println("',");
			}
			
			ps.println("\tLayout: [");

			this.writeDynamicChildren(ps, "");
			
			ps.println();
			ps.println("\t],");
			
			boolean first = true;
			
			// ==============================================
			//  Libs
			// ==============================================
			
			ps.print("\tRequireLibs: [");
			
			for (XElement func : this.view.source.selectAll("RequireLib")) {
				if (!func.hasAttribute("Path"))
					continue;
				
				if (first)
					first = false;
				else
					ps.print(",");
				
				ps.print(" '");				
				Node.writeDynamicJsString(ps, func.getAttribute("Path"));				
				ps.print("'");
			}
			
			ps.println(" ], ");
			
			// ==============================================
			//  Functions
			// ==============================================
			
			first = true;
			
			ps.println("\tFunctions: {");
			
			for (XElement func : this.view.source.selectAll("Function")) {
				if (!func.hasAttribute("Name"))
					continue;
				
				if (first)
					first = false;
				else
					ps.println(",");
				
				ps.print("\t\t" + func.getAttribute("Name") + ": function(" + func.getAttribute("Params", "") + ") {");
				
				ps.print(func.getText());
				
				ps.print("\t\t}");
			}
			
			ps.println();
			
			ps.println("\t}");
			
			ps.println("});");
			
			ps.println();
			
			ps.println("dc.pui.Loader.resumePageLoad();");			
		}
		else {
	    	this.context.getResponse().getPrintStream().println("<!DOCTYPE html>");        
	        super.write();
		}
    }

	@Override
	public OperationResult execute(WebContext ctx) throws Exception {
		OperationResult or = new OperationResult();
		
		this.context = ctx;
		
		String mode = ctx.getExternalParam("_dcui");

		if ("dyn".equals(mode) || "dyn".equals(mode)) {
			this.dynamic = true;
			this.context.getResponse().setHeader("Content-Type", "application/javascript");
		}
		else {
			this.context.getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
			this.context.getResponse().setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
		}
		
		this.doBuild();
		
		if (this.context.isCompleted()) {
			this.context.send();
			return or;
		}
	
		this.awaitForFutures(new OperationCallback() {
			@Override
			public void callback() {
				if (!ViewBuilder.this.context.isCompleted()) {
					try {
						ViewBuilder.this.write();
					} 
					catch (IOException x) {
						// TODO log
						System.out.println("View builder execute error: " + x);
					}
				}
				
				ViewBuilder.this.context.send();
			}
		});
		
		return or;
	}

	@Override
	public WebContext getContext() {
		return this.context;
	}

	public static String streamNodes(String indent, List<Node> children, boolean cleanWhitespace) {
        if (children.size() == 0) 
        	return null;
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(os);
        
        boolean lastblock = true;
        boolean firstch = true;   

        for (Node node : children) {
            node.stream(out, indent, (firstch || lastblock), true);
            
            lastblock = node.getBlockIndent();
            firstch = false;
        }
        
        out.flush();
        
        if (cleanWhitespace)
        	return StringUtil.stripWhitespacePerXml(os.toString());
        
        return os.toString();
	}
}
