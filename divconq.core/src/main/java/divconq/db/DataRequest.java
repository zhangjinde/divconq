/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db;

import divconq.db.IDatabaseRequest;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;

/**
 * Assemble a generic Query request for the database.  A query request should not
 * call a stored procedure with parameters that will cause it to update/alter data
 * with in the database (other than temp tables and caches).  Other than that restriction
 * this class can call nearly any stored procedure if the parameters are assembled 
 * correctly.
 * 
 * @author Andy
 *
 */
public class DataRequest implements IDatabaseRequest {
	protected CompositeStruct parameters = null;	
	protected String proc = null;
	protected boolean rootdomain = false;
	
	@Override
	public boolean isRootDomain() {		
		return this.rootdomain;
	}
	
	public DataRequest withRootDomain(boolean v) {
		this.rootdomain = v;
		return this;
	}
	
	public DataRequest withParams(CompositeStruct v) {
		this.parameters = v;
		return this;
	}
	
	public DataRequest withEmptyParams() {
		this.parameters = new RecordStruct();
		return this;
	}
	
	/**
	 * Build an unguided query request.
	 * 
	 * @param proc procedure name to call
	 */
	public DataRequest(String proc) {
		this.proc = proc;
	}
	
	@Override
	public CompositeStruct buildParams() {
		return this.parameters;
	}
	
	@Override
	public boolean isReplicate() {
		return false;
	}
	
	@Override
	public String getProcedure() {
		return this.proc;
	}
}
