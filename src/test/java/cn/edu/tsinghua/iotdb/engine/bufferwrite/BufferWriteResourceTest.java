package cn.edu.tsinghua.iotdb.engine.bufferwrite;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.edu.tsinghua.iotdb.engine.bufferwriteV2.BufferWriteResource;
import cn.edu.tsinghua.iotdb.engine.memtable.IMemTable;
import cn.edu.tsinghua.iotdb.engine.memtable.MemTableTestUtils;
import cn.edu.tsinghua.iotdb.engine.memtable.TreeSetMemTable;
import cn.edu.tsinghua.iotdb.utils.EnvironmentUtils;
import cn.edu.tsinghua.tsfile.common.utils.BytesUtils;
import cn.edu.tsinghua.tsfile.common.utils.ITsRandomAccessFileWriter;
import cn.edu.tsinghua.tsfile.common.utils.Pair;
import cn.edu.tsinghua.tsfile.common.utils.TsRandomAccessFileWriter;
import cn.edu.tsinghua.tsfile.file.metadata.RowGroupMetaData;
import cn.edu.tsinghua.tsfile.file.metadata.TsRowGroupBlockMetaData;
import cn.edu.tsinghua.tsfile.file.utils.ReadWriteThriftFormatUtils;
import cn.edu.tsinghua.tsfile.timeseries.write.schema.FileSchema;

public class BufferWriteResourceTest {

	private BufferWriteResource bufferwriteResource;
	private String processorName = "processor";
	private String insertPath = "insertfile";
	private String insertRestorePath = insertPath + ".restore";

	@Before
	public void setUp() throws Exception {
		bufferwriteResource = new BufferWriteResource(processorName, insertPath);
	}

	@After
	public void tearDown() throws Exception {
		EnvironmentUtils.cleanDir(insertPath);
	}

	@Test
	public void testInitResource() throws IOException {
		Pair<Long, List<RowGroupMetaData>> pair = bufferwriteResource.readRestoreInfo();
		assertEquals(true, new File(insertRestorePath).exists());
		assertEquals(0, (long) pair.left);
		assertEquals(0, pair.right.size());
		FileSchema fileSchema = new FileSchema();
		bufferwriteResource.close(fileSchema);
		assertEquals(false, new File(insertRestorePath).exists());
	}

	@Test
	public void testRecover() throws IOException {
		File insertFile = new File(insertPath);
		// mkdir
		insertFile.mkdir();
		ITsRandomAccessFileWriter out = new TsRandomAccessFileWriter(new File(insertRestorePath));
		// write tsfile position using byte[8] which is present one long
		writeRestoreFile(out, 2);
		writeRestoreFile(out, 3);
		byte[] lastPositionBytes = BytesUtils.longToBytes(200);
		out.write(lastPositionBytes);
		bufferwriteResource = new BufferWriteResource(processorName, insertPath);
		assertEquals(insertPath, bufferwriteResource.getInsertFilePath());
		assertEquals(insertRestorePath, bufferwriteResource.getRestoreFilePath());
		bufferwriteResource.close(new FileSchema());
	}

	@Test
	public void testFlushAndGetMetadata() throws IOException {
		bufferwriteResource = new BufferWriteResource(processorName, insertRestorePath);
		assertEquals(0, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		IMemTable memTable = new TreeSetMemTable();
		MemTableTestUtils.produceData(memTable, 10, 100, MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0);
		bufferwriteResource.flush(MemTableTestUtils.getFileSchema(), memTable);
		assertEquals(0, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		bufferwriteResource.appendMetadata();
		assertEquals(1, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		MemTableTestUtils.produceData(memTable, 200, 300, MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0);
		bufferwriteResource.appendMetadata();
		assertEquals(1, bufferwriteResource.getInsertMetadatas(MemTableTestUtils.deltaObjectId0,
				MemTableTestUtils.measurementId0, MemTableTestUtils.dataType0).size());
		bufferwriteResource.close(MemTableTestUtils.getFileSchema());
	}

	private void writeRestoreFile(ITsRandomAccessFileWriter out, int metadataNum) throws IOException {
		TsRowGroupBlockMetaData tsRowGroupBlockMetaData = new TsRowGroupBlockMetaData();
		List<RowGroupMetaData> appendRowGroupMetaDatas = new ArrayList<>();
		for (int i = 0; i < metadataNum; i++) {
			appendRowGroupMetaDatas.add(new RowGroupMetaData("d1", 1000, 1000, new ArrayList<>(), "d1t"));
		}
		tsRowGroupBlockMetaData.setRowGroups(appendRowGroupMetaDatas);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ReadWriteThriftFormatUtils.writeRowGroupBlockMetadata(tsRowGroupBlockMetaData.convertToThrift(), baos);
		// write metadata size using int
		int metadataSize = baos.size();
		out.write(BytesUtils.intToBytes(metadataSize));
		// write metadata
		out.write(baos.toByteArray());
	}
}