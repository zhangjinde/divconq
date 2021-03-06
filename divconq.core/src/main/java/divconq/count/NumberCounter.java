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
package divconq.count;

import java.math.BigDecimal;

import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

public class NumberCounter extends Counter {
	protected BigDecimal value = null; 
	protected BigDecimal low = null;
	protected BigDecimal high = null;
	
	public void setValue(long value) {
		this.setValue(BigDecimal.valueOf(value));
	}

	public void setValue(double value) {
		this.setValue(BigDecimal.valueOf(value));
	}
	
	public void setValue(BigDecimal value) {
		try {
			this.valuelock.lockInterruptibly();
		} 
		catch (InterruptedException x) {
			return;   // should only happen under bad conditions, so probably nothing we can do
		}
		
		// we are locked during whole set/notify process so be efficient 
		try {
			this.value = value;
			
			if (this.value != null) {
				if ((this.high == null) || (this.value.compareTo(this.high) > 0))
					this.high = this.value;
				
				if ((this.low == null) || (this.value.compareTo(this.low) < 0))
					this.low = this.value;
			}
			
	    	this.setChanged();
		}
		finally {
			this.valuelock.unlock();
		}
	}
	
	public BigDecimal getValue() {
		return this.value;
	}
	
	public BigDecimal getHigh() {
		return this.high;
	}
	
	public BigDecimal getLow() {
		return this.low;
	}
	
	public NumberCounter(String name) {
		super(name);
	}

	public NumberCounter(String name, BigDecimal value) {
		this(name);
		
		this.setValue(value);
	}

	public void increment() {
		try {
			this.valuelock.lockInterruptibly();
		} 
		catch (InterruptedException x) {
			return;   // should only happen under bad conditions, so probably nothing we can do
		}
		
		// we are locked during whole set/notify process so be efficient 
		try {
			if (this.value == null)
				this.setValue(1);
			else
				this.setValue(this.value.add(BigDecimal.valueOf(1)));
		}
		finally {
			this.valuelock.unlock();
		}
	}
	
	@Override
	public Counter clone() {
		NumberCounter clone = new NumberCounter(this.name, this.value);
		this.copyToClone(clone);
		return clone;
	}
	
	@Override
	public void copyToClone(Counter clone) {
		super.copyToClone(clone);
		
		// caller is doing weird stuff - ignore them
		if (!(clone instanceof NumberCounter))
			return;
		
		NumberCounter nclone = (NumberCounter)clone; 
		
		nclone.low = this.low;
		nclone.high = this.high;
	}
	
	@Override
	public RecordStruct toRecord() {
		return new RecordStruct(new FieldStruct("Name", this.name), new FieldStruct("Value", this.value),
				new FieldStruct("High", this.high), new FieldStruct("Low", this.low),
				new FieldStruct("Object", this.currentObject));
	}
	
	@Override
	public RecordStruct toCleanRecord() {
		return new RecordStruct(new FieldStruct("Name", this.name), new FieldStruct("Value", this.value),
				new FieldStruct("High", this.high), new FieldStruct("Low", this.low));
	}
	
	@Override
	public void reset() {
		super.reset();
		
		this.value = null; 
		this.low = null;
		this.high = null;
	}
}
