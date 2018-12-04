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
package org.apache.ambari.metrics.core.loadsimulator.net;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * Implements MetricsSender and provides a way of pushing metrics to application metrics history service using REST
 * endpoint.
 * 实现MetricsSender，并提供一种使用REST端点将metrics推到application metrics history的方法。
 */
public class RestMetricsSender implements MetricsSender {
  private final static Logger LOG = LoggerFactory.getLogger(RestMetricsSender.class);

  private final static String COLLECTOR_URL = "http://%s/ws/v1/timeline/metrics";
  private final String collectorServiceAddress;

  /**
   * Creates unconnected RestMetricsSender with endpoint configured as
   * http://${metricsHost}:6188/ws/v1/timeline/metrics,
   * where ${metricsHost} is specified by metricHost param.
   * 创建未连接的RestMetricsSender，端点配置为http://${metricsHost}:6188/ws/v1/timeline/metrics，
   * 其中${metricsHost}由metricHost参数指定。
   * @param metricsHost the hostname that will be used to access application metrics history service.
   */
  public RestMetricsSender(String metricsHost) {
    collectorServiceAddress = String.format(COLLECTOR_URL, metricsHost);
  }

  /**
   * Push metrics to the REST endpoint. Connection is always open and closed on every call.
   * 将指标推送到REST端点。该函数总是在每次调用时打开和关闭。
   * @param payload the payload with metrics to be sent to metrics service
   * @return response message either acknowledgement or error, empty on exception
   */
  @Override
  public String pushMetrics(String payload) {
    String responseString = "";
    UrlService svc = null;
    // 统计连接到发送耗费时长
    Stopwatch timer = Stopwatch.createStarted();

    try {
      LOG.info("server: {}", collectorServiceAddress);

      // 连接到http://collectorhost/ws/v1/timeline/metrics
      svc = getConnectedUrlService();
      responseString = svc.send(payload);

      timer.stop();
      LOG.info("http response time: " + timer.elapsed(TimeUnit.MILLISECONDS)
        + " ms");

      if (responseString.length() > 0) {
        LOG.debug("POST response from server: " + responseString);
      }
    } catch (IOException e) {
      LOG.error("", e);
    } finally {
      if (svc != null) {
        svc.disconnect();
      }
    }

    return responseString;
  }

  /**
   * Relaxed to protected for testing.
   */
  protected UrlService getConnectedUrlService() throws IOException {
    return UrlService.newConnection(collectorServiceAddress);
  }

}
