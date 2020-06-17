package farsight.utils.streams;

import java.io.ByteArrayOutputStream;

public class LimitedByteArrayOutputStream extends ByteArrayOutputStream {

	private static final String DEFAULT_EXCEPTION_TEXT = "Capacity exceeded";
	private static int DEFAULT_INITIAL_SIZE = 1024;
	
	private boolean throwException;
	private final int maxSize;
	private boolean isLimitExceeded = false;
	private String exceptionText = DEFAULT_EXCEPTION_TEXT;
	
	
	public boolean isLimitExceeded() {
		return isLimitExceeded;
	}
	
	public LimitedByteArrayOutputStream setExceededExceptionText(String text) {
		this.exceptionText = text;
		this.setThrowException(true);
		return this;
	}
	
	public LimitedByteArrayOutputStream setThrowException(boolean state) {
		throwException = state;
		return this;
	}

	@Override
	public synchronized void write(int arg0) {
		if(!isLimitExceeded && size() < maxSize) {
			super.write(arg0);
		} else {
			isLimitExceeded = true;
			if(throwException) {
				throw new RuntimeException(exceptionText);
			}
		}
	}
	
	@Override
	public synchronized void write(byte[] bytes, int start, int length) {
		//check capacity
		if(!isLimitExceeded && size() + length <= maxSize) {
			super.write(bytes, start, length);			
		} else {
			isLimitExceeded = true;
			if(size() < maxSize) {
				//fill to limit
				super.write(bytes, start, maxSize - size());
			}
			if(throwException) {
				throw new RuntimeException(exceptionText);
			}
		}
	}

	public LimitedByteArrayOutputStream(int maxSize, int intialSize) {
		super(intialSize);
		this.maxSize = maxSize;
		this.throwException = false;
	}
	
	public LimitedByteArrayOutputStream(int maxSize) {
		this(maxSize, Math.min(DEFAULT_INITIAL_SIZE, maxSize));
	}
	
	
}
