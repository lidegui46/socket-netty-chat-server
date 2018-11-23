package com.kingston.im.dispatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.kingston.im.util.NamedThreadFactory;

@Component
public class MessageDispatcher {

    /**
     * Java虚拟机的可用的处理器数量
     */
    private final int CORE_SIZE = Runtime.getRuntime().availableProcessors();

    private ExecutorService[] executors = new ExecutorService[CORE_SIZE];

    /**
     * 启动时，根据Java虚拟机可用的处理器数量创建可用的线程池
     */
    @PostConstruct
    public void init() {
        for (int i = 0; i < executors.length; i++) {
            executors[i] = Executors.newSingleThreadExecutor(new NamedThreadFactory("message-dispatcher"));
        }
    }

    /**
     * 把任务添加到线程池中
     *
     * @param task 任务
     */
    public void addMessageTask(DispatchTask task) {
        // 根据分发id求模映射 ===> 可考虑使用hash计算Index
        int index = task.getDispatchKey() % executors.length;
        executors[index].submit(task);
    }

}
