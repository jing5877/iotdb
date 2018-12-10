package cn.edu.tsinghua.iotdb.writelog.transfer;

import cn.edu.tsinghua.iotdb.conf.TsfileDBConfig;
import cn.edu.tsinghua.iotdb.conf.TsfileDBDescriptor;
import cn.edu.tsinghua.iotdb.qp.physical.crud.DeletePlan;
import cn.edu.tsinghua.iotdb.qp.physical.crud.InsertPlan;
import cn.edu.tsinghua.iotdb.qp.physical.crud.UpdatePlan;
import cn.edu.tsinghua.tsfile.read.common.Path;
import cn.edu.tsinghua.tsfile.utils.BytesUtils;
import cn.edu.tsinghua.tsfile.utils.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public enum PhysicalPlanCodec {

    MULTIINSERTPLAN(SystemLogOperator.INSERT, codecInstances.multiInsertPlanCodec),
    UPDATEPLAN(SystemLogOperator.UPDATE, codecInstances.updatePlanCodec),
    DELETEPLAN(SystemLogOperator.DELETE, codecInstances.deletePlanCodec);

    private static TsfileDBConfig config = TsfileDBDescriptor.getInstance().getConfig();

    public final int planCode;
    public final Codec<?> codec;

    PhysicalPlanCodec(int planCode, Codec<?> codec) {
        this.planCode = planCode;
        this.codec = codec;
    }

    private static final HashMap<Integer, PhysicalPlanCodec> codecMap = new HashMap<>();

    static {
        for (PhysicalPlanCodec codec : PhysicalPlanCodec.values()) {
            codecMap.put(codec.planCode, codec);
        }
    }

    public static PhysicalPlanCodec fromOpcode(int opcode) {
        if (!codecMap.containsKey(opcode)) {
            throw new UnsupportedOperationException("SystemLogOperator [" + opcode + "] is not supported. " );
        }
        return codecMap.get(opcode);
    }

    static class codecInstances {

        static final Codec<DeletePlan> deletePlanCodec = new Codec<DeletePlan>() {
            ThreadLocal<ByteBuffer> localBuffer = new ThreadLocal<>();

            @Override
            public byte[] encode(DeletePlan t) {
                if (localBuffer.get() == null)
                    localBuffer.set(ByteBuffer.allocate(config.maxLogEntrySize));

                int type = SystemLogOperator.DELETE;
                ByteBuffer buffer = localBuffer.get();
                buffer.clear();
                buffer.put((byte) type);
                buffer.putLong(t.getDeleteTime());
                byte[] pathBytes = BytesUtils.StringToBytes(t.getPaths().get(0).getFullPath());
                buffer.putInt(pathBytes.length);
                buffer.put(pathBytes);

                return Arrays.copyOfRange(buffer.array(), 0, buffer.position());
            }

            @Override
            public DeletePlan decode(byte[] bytes) throws IOException {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                int type = buffer.get();
                long time = buffer.getLong();

                int pathLength = buffer.getInt();
                byte[] pathBytes = new byte[pathLength];
                buffer.get(pathBytes, 0, pathLength);
                String path = BytesUtils.bytesToString(pathBytes);

                return new DeletePlan(time, new Path(path));
            }
        };

        static final Codec<UpdatePlan> updatePlanCodec = new Codec<UpdatePlan>() {
            ThreadLocal<ByteBuffer> localBuffer = new ThreadLocal<>();

            @Override
            public byte[] encode(UpdatePlan updatePlan) {
                int type = SystemLogOperator.UPDATE;
                if (localBuffer.get() == null)
                    localBuffer.set(ByteBuffer.allocate(config.maxLogEntrySize));

                ByteBuffer buffer = localBuffer.get();
                buffer.clear();
                buffer.put((byte) type);
                buffer.putInt(updatePlan.getIntervals().size());
                for (Pair<Long, Long> pair : updatePlan.getIntervals()) {
                    buffer.putLong(pair.left);
                    buffer.putLong(pair.right);
                }

                byte[] valueBytes = BytesUtils.StringToBytes(updatePlan.getValue());
                buffer.putInt(valueBytes.length);
                buffer.put(valueBytes);

                byte[] pathBytes = BytesUtils.StringToBytes(updatePlan.getPath().getFullPath());
                buffer.putInt(pathBytes.length);
                buffer.put(pathBytes);

                return Arrays.copyOfRange(buffer.array(), 0, buffer.position());
            }

            @Override
            public UpdatePlan decode(byte[] bytes) throws IOException {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                int type = buffer.get();

                int timeListBytesLength = buffer.getInt();
                List<Pair<Long, Long>> timeArrayList = new ArrayList<>(timeListBytesLength);
                for (int i = 0; i < timeListBytesLength; i++) {
                    long startTime = buffer.getLong();
                    long endTime = buffer.getLong();
                    timeArrayList.add(new Pair<>(startTime, endTime));
                }

                int valueLength = buffer.getInt();
                byte[] valueBytes = new byte[valueLength];
                buffer.get(valueBytes);
                String value = BytesUtils.bytesToString(valueBytes);

                int pathLength = buffer.getInt();
                byte[] pathBytes = new byte[pathLength];
                buffer.get(pathBytes);
                String path = BytesUtils.bytesToString(pathBytes);

                return new UpdatePlan(timeArrayList, value, new Path(path));
            }
        };

        static final Codec<InsertPlan> multiInsertPlanCodec = new Codec<InsertPlan>() {
            ThreadLocal<ByteBuffer> localBuffer = new ThreadLocal<>();

            @Override
            public byte[] encode(InsertPlan plan) {
                int type = SystemLogOperator.INSERT;
                if (localBuffer.get() == null)
                    localBuffer.set(ByteBuffer.allocate(config.maxLogEntrySize));
                ByteBuffer buffer = localBuffer.get();
                buffer.clear();
                buffer.put((byte) type);
                buffer.put((byte) plan.getInsertType());
                buffer.putLong(plan.getTime());

                byte[] deltaObjectBytes = BytesUtils.StringToBytes(plan.getDeltaObject());
                buffer.putInt(deltaObjectBytes.length);
                buffer.put(deltaObjectBytes);

                List<String> measurementList = plan.getMeasurements();
                buffer.putInt(measurementList.size());
                for (String m : measurementList) {
                    byte[] mBytes = BytesUtils.StringToBytes(m);
                    buffer.putInt(mBytes.length);
                    buffer.put(mBytes);
                }

                List<String> valueList = plan.getValues();
                buffer.putInt(valueList.size());
                for (String m : valueList) {
                    byte[] vBytes = BytesUtils.StringToBytes(m);
                    buffer.putInt(vBytes.length);
                    buffer.put(vBytes);
                }

                return Arrays.copyOfRange(buffer.array(), 0, buffer.position());
            }

            @Override
            public InsertPlan decode(byte[] bytes) throws IOException {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);

                int type = buffer.get();
                int insertType = buffer.get();
                long time = buffer.getLong();

                int deltaObjLen = buffer.getInt();
                byte[] deltaObjBytes = new byte[deltaObjLen];
                buffer.get(deltaObjBytes);
                String deltaObject = BytesUtils.bytesToString(deltaObjBytes);

                int mmListLength = buffer.getInt();
                List<String> measurementsList = new ArrayList<>(mmListLength);
                for(int i = 0; i < mmListLength; i++) {
                    int mmLen = buffer.getInt();
                    byte[] mmBytes = new byte[mmLen];
                    buffer.get(mmBytes);
                    measurementsList.add(BytesUtils.bytesToString(mmBytes));
                }

                int valueListLength = buffer.getInt();
                List<String> valuesList = new ArrayList<>(valueListLength);
                for(int i = 0; i < valueListLength; i++) {
                    int valueLen = buffer.getInt();
                    byte[] valueBytes = new byte[valueLen];
                    buffer.get(valueBytes);
                    valuesList.add(BytesUtils.bytesToString(valueBytes));
                }

                InsertPlan ans = new InsertPlan(deltaObject, time, measurementsList, valuesList);
                ans.setInsertType(insertType);
                return ans;
            }
        };

    }
}