package com.xingling.scenerystaff.client;

import com.xingling.scenerystaff.sa.SaCharge;

/**
 * 客户端侧的 SA 充能缓存。
 * <p>
 * 服务端每次改动充能后会推一条 {@code SyncChargeMessage}，这里存下最新值，HUD 只读这里。
 * 之所以不直接读客户端玩家实体上的能力：能力数据不会从服务端自动同步，
 * 客户端本地那份永远是 0。
 * <p>
 * 本类<b>只在客户端被加载</b>（由 {@code SyncChargeMessage.handle} 反射调用），
 * 因此专用服务端不会因它而加载任何客户端类。
 */
public final class ChargeClientCache {

    private static volatile int domainCharge;
    private static volatile int giantSwordCharge;

    private ChargeClientCache() {
    }

    public static void accept(int domain, int giantSword) {
        domainCharge = domain;
        giantSwordCharge = giantSword;
    }

    /** 取某条充能在客户端的镜像值 */
    public static int get(SaCharge.Charge charge) {
        return charge == SaCharge.DOMAIN ? domainCharge : giantSwordCharge;
    }

    /** 换存档 / 断线时清空 */
    public static void clear() {
        domainCharge = 0;
        giantSwordCharge = 0;
    }
}
