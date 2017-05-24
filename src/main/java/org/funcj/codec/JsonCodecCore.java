package org.funcj.codec;

import org.funcj.control.Exceptions;
import org.funcj.data.IList;
import org.funcj.json.Node;

import java.lang.reflect.Array;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.funcj.control.Exceptions.TODO;

public class JsonCodecCore extends CodecCore<Node> {

    protected String typeFieldName() {
        return "@type";
    }

    protected String keyFieldName() {
        return "@key";
    }

    protected String valueFieldName() {
        return "@value";
    }

    private final Codec.NullCodec<Node> nullCodec = new Codec.NullCodec<Node>() {
        @Override
        public boolean isNull(Node in) {
            return in.isNull();
        }

        @Override
        public Node encode(Object val, Node out) {
            return Node.nul();
        }

        @Override
        public Object decode(Node in) {
            in.asNull();
            return null;
        }
    };

    @Override
    protected Codec.NullCodec<Node> nullCodec() {
        return nullCodec;
    }

    private final Codec.BooleanCodec<Node> booleanCodec = new Codec.BooleanCodec<Node>() {

        @Override
        Node encodePrim(boolean val, Node out) {
            return Node.bool(val);
        }

        @Override
        public boolean decodePrim(Node in) {
            return in.asBool().value;
        }
    };

    @Override
    protected Codec.BooleanCodec<Node> booleanCodec() {
        return booleanCodec;
    }

    private final Codec.BooleanArrayCodec<Node> booleanArrayCodec = new Codec.BooleanArrayCodec<Node>() {

        @Override
        public Node encode(boolean[] vals, Node out) {
            final List<Node> nodes = new ArrayList<>(vals.length);
            for (boolean val : vals) {
                nodes.add(booleanCodec.encode(val, out));
            }
            return Node.array(nodes);
        }

        @Override
        public boolean[] decode(Node in) {
            final Node.ArrayNode arrNode = in.asArray();
            final boolean[] vals = new boolean[arrNode.values.size()];
            int i = 0;
            for (Node node : arrNode.values) {
                vals[i++] = booleanCodec.decode(node);
            }
            return vals;
        }
    };

    @Override
    protected Codec.BooleanArrayCodec<Node> booleanArrayCodec() {
        return booleanArrayCodec;
    }

    private final Codec.IntegerCodec<Node> integerCodec = new Codec.IntegerCodec<Node>() {

        @Override
        Node encodePrim(int val, Node out) {
            return Node.number(val);
        }

        @Override
        public int decodePrim(Node in) {
            return (int)in.asNumber().value;
        }
    };

    @Override
    protected Codec.IntegerCodec<Node> integerCodec() {
        return integerCodec;
    }

    private final Codec<int[], Node> integerArrayCodec = new Codec<int[], Node>() {

        @Override
        public Node encode(int[] vals, Node out) {
            final List<Node> nodes = new ArrayList<>(vals.length);
            for (int val : vals) {
                nodes.add(integerCodec.encode(val, out));
            }
            return Node.array(nodes);
        }

        @Override
        public int[] decode(Node in) {
            final Node.ArrayNode arrNode = in.asArray();
            final int[] vals = new int[arrNode.values.size()];
            int i = 0;
            for (Node node : arrNode.values) {
                vals[i++] = integerCodec.decode(node);
            }
            return vals;
        }
    };

    @Override
    protected Codec<int[], Node> integerArrayCodec() {
        return integerArrayCodec;
    }

    @Override
    protected <K, V> Codec<Map<K, V>, Node> mapCodec(
            CodecCore<Node> core,
            Class<Map<K, V>> stcClass,
            Class<K> stcKeyClass,
            Class<V> stcValClass) {
        return new Codec.CodecBase<Map<K, V>, Node>(core, stcClass) {
            @Override
            public Node encode(Map<K, V> val, Node out) {
                if (val == null) {
                    return core.nullCodec().encode(val, out);
                } else {
                    final String typeFieldName = typeFieldName();
                    final String keyFieldName = keyFieldName();
                    final String valueFieldName = valueFieldName();
                    final LinkedHashMap<String, Node> fields = new LinkedHashMap<>();
                    fields.put(typeFieldName, Node.string(classToName(val.getClass())));

                    final List<Node> nodes = val.entrySet().stream()
                            .map(en -> {
                                final LinkedHashMap<String, Node> elemFields = new LinkedHashMap<>();
                                elemFields.put(keyFieldName, core.encode(stcKeyClass, en.getKey(), out));
                                elemFields.put(valueFieldName, core.encode(stcValClass, en.getValue(), out));
                                return Node.object(elemFields);
                            }).collect(toList());
                    fields.put(valueFieldName(), Node.array(nodes));
                    return Node.object(fields);
                }
            }

            @Override
            public Map<K, V> decode(Node in) {
                if (in.isNull()) {
                    return (Map<K, V>)nullCodec.decode(in);
                } else {
                    final String typeFieldName = typeFieldName();
                    final String keyFieldName = keyFieldName();
                    final String valueFieldName = valueFieldName();
                    final Node.ObjectNode objNode = in.asObject();
                    final Class<?> clazz = nameToClass(objNode.fields.get(typeFieldName).asString().value);
                    final Map<K, V> map = (Map<K, V>) Exceptions.wrap(() -> clazz.newInstance());
                    objNode.fields.get(valueFieldName()).asArray().values.forEach(elemNode -> {
                        final Node.ObjectNode elemObjNode = elemNode.asObject();
                        final K key = core.decode(stcKeyClass, elemObjNode.fields.get(keyFieldName));
                        final V val = core.decode(stcValClass, elemObjNode.fields.get(valueFieldName));
                        map.put(key, val);
                    });
                    return map;
                }
            }
        };
    }

    @Override
    protected <T> Codec.DynamicCodec<T, Node> dynamicCodec(Class<T> stcClass) {
        return new Codec.DynamicCodec<T, Node>(this, stcClass) {
            @Override
            public Node encode(T val, Node out) {
                if (val == null) {
                    return core.nullCodec().encode(val, out);
                } else {
                    final Class<? extends T> dynClass = (Class<? extends T>)val.getClass();
                    final Codec<Object, Node> codec = core.getCodec(stcClass, (Class)val.getClass());
                    if (dynClass == stcClass) {
                        return codec.encode(val, out);
                    } else {
                        final LinkedHashMap<String, Node> fields = new LinkedHashMap<>();
                        fields.put(typeFieldName(), Node.string(classToName(dynClass)));
                        fields.put(valueFieldName(), codec.encode(val, out));
                        return Node.object(fields);
                    }
                }
            }

            @Override
            public T decode(Node in) {
                Class<? extends T> dynClass = stcClass;
                if (in.isNull()) {
                    return (T)nullCodec.decode(in);
                } else if (in.isObject()) {
                    final Node.ObjectNode objNode = in.asObject();
                    final Node typeNode = objNode.fields.get(typeFieldName());
                    if (typeNode != null) {
                        dynClass = nameToClass(typeNode.asString().value);
                    }

                    final Node valueNode = objNode.fields.get(valueFieldName());
                    if (valueNode != null) {
                        return core.getCodec(stcClass, dynClass).decode(valueNode);
                    }
                }

                return core.getCodec(stcClass, dynClass).decode(in);
            }

            @Override
            protected Class<? extends T> getType(Node in) {
                if (in.isObject()) {
                    final Node.ObjectNode objNode = in.asObject();
                    final String typeFieldName = typeFieldName();
                    final Node typeNode = objNode.fields.get(typeFieldName);
                    if (typeNode != null) {
                        return nameToClass(typeNode.asString().value);
                    }
                }

                return stcClass;
            }
        };
    }

    @Override
    protected <T> Codec<T[], Node> objectArrayCodec(Class<T> elemClass, Codec<T, Node> elemCodec) {
        return new Codec<T[], Node>() {
            @Override
            public Node encode(T[] vals, Node out) {
                final List<Node> nodes = new ArrayList<>(vals.length);
                for (T val : vals) {
                    nodes.add(elemCodec.encode(val, out));
                }
                return Node.array(nodes);
            }

            @Override
            public T[] decode(Node in) {
                final Node.ArrayNode arrNode = in.asArray();
                final T[] vals = (T[]) Array.newInstance(elemClass, arrNode.values.size());
                int i = 0;
                for (Node node : arrNode.values) {
                    vals[i++] = elemCodec.decode(node);
                }
                return vals;
            }
        };
    }

    @Override
    protected <T> Codec<T, Node> createObjectCodec(
            Class<T> stcClass,
            Class<? extends T> dynClass,
            Map<String, FieldCodec<Node>> fieldCodecs) {
        return new Codec<T, Node>() {
            @Override
            public Node encode(T val, Node out) {
                if (val == null) {
                    return nullCodec.encode(val, out);
                } else {
                    final Class<? extends T> dynClass = (Class<? extends T>)val.getClass();
                    final LinkedHashMap<String, Node> fields = new LinkedHashMap<>();
                    if (dynClass != stcClass) {
                        fields.put(typeFieldName(), Node.string(classToName(dynClass)));
                    }
                    fieldCodecs.forEach((name, codec) -> fields.put(name, codec.encode(val, out)));
                    return Node.object(fields);
                }
            }

            @Override
            public T decode(Node in) {
                if (in.isNull()) {
                    return (T)nullCodec.decode(in);
                } else {
                    final Node.ObjectNode objNode = in.asObject();
                    final String typeFieldName = typeFieldName();
                    final Class<? extends T> type;
                    final Node typeNode = objNode.fields.get(typeFieldName);
                    if (typeNode != null) {
                        type = nameToClass(typeNode.asString().value);
                    } else {
                        type = stcClass;
                    }
                    final T val = (T) Exceptions.wrap(() -> type.newInstance());
                    fieldCodecs.forEach((name, codec) -> {
                        if (!name.equals(typeFieldName)) {
                            codec.decode(val, objNode.fields.get(name));
                        }
                    });
                    return val;
                }
            }
        };
    }
}