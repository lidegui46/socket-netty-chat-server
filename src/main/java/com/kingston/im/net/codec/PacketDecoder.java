package com.kingston.im.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import com.kingston.im.net.message.AbstractPacket;
import com.kingston.im.net.message.PacketManager;

/**
 * 通过“自定义长度解码器”解决TCP黏包问题
 * <pre>
 *     消息头：内容长度点位
 *     消息体：内容字节
 * </pre>
 */
public class PacketDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 通过自定义长度解码器 解决TCP黏包问题
     *
     * @param maxFrameLength      最大长度
     * @param lengthFieldOffset   长度字段的偏差
     * @param lengthFieldLength   长度字段占的字节数
     * @param lengthAdjustment    添加到长度字段的补偿值
     * @param initialBytesToStrip 从解码帧中第一次去除的字节数
     */
    public PacketDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment
            , int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    /**
     * 解码数据
     * <pre>
     *     return 的类型为”入站“读取消息时类型
     * </pre>
     *
     * @param ctx 管道处理器上下文
     * @param in  字节缓存冲区
     * @return 解码内容
     * @throws Exception
     */
    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        if (frame.readableBytes() <= 0) {
            return null;
        }

        // 获取事件行为编码
        int packetType = frame.readInt();
        // 实例化事件
        AbstractPacket packet = PacketManager.INSTANCE.createNewPacket(packetType);
        // 是否压缩
        boolean useCompression = packet.isUseCompression();
        // 从字节缓冲区读取原始数据
        ByteBuf realBuf = decompression(frame, useCompression);
        // 读取数据内容到实体中
        packet.readBody(realBuf);

        return packet;
    }

    /**
     * 解压数据
     *
     * @param sourceBuf      字节缓冲区 - 源
     * @param useCompression 数据是否压缩
     * @return
     * @throws Exception
     */
    private ByteBuf decompression(ByteBuf sourceBuf, boolean useCompression) throws Exception {
        if (!useCompression) {
            return sourceBuf;
        }

        // 从字节缓冲区读取到内存中
        int bodyLength = sourceBuf.readInt();    //先读压缩数据的长度
        byte[] sourceBytes = new byte[bodyLength];
        sourceBuf.readBytes(sourceBytes); //得到压缩数据的字节数组

        // 从内存 到 解压缩
        //解压缩
        ByteArrayInputStream bis = new ByteArrayInputStream(sourceBytes);
        GZIPInputStream gzip = new GZIPInputStream(bis);

        // 读取原数据到内存中
        final int MAX_MSG_LENGTH = bodyLength * 2;  //假设压缩率最大为100%！！！！！
        byte[] content = new byte[MAX_MSG_LENGTH];
        int num = -1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((num = gzip.read(content, 0, content.length)) != -1) {
            baos.write(content, 0, num);
        }
        baos.flush();
        gzip.close();
        bis.close();

        // 原数据写到字节缓冲区
        //重新封装成ByteBuf对象
        ByteBuf resultBuf = Unpooled.buffer();
        byte[] realBytes = baos.toByteArray();  //压缩前的实际数据
        resultBuf.writeBytes(realBytes);
        baos.close();

        return resultBuf;
    }

}
