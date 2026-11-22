package net.minecraft.a.c;

import java.util.HashMap;
import java.util.Map;

/**
 * 假 EntityList（EntityList 双轨批）：复刻 0618 锚 net.minecraft.a.c.e 的
 * static a(Entity)String=b.get(getClass()) 查表面——WorldOps.entityLookup 按
 * 描述符特征筛（static+String 返回+单参）命中本方法。未注册返 null→fallback。
 */
public final class e {

    public static final Map<Object, Object> names = new HashMap<Object, Object>();

    public static String a(Object entity) {
        Object n = names.get(entity.getClass());
        return n == null ? null : (String) n;
    }
}
