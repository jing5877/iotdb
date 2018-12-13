//package cn.edu.tsinghua.iotdb.queryV2.reader.component;
//
//import cn.edu.tsinghua.tsfile.timeseries.readV2.common.SeriesChunk;
//import cn.edu.tsinghua.tsfile.timeseries.readV2.common.SeriesChunkDescriptor;
//
//import java.io.InputStream;
//
//
//public class BufferedSeriesChunk implements SeriesChunk {
//
//    private SegmentInputStream seriesChunkInputStream;
//    private SeriesChunkDescriptor seriesChunkDescriptor;
//
//    public BufferedSeriesChunk(SegmentInputStream seriesChunkInputStream, SeriesChunkDescriptor seriesChunkDescriptor) {
//        this.seriesChunkInputStream = seriesChunkInputStream;
//        this.seriesChunkDescriptor = seriesChunkDescriptor;
//    }
//
//    @Override
//    public SeriesChunkDescriptor getEncodedSeriesChunkDescriptor() {
//        return this.seriesChunkDescriptor;
//    }
//
//    @Override
//    public InputStream getSeriesChunkBodyStream() {
//        return this.seriesChunkInputStream;
//    }
//
//}
