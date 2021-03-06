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
package divconq.web;

import divconq.lang.op.OperationResult;
import divconq.mod.Bundle;
import divconq.mod.IExtension;
import divconq.session.Session;

public interface IWebExtension extends IExtension {
	String getAppName();
	OperationResult handle(Session sess, HttpContext hctx);
	Bundle getBundle();
}
