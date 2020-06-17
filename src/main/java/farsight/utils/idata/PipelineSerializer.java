package farsight.utils.idata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.wm.data.IData;
import com.wm.util.coder.IDataCoder;
import com.wm.util.coder.IDataJSONCoder;
import com.wm.util.coder.IDataXMLCoder;

import farsight.utils.streams.LimitedByteArrayOutputStream;

public class PipelineSerializer {

	public static byte[] serializePipelineXML(IData pipeline, int limit) {
		return serializePipeline(pipeline, new IDataXMLCoder(), limit);
	}

	public static byte[] serializePipelineXML(IData pipeline) {
		return serializePipelineXML(pipeline, 0);
	}

	public static byte[] serializePipelineJson(IData pipeline, int limit) {
		return serializePipeline(pipeline, new IDataJSONCoder(), limit);
	}

	public static byte[] serializePipelineJson(IData pipeline) {
		return serializePipelineJson(pipeline, 0);
	}

	private static ByteArrayOutputStream createByteArrayStream(int limit) {
		return limit > 0 ? new LimitedByteArrayOutputStream(limit).setThrowException(true)
				: new ByteArrayOutputStream();
	}

	public static byte[] serializePipeline(IData pipeline, IDataCoder coder) {
		return serializePipeline(pipeline, coder, 0);
	}

	public static byte[] serializePipeline(IData pipeline, IDataCoder coder, int limit) {
		try (ByteArrayOutputStream os = createByteArrayStream(limit)) {
			coder.encode(os, pipeline);
			return os.toByteArray();
		} catch (RuntimeException | IOException e) {
			return null;
		}
	}

	public static IData deserializePipelineXML(byte[] bytes) {
		return deserializePipeline(bytes, new IDataXMLCoder());
	}

	public static IData deserializePipelineXML(InputStream in) {
		return deserializePipeline(in, new IDataXMLCoder());
	}

	public static IData deserializePipelineJSON(byte[] bytes) {
		return deserializePipeline(bytes, new IDataJSONCoder());
	}

	public static IData deserializePipelineJSON(InputStream in) {
		return deserializePipeline(in, new IDataJSONCoder());
	}

	public static IData deserializePipeline(byte[] bytes, IDataCoder coder) {
		try {
			return coder.decodeFromBytes(bytes);
		} catch (IOException e) {
			return null;
		}
	}

	public static IData deserializePipeline(InputStream in, IDataCoder coder) {
		try {
			return coder.decode(in);
		} catch (IOException e) {
			return null;
		}
	}

}
