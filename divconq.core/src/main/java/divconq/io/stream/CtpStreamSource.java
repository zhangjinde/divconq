package divconq.io.stream;

import io.netty.channel.Channel;
import divconq.script.StackEntry;
import divconq.xml.XElement;

public class CtpStreamSource extends BaseStream implements IStreamSource {
	protected Channel chan = null;
	protected StreamMessage next = null;
	
	public void setNext(StreamMessage v) {
		this.next = v;
	}
	
	public CtpStreamSource(Channel chan, StreamMessage first) {
		this.chan = chan;
		this.setNext(first);
	}

	@Override
	public void init(StackEntry stack, XElement el) {
	}
	
	@Override
	public void close() {
		super.close();
		
		this.chan = null;
		this.next = null;
	}

	@Override
	public HandleReturn handle(StreamMessage msg) {
		if (this.downstream.handle(msg) == HandleReturn.CONTINUE) 
			this.chan.read();
		
		return null;
	}

	@Override
	public void request() {
		if (this.next != null) {
			StreamMessage f = this.next;
			
			this.next = null;
			
			if (this.downstream.handle(f) == HandleReturn.CONTINUE) 
				this.chan.read();
		}
		else 
			this.chan.read();
	}
}
