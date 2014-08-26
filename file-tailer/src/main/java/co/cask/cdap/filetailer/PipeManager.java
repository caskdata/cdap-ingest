/*
 * Copyright 2014 Cask, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.filetailer;

import co.cask.cdap.client.StreamClient;
import co.cask.cdap.client.StreamWriter;
import co.cask.cdap.client.exception.NotFoundException;
import co.cask.cdap.filetailer.config.Configuration;
import co.cask.cdap.filetailer.config.ConfigurationLoader;
import co.cask.cdap.filetailer.config.ConfigurationLoaderImpl;
import co.cask.cdap.filetailer.config.FlowConfiguration;
import co.cask.cdap.filetailer.config.exception.ConfigurationLoadingException;
import co.cask.cdap.filetailer.metrics.FileTailerMetricsProcessor;
import co.cask.cdap.filetailer.queue.FileTailerQueue;
import co.cask.cdap.filetailer.sink.FileTailerSink;
import co.cask.cdap.filetailer.sink.SinkStrategy;
import co.cask.cdap.filetailer.state.FileTailerStateProcessor;
import co.cask.cdap.filetailer.state.FileTailerStateProcessorImpl;
import co.cask.cdap.filetailer.tailer.LogTailer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PipeManager  creates and manage pipes
 */

public class PipeManager {
  private final String confPath;
  private List<Pipe> flowfList = new ArrayList<Pipe>();

  public PipeManager(String confPath) {
    this.confPath = confPath;
  }

  /**
   * Pipes setup
   *
   * @throws IOException if can not create client stream
   */

  public void setupPipes() throws IOException {
    try {
      List<FlowConfiguration> flowConfList = getFlowsConfigList();
      for (FlowConfiguration flowConf : flowConfList) {
        FileTailerQueue queue = new FileTailerQueue(flowConf.getQueueSize());
        StreamWriter writer = getStreamWriterForFlow(flowConf);
        FileTailerStateProcessor stateProcessor =
          new FileTailerStateProcessorImpl(flowConf.getDaemonDir(), flowConf.getStateFile());
        FileTailerMetricsProcessor metricsProcessor =
          new FileTailerMetricsProcessor(flowConf.getDaemonDir(), flowConf.getStatisticsFile(),
                                             flowConf.getStatisticsSleepInterval(), flowConf.getFlowName(),
                                             flowConf.getSourceConfiguration().getFileName());
        flowfList.add(new Pipe(new LogTailer(flowConf, queue, stateProcessor, metricsProcessor),
                               new FileTailerSink(queue, writer, SinkStrategy.LOADBALANCE,
                                                  stateProcessor, metricsProcessor,
                                                  flowConf.getSinkConfiguration().getPackSize()),
                               metricsProcessor));
      }
    } catch (ConfigurationLoadingException e) {
      throw new ConfigurationLoadingException("Error during loading configuration from file: "
                                                + confPath + e.getMessage());
    }
  }

  /**
   * Get pipes configuration
   * @return List of the  Pipes configuration read from configuration file
   * @throws ConfigurationLoadingException if can not create client stream
   */
  private List<FlowConfiguration> getFlowsConfigList() throws ConfigurationLoadingException {
    ConfigurationLoader loader = new ConfigurationLoaderImpl();
    Configuration configuration = loader.load(confPath);
    return configuration.getFlowsConfiguration();
  }

  /**
   * create StreamWriter for pipe
   * @return  streamWriter
   * @throws IOException streamWriter creation failed
   */
  private StreamWriter getStreamWriterForFlow(FlowConfiguration flowConf) throws IOException {
    StreamClient client = flowConf.getSinkConfiguration().getStreamClient();
    String streamName = flowConf.getSinkConfiguration().getStreamName();
    try {
      client.create(streamName);
      StreamWriter writer = client.createWriter(streamName);
      return writer;
    } catch (IOException e) {
      throw new IOException("Can not create/get client stream by name:" + streamName + ": " + e.getMessage());
    } catch (NotFoundException e) {
      throw new IOException("Can not create/get client stream by name:" + streamName + ": " + e.getMessage());
    }
  }
  /**
   * Start all pipes
   */
  public void startPipes() {
    for (Pipe pipe : flowfList) {
      pipe.start();
    }
  }
  /**
   * Start all pipes
   */
  public void stopPipes() {
    for (Pipe pipe : flowfList) {
      pipe.stop();
    }
  }
}
