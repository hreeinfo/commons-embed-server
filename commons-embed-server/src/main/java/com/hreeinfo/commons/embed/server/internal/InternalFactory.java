package com.hreeinfo.commons.embed.server.internal;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 单例工厂类 用于构建单例对象
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/30 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public final class InternalFactory {
    private static final Logger LOG = Logger.getLogger(BaseEmbedServer.class.getName());
    private static final Object NULL_OBJECT = new Object();
    private static final InternalFactory INSTANCE = new InternalFactory();

    private final Map<String, Supplier<?>> providers = new ConcurrentHashMap<>();
    private final Map<String, Object> objects = new ConcurrentHashMap<>();

    /**
     * 获取实例
     * @return
     */
    public static InternalFactory inst() {
        return INSTANCE;
    }

    private InternalFactory() {
    }

    private <T> T newInstance(Class<T> type) {
        if (type == null) return null;

        try {
            return ConstructorUtils.invokeConstructor(type);
        } catch (Throwable e) {
            LOG.warning("通过类型 " + type.getName() + " 无法直接构建对象（可能是对象没有默认构造方法或者构造过程出错） " + e.getMessage());
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    private <T> T loadByProvider(String name) {
        if (name == null) return null;
        Supplier<T> op = (Supplier<T>) this.providers.get(name);
        if (op != null) return op.get();
        return null;
    }


    /**
     * 载入单例对象
     * <p>
     * 当载入的单例对象是 NULL_OBJECT 且 forceLoad=true 时会尝试再次载入
     *
     * @param name
     * @param forceLoad
     * @param provider
     * @return
     */
    private Object doGet(String name, boolean forceLoad, Supplier<?> provider) {
        if (name == null) return null;

        Object obj = this.objects.get(name);
        if (forceLoad && obj == NULL_OBJECT) obj = null;
        if (obj == null) {
            synchronized (this.objects) {
                obj = this.objects.get(name);
                if (forceLoad && obj == NULL_OBJECT) obj = null;
                if (obj == null) {
                    if (provider != null) obj = provider.get();

                    if (obj == null) obj = NULL_OBJECT;
                    this.objects.put(name, obj);
                }
            }
        }

        return obj == NULL_OBJECT ? null : obj;
    }


    /**
     * 增加provider
     *
     * @param name
     * @param provider
     */
    public void provider(String name, Supplier<?> provider) {
        if (name == null || provider == null) return;

        synchronized (this.objects) {
            this.providers.put(name, provider);
        }
    }

    /**
     * 获取单例对象 指定对象类型 如果对象不存在则使用给定的provider获取目标对象
     * <p>
     * 如果provider为空，则尝试使用类的默认构建方法构造对象
     *
     * @param name
     * @param type
     * @param provider
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String name, final Class<T> type, final Supplier<T> provider) {
        Object obj = this.doGet(name, false, provider);
        if (obj == null || obj == NULL_OBJECT) {
            // 二次检查 尝试使用默认构建方式构建
            obj = this.doGet(name, true, () -> this.newInstance(type));
            if (obj == null || obj == NULL_OBJECT) return null;
        }

        if (type != null) {
            if (type.isInstance(obj)) return (T) obj;
            throw new ClassCastException("生成的对象不兼容类型 " + type.getName());
        }

        return (T) obj;
    }

    public <T> T get(final String name, final Class<T> type) {
        return this.get(name, type, () -> this.loadByProvider(name));
    }


    /**
     * 获取单例对象 如果对象不存在则使用给定的provider获取目标对象
     *
     * @param name
     * @param provider
     * @return
     */
    public Object get(final String name, final Supplier<?> provider) {
        return this.doGet(name, false, provider);
    }

    /**
     * 获取单例对象 如果定义了name对应的provider则使用它构造对象并返回
     * <p>
     * 如果provider未定义，返回空
     *
     * @param name
     * @return
     */
    public Object get(String name) {
        return this.get(name, () -> this.loadByProvider(name));
    }
}
