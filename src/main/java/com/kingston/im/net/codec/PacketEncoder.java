package com.kingston.im.net.codec;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kingston.im.net.message.AbstractPacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 编码
 */
public class PacketEncoder extends MessageToByteEncoder<AbstractPacket> {

    private Logger logger = LoggerFactory.getLogger(PacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, AbstractPacket msg, ByteBuf out)
            throws Exception {
        try {
            _encode(ctx, msg, out);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * 编码
     *
     * @param ctx 管道处理器上下文
     * @param msg 编码内容
     * @param out 输出字节缓冲区
     * @throws Exception
     */
    private void _encode(ChannelHandlerContext ctx, AbstractPacket msg, ByteBuf out) throws Exception {
        //消息头
        out.writeInt(msg.getPacketType().getType());

        //消息体
        if (msg.isUseCompression()) {  //开启gzip压缩
            // 内容写到字节缓存区
            ByteBuf buf = Unpooled.buffer();
            msg.writeBody(buf);

            // 字节缓冲区 写到 字节数组（内存)
            byte[] content = new byte[buf.readableBytes()];
            buf.getBytes(0, content);

            // 字节数组输出流
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // 压缩内容并写入到节字数组输出流
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(content);
            gzip.close();

            // 字节数组输出流 转换为 字节数组
            byte[] destBytes = bos.toByteArray();

            // 压缩后的内容写入到输出字节缓冲区
            out.writeInt(destBytes.length);
            out.writeBytes(destBytes);

            // 关闭字节数组输出流
            bos.close();
        } else {
            msg.writeBody(out);
        }
    }

}
