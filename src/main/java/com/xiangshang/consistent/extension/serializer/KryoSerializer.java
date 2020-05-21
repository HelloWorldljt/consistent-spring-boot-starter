/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiangshang.consistent.extension.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.base.Stopwatch;
import com.xiangshang.consistent.extension.exception.ObjectSerializeException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * kryo序列化工具类
 *
 * @author chenrg
 * @date 2019年1月28日
 */
public final class KryoSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializer.class);

    /**
     * 对象序列化后的最大字节数。如果超过该值，表示对象过大，可能对应用内存造成风险。开发人员应调整被序列化对象大小。
     */
    private static final int MAX_SERIALIZED_SIZE = 4096;

    /**
     * 初始化kryo池，使用kryo原生的池
     */
    private static final KryoPool kryoPool = new KryoPool.Builder(new KryoFactory() {
        @Override
        public Kryo create() {
            return new Kryo();
        }
    }).softReferences().build();


    /**
     * 序列化.
     *
     * @param obj 需要序更列化的对象
     * @return 序列化后的byte 数组
     * @throws ObjectSerializeException 异常
     */
    public static final byte[] serialize(final Object obj) {
        Stopwatch sw = Stopwatch.createStarted();
        byte[] bytes;
        Kryo kryo = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Output output = new Output(outputStream);
            kryo = kryoPool.borrow();
            kryo.writeObject(output, obj);
            // 必须先调用close，它会将最后一次的缓冲区数据刷新到指定的outputStream中。
            output.close();
            bytes = outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("kryo serialize error", e);
        } finally {
            if (kryo != null) {
                kryoPool.release(kryo);
            }
        }
        // 如果序列化后的字节超过4096，就打印error日志，提醒开发者修改方法参数，降低要序列化对象大小
        if (ArrayUtils.getLength(bytes) > MAX_SERIALIZED_SIZE) {
            LOGGER.error("Serialized object is too big, it's large than 4096 bytes. Please check you object, and try to reduce the object size.");
        }
        sw.stop();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Kryo serialize object cost : [{}]", sw.toString());
        }
        return bytes;
    }

    /**
     * 反序列化.
     *
     * @param param 需要反序列化的byte []
     * @return 序列化对象
     * @throws ObjectSerializeException 异常
     */
    public static final <T> T deSerialize(final byte[] param, final Class<T> clazz) {
        Stopwatch sw = Stopwatch.createStarted();
        T object;
        Kryo kryo = null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(param); Input input = new Input(inputStream)) {
            kryo = kryoPool.borrow();
            object = kryo.readObject(input, clazz);
        } catch (Exception e) {
            throw new ObjectSerializeException("kryo deSerialize error : " + e.getMessage());
        } finally {
            if (kryo != null) {
                kryoPool.release(kryo);
            }
        }
        sw.stop();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Kryo deSerialize object cost : [{}]", sw.toString());
        }
        return object;
    }
}
