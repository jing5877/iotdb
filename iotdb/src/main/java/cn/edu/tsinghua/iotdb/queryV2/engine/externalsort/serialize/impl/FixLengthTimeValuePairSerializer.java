package cn.edu.tsinghua.iotdb.queryV2.engine.externalsort.serialize.impl;

import cn.edu.tsinghua.iotdb.queryV2.engine.externalsort.serialize.TimeValuePairSerializer;
import cn.edu.tsinghua.tsfile.common.utils.BytesUtils;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.timeseries.readV2.datatype.TimeValuePair;

import java.io.*;

/**
 * IMPORTANT: One instance of this class should used with same type of TimeValuePair.
 * FileFormat:
 * [Header][Body]
 * [Header] = [DataTypeLength] + [DataTypeInStringBytes]
 * [DataTypeLength] = 4 bytes
 * Created by zhangjinrui on 2018/1/21.
 */
public class FixLengthTimeValuePairSerializer implements TimeValuePairSerializer {

    private TimeValuePairWriter writer;
    private OutputStream outputStream;
    private boolean dataTypeDefined;

    public FixLengthTimeValuePairSerializer(String tmpFilePath) throws IOException {
        checkPath(tmpFilePath);
        outputStream = new BufferedOutputStream(new FileOutputStream(tmpFilePath));
    }

    @Override
    public void write(TimeValuePair timeValuePair) throws IOException {
        if (!dataTypeDefined) {
            setWriter(timeValuePair.getValue().getDataType());
            writeHeader(timeValuePair.getValue().getDataType());
            dataTypeDefined = true;
        }
        writer.write(timeValuePair, outputStream);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    private void writeHeader(TSDataType dataType) throws IOException {
        String typeInString = dataType.toString();
        outputStream.write(BytesUtils.intToBytes(typeInString.length()));
        outputStream.write(BytesUtils.StringToBytes(typeInString));
    }

    private void checkPath(String tmpFilePath) throws IOException {
        File file = new File(tmpFilePath);
        if (file.exists()) {
            file.delete();
        }
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
    }

    private void setWriter(TSDataType type) {
        switch (type) {
            case BOOLEAN:
                this.writer = new TimeValuePairWriter.BooleanWriter();
                break;
            case INT32:
                this.writer = new TimeValuePairWriter.IntWriter();
                break;
            case INT64:
                this.writer = new TimeValuePairWriter.LongWriter();
                break;
            case FLOAT:
                this.writer = new TimeValuePairWriter.FloatWriter();
                break;
            case DOUBLE:
                this.writer = new TimeValuePairWriter.DoubleWriter();
                break;
            case TEXT:
                this.writer = new TimeValuePairWriter.BinaryWriter();
                break;
            default:
                throw new RuntimeException("Unknown TSDataType in FixLengthTimeValuePairSerializer:" + type);
        }
    }

    private abstract static class TimeValuePairWriter {
        public abstract void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException;

        private static class BooleanWriter extends TimeValuePairWriter {
            @Override
            public void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException {
                outputStream.write(BytesUtils.longToBytes(tvPair.getTimestamp()));
                outputStream.write(BytesUtils.boolToBytes(tvPair.getValue().getBoolean()));
            }
        }

        private static class IntWriter extends TimeValuePairWriter {
            @Override
            public void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException {
                outputStream.write(BytesUtils.longToBytes(tvPair.getTimestamp()));
                outputStream.write(BytesUtils.intToBytes(tvPair.getValue().getInt()));
            }
        }

        private static class LongWriter extends TimeValuePairWriter {
            @Override
            public void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException {
                outputStream.write(BytesUtils.longToBytes(tvPair.getTimestamp()));
                outputStream.write(BytesUtils.longToBytes(tvPair.getValue().getLong()));
            }
        }

        private static class FloatWriter extends TimeValuePairWriter {
            @Override
            public void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException {
                outputStream.write(BytesUtils.longToBytes(tvPair.getTimestamp()));
                outputStream.write(BytesUtils.floatToBytes(tvPair.getValue().getFloat()));
            }
        }

        private static class DoubleWriter extends TimeValuePairWriter {
            @Override
            public void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException {
                outputStream.write(BytesUtils.longToBytes(tvPair.getTimestamp()));
                outputStream.write(BytesUtils.doubleToBytes(tvPair.getValue().getDouble()));
            }
        }

        private static class BinaryWriter extends TimeValuePairWriter {
            @Override
            public void write(TimeValuePair tvPair, OutputStream outputStream) throws IOException {
                outputStream.write(BytesUtils.longToBytes(tvPair.getTimestamp()));
                outputStream.write(BytesUtils.intToBytes(tvPair.getValue().getBinary().getLength()));
                outputStream.write(BytesUtils.StringToBytes(tvPair.getValue().getBinary().getStringValue()));
            }
        }
    }
}
