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
package divconq.script.inst.file;

import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreDriver;
import divconq.filestore.IFileStoreFile;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.script.inst.With;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class File extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");
        
        if (StringUtil.isEmpty(name))
        	name = "Folder_" + stack.getActivity().tempVarName();
        
        String vname = name;
        
        Struct ss = stack.refFromSource("In");
        
        if ((ss == null) || (!(ss instanceof IFileStoreDriver) && !(ss instanceof IFileStoreFile))) {
        	OperationContext.get().errorTr(536);
    		this.nextOpResume(stack);
        	return;
        }
        
        CommonPath path = null;
        
        try {
            path = new CommonPath(stack.stringFromSource("Path", "/"));
        }
        catch (Exception x) {
        	OperationContext.get().errorTr(537);
			this.nextOpResume(stack);
			return;
        }

        IFileStoreDriver drv = null;
        
        if (ss instanceof IFileStoreDriver) {
            drv = (IFileStoreDriver)ss;
        }
        else {
        	drv = ((IFileStoreFile)ss).driver();
        	path = ((IFileStoreFile)ss).resolvePath(path);
        }
        
        drv.getFileDetail(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					OperationContext.get().errorTr(538);
					File.this.nextOpResume(stack);
					return;
				}
				
	            IFileStoreFile fh = this.getResult();			            
	            
	            if (!fh.exists() && stack.getInstruction().getXml().getName().equals("Folder"))
	            	fh.isFolder(true); 
				
	            stack.addVariable(vname, (Struct)fh);
	            
	            File.this.setTarget(stack, (Struct)fh);
	            
	    		File.this.nextOpResume(stack);
			}
		});
	}
}
