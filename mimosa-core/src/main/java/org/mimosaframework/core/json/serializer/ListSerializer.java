/*
 * Copyright 1999-2101 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mimosaframework.core.json.serializer;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public final class ListSerializer implements ObjectSerializer {

    public static final ListSerializer instance = new ListSerializer();

    public final void write(ModelSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
                                                                                                       throws IOException {

        boolean writeClassName = serializer.out.isEnabled(SerializerFeature.WriteClassName);

        SerializeWriter out = serializer.out;

        Type elementType = null;
        if (writeClassName) {
            if (fieldType instanceof ParameterizedType) {
                ParameterizedType param = (ParameterizedType) fieldType;
                elementType = param.getActualTypeArguments()[0];
            }
        }

        if (object == null) {
            out.writeNull(SerializerFeature.WriteNullListAsEmpty);
            return;
        }

        List<?> list = (List<?>) object;

        if (list.size() == 0) {
            out.append("[]");
            return;
        }

        SerialContext context = serializer.context;
        serializer.setContext(context, object, fieldName, 0);

        ObjectSerializer itemSerializer = null;
        try {
            if (out.isEnabled(SerializerFeature.PrettyFormat)) {
                out.append('[');
                serializer.incrementIndent();

                int i = 0;
                for (Object item : list) {
                    if (i != 0) {
                        out.append(',');
                    }

                    serializer.println();
                    if (item != null) {
                        if (serializer.containsReference(item)) {
                            serializer.writeReference(item);
                        } else {
                            itemSerializer = serializer.getObjectWriter(item.getClass());
                            SerialContext itemContext = new SerialContext(context, object, fieldName, 0, 0);
                            serializer.context = itemContext;
                            itemSerializer.write(serializer, item, i, elementType, 0);
                        }
                    } else {
                        serializer.out.writeNull();
                    }
                    i++;
                }

                serializer.decrementIdent();
                serializer.println();
                out.append(']');
                return;
            }

            out.append('[');
            for (int i = 0, size = list.size(); i < size; ++i) {
                Object item = list.get(i);
                if (i != 0) {
                    out.append(',');
                }
                
                if (item == null) {
                    out.append("null");
                } else {
                    Class<?> clazz = item.getClass();

                    if (clazz == Integer.class) {
                        out.writeInt(((Integer) item).intValue());
                    } else if (clazz == Long.class) {
                        long val = ((Long) item).longValue();
                        if (writeClassName) {
                            out.writeLong(val);
                            out.write('L');
                        } else {
                            out.writeLong(val);
                        }
                    } else {
                        if (!out.disableCircularReferenceDetect) {
                            SerialContext itemContext = new SerialContext(context, object, fieldName, 0, 0);
                            serializer.context = itemContext;
                        }

                        if (serializer.containsReference(item)) {
                            serializer.writeReference(item);
                        } else {
                            itemSerializer = serializer.getObjectWriter(item.getClass());
                            itemSerializer.write(serializer, item, i, elementType, 0);
                        }
                    }
                }
            }
            out.append(']');
        } finally {
            serializer.context = context;
        }
    }

}