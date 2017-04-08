package com.boydti.rededit.remote;

import com.boydti.fawe.object.RunnableVal2;
import com.boydti.rededit.RedEdit;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.listener.RedEditPubSub;
import com.boydti.rededit.serializer.DefaultSerializer;
import com.boydti.rededit.serializer.Serializer;
import com.boydti.rededit.util.MapUtil;
import com.google.common.cache.LoadingCache;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;


import static com.google.common.base.Preconditions.checkNotNull;

public abstract class RemoteCall<Result, Argument> {

    private LongAdder sequence = new LongAdder();

    private final Class<Result> resType;
    private final Class<Argument> argType;

    private final RunMode mode;
    private final int bufferSize;
    private final int timout;
    private final int id;
    private final RemoteCallException e;

    LoadingCache<Integer, RunnableVal2<Server, Result>> cache;

    public Serializer argumentSerializer;
    public Serializer resultSerializer;

    public enum RunMode {
        // Can run immediately on any thread
        ASYNC,
        // Can only run on the main thread
        SYNC,
        // Can run in parallel with the main threead
        PARALLEL
    }

    public enum Type {
        RESULT,
        ARGUMENT
    }

    /**
     * this(RunMode.PARALLEL, 4096, 10000);
     */
    public RemoteCall() {
        this(RunMode.PARALLEL, 4096, 10000);
    }

    public RemoteCall(int id) {
        this(id, RunMode.PARALLEL, 4096, 10000);
    }

    /**
     * RunMode:<br>
     *  - Async - Runs on any thread<br>
     *  - Sync - Runs on the main thread<br>
     *  - Parallel - Blocks the main thread, but runs multiple tasks at a time<br>
     * Compression:<br>
     *  - None - Small amounts of data or data that doesn't compress well<br>
     *  - Fast - Large amounts of data, need a quick response or the data may not compress well<br>
     *  - High - Large amounts of data that compresses well = worth spending more time compressing<br>
     * @param mode How the task will run on the remote server
     */
    public RemoteCall(RunMode mode, int bufferSize, int timout) {
        this(0, mode, bufferSize, timout);
    }

    public RemoteCall(int id, RunMode mode, int bufferSize, int timout) {
        this.id = id == 0 ? getClass().hashCode() : id;
        this.timout = timout;
        this.mode = mode;
        this.bufferSize = bufferSize;
        ParameterizedType generics = ((ParameterizedType) getClass().getGenericSuperclass());
        this.resType = (Class<Result>) generics.getActualTypeArguments()[0];
        this.argType = (Class<Argument>) generics.getActualTypeArguments()[1];
        RedEdit.get().getScheduler().registerPacket(this);
        e = new RemoteCallException();
        cache = MapUtil.getExpiringMap(10, TimeUnit.SECONDS);
        argumentSerializer = new DefaultSerializer();
        resultSerializer = new DefaultSerializer();
    }

    public RemoteCall<Result, Argument> setSerializer(Serializer<?> resultSerializer, Serializer<?> argumentSerializer) {
        checkNotNull(resultSerializer);
        checkNotNull(argumentSerializer);
        this.argumentSerializer = argumentSerializer;
        this.resultSerializer = resultSerializer;
        return this;
    }

    public RemoteCall<Result, Argument> setArgumentSerializer(Serializer<?> argumentSerializer) {
        checkNotNull(argumentSerializer);
        this.argumentSerializer = argumentSerializer;
        return this;
    }

    public RemoteCall<Result, Argument> setResultSerializer(Serializer<?> resultSerializer) {
        checkNotNull(resultSerializer);
        this.resultSerializer = resultSerializer;
        return this;
    }

    private static class RemoteCallException extends RuntimeException {
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    public final int getId() {
        return id;
    }

    public final int getTimout() {
        return timout;
    }

    public final RunMode getRunMode() {
        return mode;
    }

    public final int getBufferSize() {
        return bufferSize;
    }

    public final Class<Argument> getArgType() {
        return argType;
    }

    public final Class<Result> getResType() {
        return resType;
    }

    private boolean write(OutputStream out, Object value, Type resOrArg, int sequence) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(out);
        writeHeader(dataOut, resOrArg.ordinal(), sequence);
        if (value != null) {
            writeObject(dataOut, value, resOrArg);
        }
        out.close();
        return true;
    }

    /**
     * channel = destination<br>
     * 4 = sender<br>
     * 4 = method<br>
     * 2 = sequence number<br>
     * 1 = send or receive<br>
     * # = data<br>
     * @param out
     * @param mode
     * @throws IOException
     */
    private void writeHeader(DataOutputStream out, int mode, int sequence) throws IOException {
        out.writeShort(Settings.IMP.SERVER_GROUP);
        out.writeShort(Settings.IMP.SERVER_ID);
        out.writeInt(getId());
        out.writeShort(sequence);
        out.write(mode);
    }

    public final short getSequenceNumber() {
        sequence.add(1);
        return sequence.shortValue();
    }

    public void writeObject(DataOutputStream out, Object value, Type resOrArg) throws IOException {
        try {
            switch (resOrArg) {
                case RESULT:
                    resultSerializer.write(out, value);
                    break;
                case ARGUMENT:
                    argumentSerializer.write(out, value);
                    break;
            }
        } finally {
            out.close();
        }
    }

    private Object read(DataInputStream dataIn, Type resOrArg) throws IOException, ClassNotFoundException {
        if (dataIn.available() == 0) {
            return null;
        }
        return readObject(dataIn, resOrArg);
    }

    public Object readObject(DataInputStream in, Type resOrArg) throws IOException, ClassNotFoundException {
        try {
            switch (resOrArg) {
                case RESULT:
                    return resultSerializer.read(in);
                case ARGUMENT:
                    return argumentSerializer.read(in);
            }
        } finally {
            in.close();
        }
        return null;
    }

    public abstract Result run(Server sender, Argument arg);

    public void result(Server sender, int sequence, Result result) throws IOException {
        RunnableVal2<Server, Result> runnable = cache.getIfPresent(sequence);
        if (runnable != null) {
            runnable.run(sender, result);
        }
    }

    public void argument(Server server, int sequence, Argument arg) throws IOException {
        Result result = run(server, arg);
        RedEditPubSub scheduler = RedEdit.get().getScheduler();
        if (result != null) {
            OutputStream os = scheduler.getOS(server.getChannel());
            write(os, result, Type.RESULT, sequence);
        }
    }

    public void call(int withGroup, int withServerId, Argument arg) {
        call(withGroup, withServerId, arg, null);
    }

    public void call(int withGroup, int withServerId, Argument arg, RunnableVal2<Server, Result> onResult) {
        try {
            RedEditPubSub scheduler = RedEdit.get().getScheduler();
            OutputStream os = scheduler.getOS(Channel.getId(withGroup, withServerId));
            int sequence = getSequenceNumber();
            if (onResult != null) cache.put(sequence, onResult);
            write(os, arg, Type.ARGUMENT, sequence);
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}