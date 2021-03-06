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
package divconq.ctp.stream;

import divconq.work.ISmartWork;
import divconq.work.TaskRun;

public class StreamWork implements ISmartWork {
	protected IStreamDest dest = null;
	
	public StreamWork(IStreamDest dest) {
		this.dest = dest;
	}

	@Override
	public void run(TaskRun trun) {
		IStreamDest d = this.dest;
		
		if (d != null)
			d.execute();
		else 
			trun.kill("Attempted to run StreamWork but missing dest.");
	}

	@Override
	public void cancel(TaskRun run) {
	}

	@Override
	public void completed(TaskRun run) {
		//System.out.println("Start cleanup");
		
		IStreamDest d = this.dest;
		
		if (d != null)
			d.cleanup();
		
		this.dest = null;
		
		//System.out.println("End cleanup");
	}
}
