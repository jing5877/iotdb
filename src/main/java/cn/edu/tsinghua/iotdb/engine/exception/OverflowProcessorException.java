package cn.edu.tsinghua.iotdb.engine.exception;

import cn.edu.tsinghua.iotdb.exception.PathErrorException;
import cn.edu.tsinghua.tsfile.common.exception.ProcessorException;

public class OverflowProcessorException extends ProcessorException {

	private static final long serialVersionUID = -2784502746101925819L;

	public OverflowProcessorException() {
		super();
	}

	public OverflowProcessorException(PathErrorException pathExcp) {
		super(pathExcp.getMessage());
	}

	public OverflowProcessorException(String msg) {
		super(msg);
	}

	public OverflowProcessorException(Throwable throwable) {
		super(throwable.getMessage());
	}

	
}