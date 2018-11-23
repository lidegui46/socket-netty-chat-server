package com.kingston.im.net.message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.kingston.im.net.IoSession;

public enum PacketManager {

    INSTANCE;

    /**
     * 执行事件方法
     *
     * @param session 会话
     * @param pact    事件抽象类
     */
    public void execPacket(IoSession session, AbstractPacket pact) {
        if (pact == null) return;
        try {
            Method m = pact.getClass().getMethod("execPacket", IoSession.class);
            m.invoke(pact, session);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新建新的事件
     *
     * @param packetType 事件类型
     * @return
     */
    public AbstractPacket createNewPacket(int packetType) {
        Class<? extends AbstractPacket> packetClass = PacketType.getPacketClassBy(packetType);
        if (packetClass == null) {
            throw new IllegalPacketException("类型为" + packetType + "的包定义不存在");
        }
        AbstractPacket packet = null;
        try {
            packet = (AbstractPacket) packetClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalPacketException("类型为" + packetType + "的包实例化失败");
        }

        return packet;
    }

}
