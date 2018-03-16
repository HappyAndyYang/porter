/**
 * All rights Reserved, Designed By Suixingpay.
 *
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2018年02月02日 15:14
 * @Copyright ©2018 Suixingpay. All rights reserved.
 * 注意：本内容仅限于随行付支付有限公司内部传阅，禁止外泄以及用于其他的商业用途。
 */

package com.suixingpay.datas.common.client.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.instance.core.CanalInstance;
import com.alibaba.otter.canal.instance.core.CanalInstanceGenerator;
import com.alibaba.otter.canal.instance.manager.CanalInstanceWithManager;
import com.alibaba.otter.canal.instance.manager.model.Canal;
import com.alibaba.otter.canal.instance.manager.model.CanalParameter;
import com.alibaba.otter.canal.instance.manager.model.CanalStatus;
import com.alibaba.otter.canal.protocol.ClientIdentity;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.server.embedded.CanalServerWithEmbedded;
import com.suixingpay.datas.common.client.AbstractClient;
import com.suixingpay.datas.common.client.ConsumeClient;
import com.suixingpay.datas.common.config.source.CanalConfig;
import com.suixingpay.datas.common.exception.ClientConnectionException;
import com.suixingpay.datas.common.exception.ClientException;
import com.suixingpay.datas.common.exception.TaskStopTriggerException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author: zhangkewei[zhang_kw@suixingpay.com]
 * @date: 2018年02月02日 15:14
 * @version: V1.0
 * @review: zhangkewei[zhang_kw@suixingpay.com]/2018年02月02日 15:14
 */
public class CanalClient extends AbstractClient<CanalConfig> implements ConsumeClient {
    private int perPullSize;
    private final CanalServerWithEmbedded canalServer;
    private ClientIdentity clientId;
    private CountDownLatch canFetch = new CountDownLatch(1);

    public CanalClient(CanalConfig config) {
        super(config);
        canalServer = new CanalServerWithEmbedded();
    }

    @Override
    protected void doStart() throws TaskStopTriggerException {
        CanalConfig config = getConfig();
        perPullSize = config.getOncePollSize();
        clientId = new ClientIdentity(config.getDestination(), Short.valueOf("2017"), config.getFilter());
    }

    @Override
    protected void doShutdown() {
        if (null != canalServer) {
            canalServer.stop(getConfig().getDestination());
            canalServer.stop();
        }
        canFetch = new CountDownLatch(1);
    }

    @Override
    public void initializePosition(String taskId, String swimlaneId, String position) throws TaskStopTriggerException {
        /**
         * 这里的批次提交和后续的处理批次不一致，会导致这里的一个批次被分配到不同的数据库事务执行。
         * 为了保证数据一致性，需要将zookeeper记录的消费同步点回滚后重新执行
         */
        canalServer.setCanalInstanceGenerator(new CanalInstanceGenerator() {
            @Override
            @SneakyThrows
            public CanalInstance generate(String destination) {
                Canal canal = new Canal();
                canal.setCanalParameter(new CanalParameter());
                canal.setId(System.nanoTime());
                canal.setStatus(CanalStatus.START);
                canal.getCanalParameter().setSlaveId(System.nanoTime());
                canal.getCanalParameter().setDdlIsolation(false);
                canal.getCanalParameter().setFilterTableError(true);
                canal.getCanalParameter().setMasterAddress(new InetSocketAddress("45.77.95.200", 3306));
                canal.getCanalParameter().setDbPassword("zhuoluo1018");
                canal.getCanalParameter().setDbUsername("root");
                canal.getCanalParameter().setIndexMode(CanalParameter.IndexMode.MEMORY);
                if (!StringUtils.isBlank(position)) {
                    CanalPosition canalPosition = CanalPosition.getPosition(position);
                    canal.getCanalParameter().setMasterLogfileName(canalPosition.logfileName);
                    canal.getCanalParameter().setMasterLogfileOffest(canalPosition.offset);

                }
                return new CanalInstanceWithManager(canal, clientId.getFilter());
            }
        });

        canalServer.start();
        canalServer.start(clientId.getDestination());
        canalServer.subscribe(clientId);// 发起一次订阅
        canFetch.countDown();
    }

    @Override
    public <F, O> List<F> fetch(FetchCallback<F, O> callback) {
        List<F> msgList = new ArrayList<>();
        if (isStarted()) {
            Message msg = null;
            synchronized (canalServer) {
                if (isAutoCommitPosition()) {
                    msg = canalServer.get(clientId, perPullSize, 5L, TimeUnit.SECONDS);
                } else {
                    msg = canalServer.getWithoutAck(clientId, perPullSize, 5L, TimeUnit.SECONDS);
                }
            }
            if (null != msg && msg.getId() != -1 && !msg.getEntries().isEmpty()) {
                try {
                    List<F> f = callback.acceptAll(msg);
                    if (null != f && !f.isEmpty()) {
                        msgList.addAll(f);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return msgList;
    }

    @Override
    public <T> List<T> splitSwimlanes() throws ClientException {
        List<T> clients = new ArrayList<>();
        clients.add((T) this);
        return clients;
    }

    @Override
    public boolean isAutoCommitPosition() {
        return getConfig().isAutoCommit();
    }

    @Override
    public String getSwimlaneId() {
        return getConfig().getDestination();
    }

    @Override
    public  void commitPosition(String position) throws TaskStopTriggerException {
        //如果提交方式为手动提交
        if (!isAutoCommitPosition()) {
            try {
                CanalPosition canalPosition = CanalPosition.getPosition(position);
                synchronized (canalServer) {
                    canalServer.ack(clientId,canalPosition.batchId);
                }
            } catch (Throwable e) {
                throw new TaskStopTriggerException(e);
            }
        }
    }

    @Override
    protected boolean isAlready() {
        try {
            canFetch.await();
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @AllArgsConstructor
    private static class CanalPosition {
        private final long batchId;
        private final long offset;
        private final String logfileName;

        private static CanalPosition getPosition(String position) throws TaskStopTriggerException {
            try {
                JSONObject object = JSONObject.parseObject(position);
                long batchId = object.getLongValue("batchId");
                long offset = object.getLongValue("offset");
                String logfileName = object.getString("logfileName");
                return new CanalPosition(batchId, offset, logfileName);
            } catch (Throwable throwable) {
                throw new TaskStopTriggerException(throwable);
            }
        }
    }
}