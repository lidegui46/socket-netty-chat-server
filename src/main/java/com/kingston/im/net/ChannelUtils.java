package com.kingston.im.net;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * channel的工具类
 *
 * @author kingston
 */
public final class ChannelUtils {

    public static AttributeKey<IoSession> SESSION_KEY = AttributeKey.valueOf("session");

    /**
     * 添加通道新会话
     * <pre>
     *     始终覆盖
     * </pre>
     *
     * @param channel 通道
     * @param session 会话
     * @return
     */
    public static boolean addChannelSession(Channel channel, IoSession session) {
        Attribute<IoSession> sessionAttr = channel.attr(SESSION_KEY);
        return sessionAttr.compareAndSet(null, session);
    }

    /**
     * 获取通道会话
     *
     * @param channel 通道
     * @return
     */
    public static IoSession getSessionBy(Channel channel) {
        Attribute<IoSession> sessionAttr = channel.attr(SESSION_KEY);
        return sessionAttr.get();
    }

    /**
     * 获取通过IP
     *
     * @param channel 通道
     * @return
     */
    public static String getIp(Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress().toString().substring(1);
    }
}
