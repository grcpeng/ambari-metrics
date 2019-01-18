/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.metrics2.source.JvmMetrics;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.ambari.metrics.core.timeline.HBaseTimelineMetricsService;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.TimelineMetricStore;
import org.apache.ambari.metrics.webapp.AMSWebApp;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;

import static org.apache.hadoop.http.HttpServer2.HTTP_MAX_THREADS_KEY;

/**
 * Metrics collector web server
 */
public class AMSApplicationServer extends CompositeService {

  public static final int SHUTDOWN_HOOK_PRIORITY = 30;
  private static final Log LOG = LogFactory.getLog(AMSApplicationServer.class);

  TimelineMetricStore timelineMetricStore;
  private WebApp webApp;
  private TimelineMetricConfiguration metricConfiguration;

  public AMSApplicationServer() {
    super(AMSApplicationServer.class.getName());
  }

	/**
	 * 读取hbase-site.xml和ams-site.xml文件，创建HBase存储服务
	 * @param conf {@link YarnConfiguration}
	 * @throws Exception
	 */
  @Override
  protected void serviceInit(Configuration conf) throws Exception {
    // 单例模式
    metricConfiguration = TimelineMetricConfiguration.getInstance();
    metricConfiguration.initialize();
    // 返回HBaseTimelineMetricsService对象实例
    timelineMetricStore = createTimelineMetricStore(conf);
    // 将HBaseTimelineMetricsService添加到serviceList中，然后初始化serviceList
    addIfService(timelineMetricStore);
    super.serviceInit(conf);
  }

  @Override
  protected void serviceStart() throws Exception {
  	// 默认的metrics singleton。所有守护进程(如NameNode、DataNode、JobTracker等)都使用这个类。
	  // 在守护进程初始化期间，进程调用init(String)来初始化MetricsSystem。
  	// 便利的方法来初始化metrics system
    DefaultMetricsSystem.initialize("AmbariMetricsSystem");
    // JVM和日志相关指标。主要用于各种服务器，作为它们导出的metrics的一部分。
    JvmMetrics.initSingleton("AmbariMetricsSystem", null);

    startWebApp();
    super.serviceStart();
  }

  @Override
  protected void serviceStop() throws Exception {
    if (webApp != null) {
      webApp.stop();
    }

    DefaultMetricsSystem.shutdown();
    super.serviceStop();
  }
  
  static AMSApplicationServer launchAMSApplicationServer(String[] args) {
  	// 设置当线程由于未捕获的异常而突然终止时调用的默认处理程序，并且没有为该线程定义其他处理程序。
    Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
    // 打印用于启动和关闭的日志消息
    StringUtils.startupShutdownMessage(AMSApplicationServer.class, args, LOG);
    AMSApplicationServer amsApplicationServer = null;
    try {
    	// 用于CompositeService的JVM ShutdownHookManager，在JVM关机的情况下优雅地停止CompositeService。
      amsApplicationServer = new AMSApplicationServer();
      ShutdownHookManager.get().addShutdownHook(
        new CompositeServiceShutdownHook(amsApplicationServer),
        SHUTDOWN_HOOK_PRIORITY);
      YarnConfiguration conf = new YarnConfiguration();
      // 初始化AMS应用服务并启动(使用的是超类AbstractService方法),调用子类的serviceInit和serviceStart方法
      amsApplicationServer.init(conf);
      amsApplicationServer.start();
    } catch (Throwable t) {
      LOG.fatal("Error starting AMSApplicationServer", t);
      ExitUtil.terminate(-1, "Error starting AMSApplicationServer");
    }
    return amsApplicationServer;
  }

  public static void main(String[] args) {
    launchAMSApplicationServer(args);
  }

	/**
	 * 创建HBase存储服务
	 * @param conf
	 * @return
	 */
  protected TimelineMetricStore createTimelineMetricStore(Configuration conf) {
    LOG.info("Creating metrics store.");
    return new HBaseTimelineMetricsService(metricConfiguration);
  }

  protected void startWebApp() {
    String bindAddress = null;
    try {
    	// 从ams-site.xml配置文件读取
      bindAddress = metricConfiguration.getWebappAddress();
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Cannot find bind address");
    }
    LOG.info("Instantiating metrics collector at " + bindAddress);
    try {
    	// 获取ams-site.xml信息
      Configuration conf = metricConfiguration.getMetricsConf();
      // 设置http最大连接数，默认20
      conf.set(HTTP_MAX_THREADS_KEY, String.valueOf(metricConfiguration
        .getTimelineMetricsServiceHandlerThreadCount()));
      // 设置http安全策略，默认HTTP_ONLY
      HttpConfig.Policy policy = HttpConfig.Policy.valueOf(
        conf.get(TimelineMetricConfiguration.TIMELINE_SERVICE_HTTP_POLICY,
          HttpConfig.Policy.HTTP_ONLY.name()));
      webApp =
          WebApps
            .$for("timeline", null, null, "ws")
            .withHttpPolicy(conf, policy)
            .at(bindAddress)
            .start(new AMSWebApp(timelineMetricStore));
    } catch (Exception e) {
      String msg = "AHSWebApp failed to start.";
      LOG.error(msg, e);
      throw new YarnRuntimeException(msg, e);
    }
  }
  
}
