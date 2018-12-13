package cn.edu.tsinghua.tsfile.read.controller;


import cn.edu.tsinghua.tsfile.file.header.ChunkHeader;
import cn.edu.tsinghua.tsfile.file.metadata.ChunkMetaData;
import cn.edu.tsinghua.tsfile.read.common.Chunk;
import cn.edu.tsinghua.tsfile.utils.TsFileGeneratorForTest;
import cn.edu.tsinghua.tsfile.read.TsFileSequenceReader;
import cn.edu.tsinghua.tsfile.read.common.Path;
import cn.edu.tsinghua.tsfile.exception.write.WriteProcessException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;


public class ChunkLoaderTest {

    private static final String FILE_PATH = TsFileGeneratorForTest.outputDataFile;
    private TsFileSequenceReader fileReader;

    @Before
    public void before() throws InterruptedException, WriteProcessException, IOException {
        TsFileGeneratorForTest.generateFile(1000000, 1024 * 1024, 10000);
    }

    @After
    public void after() throws IOException {
        fileReader.close();
        TsFileGeneratorForTest.after();
    }

    @Test
    public void test() throws IOException {
        fileReader = new TsFileSequenceReader(FILE_PATH, true);
        MetadataQuerierByFileImpl metadataQuerierByFile = new MetadataQuerierByFileImpl(fileReader);
        List<ChunkMetaData> chunkMetaDataList = metadataQuerierByFile.getChunkMetaDataList(new Path("d2.s1"));

        ChunkLoaderImpl seriesChunkLoader = new ChunkLoaderImpl(fileReader);
        for (ChunkMetaData chunkMetaData : chunkMetaDataList) {
            Chunk chunk = seriesChunkLoader.getChunk(chunkMetaData);
            ChunkHeader chunkHeader = chunk.getHeader();
            Assert.assertEquals(chunkHeader.getDataSize(), chunk.getData().remaining());
        }
    }
}
