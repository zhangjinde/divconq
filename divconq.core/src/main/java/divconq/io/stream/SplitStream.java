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
package divconq.io.stream;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class SplitStream extends BaseStream implements IStreamSource {
	protected int seqnum = 1;
	protected int size = 10 * 1024 * 1024;
	protected String template = "file-%seq%.bin";
	
	protected int currchunk = 0;
    protected List<StreamMessage> outlist = new ArrayList<>();
	
    public SplitStream() {
    }

	@Override
	public void init(StackEntry stack, XElement el) {
		this.seqnum = (int) stack.intFromElement(el, "StartAt", this.seqnum);
		
		String size = stack.stringFromElement(el, "Size", "10MB");
		
		this.size = (int) FileUtil.parseFileSize(size);
		
		String temp = stack.stringFromElement(el, "Template");
		
		if (StringUtil.isNotEmpty(temp))
			this.template = temp;
	}
    
	// make sure we don't return without first releasing the file reference content
    @Override
    public HandleReturn handle(StreamMessage msg) {
    	if (msg == StreamMessage.FINAL) 
    		return this.downstream.handle(msg);

    	ByteBuf in = msg.getPayload();

    	if (in != null) {
    		while (in.isReadable()) {
    			int amt = Math.min(in.readableBytes(), this.size - this.currchunk);
    			
    			ByteBuf out = in.copy(in.readerIndex(), amt);
    			
    			in.skipBytes(amt);
    			this.currchunk += amt;
    		
    			boolean eof = (this.currchunk == this.size) || (!in.isReadable() && msg.isEof());
    			
    			this.outlist.add(this.nextMessage(out, msg, eof));
    			
    			if (eof) {
    				this.seqnum++;
    				this.currchunk = 0;
    			}
			}
    		
    		in.release();
    	}
    	else if (msg.isEof()) {
			this.outlist.add(this.nextMessage(null, msg, false));
    	}
    	
		// write all messages in the queue
		while (this.outlist.size() > 0) {
			HandleReturn ret = this.downstream.handle(this.outlist.remove(0));
			
			if (ret != HandleReturn.CONTINUE)
				return ret;
		}
    	
       	return HandleReturn.CONTINUE;
    }
    
    public StreamMessage nextMessage(ByteBuf out, StreamMessage curr, boolean eof) {
		// create the output message
		StreamMessage blk = new StreamMessage();
		
        blk.setPayload(out);
        blk.setModified(System.currentTimeMillis());		
        
        // keep the path, just vary the name to the template
        blk.setPath(curr.getPath().resolvePeer("/" + this.template.replace("%seq%", this.seqnum + "")));
        
        blk.setEof(eof);
        
        if (eof)
        	blk.setFileSize(this.currchunk);
        else
        	blk.setFileSize(0);						// don't know
        
        return blk;
    }
    
    @Override
    public void request() {
		// write all messages in the queue
		while (this.outlist.size() > 0) {
			HandleReturn ret = this.downstream.handle(this.outlist.remove(0));
			
			if (ret != HandleReturn.CONTINUE)
				return;
		}
		
    	this.upstream.request();
    }
}
